package tw.com.ehanlin.quarkusSessionSynchronizer.quarkus;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.*;
import io.undertow.util.AttachmentKey;
import io.undertow.util.ConcurrentDirectDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class QuarkusInMemorySessionSynchronizerManager implements SessionManager, SessionManagerStatistics {

    final AttachmentKey<SynchronizableInMemorySession> NEW_SESSION = AttachmentKey.create(SynchronizableInMemorySession.class);

    final SessionIdGenerator sessionIdGenerator;

    final ConcurrentMap<String, SynchronizableInMemorySession> sessions;

    final SessionListeners sessionListeners = new SessionListeners();

    private static final Logger log = LoggerFactory.getLogger(QuarkusInMemorySessionSynchronizerManager.class);

    /**
     * 30 minute default
     */
    private volatile int defaultSessionTimeout = 30 * 60;

    private final int maxSize;

    final ConcurrentDirectDeque<String> evictionQueue;

    private final String deploymentName;

    final AtomicLong createdSessionCount = new AtomicLong();
    final AtomicLong rejectedSessionCount = new AtomicLong();
    volatile long longestSessionLifetime = 0;
    volatile long expiredSessionCount = 0;
    volatile BigInteger totalSessionLifetime = BigInteger.ZERO;
    final AtomicInteger highestSessionCount = new AtomicInteger();

    final boolean statisticsEnabled;

    private volatile long startTime;

    private final boolean expireOldestUnusedSessionOnMax;
    private Synchronizer _synchronizer;

    public void setSynchronizer(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }
    public Synchronizer getSynchronizer(){
        return this._synchronizer;
    }

    public QuarkusInMemorySessionSynchronizerManager(String deploymentName, int maxSessions,
                                                     boolean expireOldestUnusedSessionOnMax, Synchronizer synchronizer) {
        this(new SynchronizableSessionIdGenerator(synchronizer), deploymentName, maxSessions, expireOldestUnusedSessionOnMax, synchronizer);
    }

    public QuarkusInMemorySessionSynchronizerManager(SessionIdGenerator sessionIdGenerator, String deploymentName,
                                                     int maxSessions, boolean expireOldestUnusedSessionOnMax, Synchronizer synchronizer) {
        this(sessionIdGenerator, deploymentName, maxSessions, expireOldestUnusedSessionOnMax, true, synchronizer);
    }

    public QuarkusInMemorySessionSynchronizerManager(SessionIdGenerator sessionIdGenerator, String deploymentName,
                                                     int maxSessions, boolean expireOldestUnusedSessionOnMax,
                                                     boolean statisticsEnabled, Synchronizer synchronizer) {
        this.sessionIdGenerator = sessionIdGenerator;
        this.deploymentName = deploymentName;
        this.statisticsEnabled = statisticsEnabled;
        this.expireOldestUnusedSessionOnMax = expireOldestUnusedSessionOnMax;
        this.sessions = new ConcurrentHashMap<>();
        this.maxSize = maxSessions;
        ConcurrentDirectDeque<String> evictionQueue = null;
        if (maxSessions > 0 && expireOldestUnusedSessionOnMax) {
            evictionQueue = ConcurrentDirectDeque.newInstance();
        }
        this.evictionQueue = evictionQueue;
        this._synchronizer = synchronizer;
    }

    public QuarkusInMemorySessionSynchronizerManager(String deploymentName, int maxSessions, Synchronizer synchronizer) {
        this(deploymentName, maxSessions, false, synchronizer);
    }

    public QuarkusInMemorySessionSynchronizerManager(String id, Synchronizer synchronizer) {
        this(id, -1, synchronizer);
    }


    @Override
    public String getDeploymentName() {
        return this.deploymentName;
    }

    @Override
    public void start() {
        createdSessionCount.set(0);
        expiredSessionCount = 0;
        rejectedSessionCount.set(0);
        totalSessionLifetime = BigInteger.ZERO;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        for (Map.Entry<String, SynchronizableInMemorySession> session : sessions.entrySet()) {
            session.getValue().destroy();
            sessionListeners.sessionDestroyed(session.getValue(), null, SessionListener.SessionDestroyedReason.UNDEPLOY);
        }
        sessions.clear();
    }

    @Override
    public Session createSession(final HttpServerExchange serverExchange, final SessionConfig config) {
        if (maxSize > 0) {
            if(expireOldestUnusedSessionOnMax) {
                while (sessions.size() >= maxSize && !evictionQueue.isEmpty()) {
                    String key = evictionQueue.poll();
                    UndertowLogger.REQUEST_LOGGER.debugf("Removing session %s as max size has been hit", key);
                    SynchronizableInMemorySession toRemove = sessions.get(key);
                    if (toRemove != null) {
                        toRemove.invalidate(null, SessionListener.SessionDestroyedReason.TIMEOUT); //todo: better reason
                    }
                }
            } else if(sessions.size() >= maxSize) {
                if(statisticsEnabled) {
                    rejectedSessionCount.incrementAndGet();
                }
                throw UndertowMessages.MESSAGES.tooManySessions(maxSize);
            }
        }
        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }
        String sessionID = config.findSessionId(serverExchange);
        int count = 0;
        while (sessionID == null) {
            sessionID = sessionIdGenerator.createSessionId();
            if(sessions.containsKey(sessionID)) {
                sessionID = null;
            }
            if(count++ == 100) {
                //this should never happen
                //but we guard against pathalogical session id generators to prevent an infinite loop
                throw UndertowMessages.MESSAGES.couldNotGenerateUniqueSessionId();
            }
        }
        Object evictionToken;
        if (evictionQueue != null) {
            evictionToken = evictionQueue.offerLastAndReturnToken(sessionID);
        } else {
            evictionToken = null;
        }
        final SynchronizableInMemorySession session = new SynchronizableInMemorySession(this,
                sessionID, config, serverExchange.getIoThread(), serverExchange.getConnection().getWorker(),
                evictionToken, defaultSessionTimeout, _synchronizer);

        UndertowLogger.SESSION_LOGGER.debugf("Created session with id %s for exchange %s", sessionID, serverExchange);
        sessions.put(sessionID, session);
        config.setSessionId(serverExchange, session.getId());
        session.bumpTimeout();
        sessionListeners.sessionCreated(session, serverExchange);
        serverExchange.putAttachment(NEW_SESSION, session);

        if(statisticsEnabled) {
            createdSessionCount.incrementAndGet();
            int highest;
            int sessionSize;
            do {
                highest = highestSessionCount.get();
                sessionSize = sessions.size();
                if(sessionSize <= highest) {
                    break;
                }
            } while (!highestSessionCount.compareAndSet(highest, sessionSize));
        }
        return session;
    }

    @Override
    public Session getSession(final HttpServerExchange serverExchange, final SessionConfig config) {
        if (serverExchange != null) {
            SynchronizableInMemorySession newSession = serverExchange.getAttachment(NEW_SESSION);
            if(newSession != null) {
                return newSession;
            }
        }
        String sessionId = config.findSessionId(serverExchange);
        SynchronizableInMemorySession session = (SynchronizableInMemorySession) getSession(sessionId);
        if(session != null && serverExchange != null) {
            session.requestStarted(serverExchange);
        }
        return session;
    }

    @Override
    public Session getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        final SynchronizableInMemorySession sess = sessions.get(sessionId);
        if (sess == null) {
            return null;
        } else {
            return sess;
        }
    }


    @Override
    public synchronized void registerSessionListener(final SessionListener listener) {
        UndertowLogger.SESSION_LOGGER.debugf("Registered session listener %s", listener);
        sessionListeners.addSessionListener(listener);
    }

    @Override
    public synchronized void removeSessionListener(final SessionListener listener) {
        UndertowLogger.SESSION_LOGGER.debugf("Removed session listener %s", listener);
        sessionListeners.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(final int timeout) {
        UndertowLogger.SESSION_LOGGER.debugf("Setting default session timeout to %s", timeout);
        defaultSessionTimeout = timeout;
    }

    @Override
    public Set<String> getTransientSessions() {
        return getAllSessions();
    }

    @Override
    public Set<String> getActiveSessions() {
        return getAllSessions();
    }

    @Override
    public Set<String> getAllSessions() {
        return new HashSet<>(sessions.keySet());
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SessionManager)) return false;
        SessionManager manager = (SessionManager) object;
        return this.deploymentName.equals(manager.getDeploymentName());
    }

    @Override
    public int hashCode() {
        return this.deploymentName.hashCode();
    }

    @Override
    public String toString() {
        return this.deploymentName;
    }

    @Override
    public SessionManagerStatistics getStatistics() {
        return this;
    }

    public long getCreatedSessionCount() {
        return createdSessionCount.get();
    }

    @Override
    public long getMaxActiveSessions() {
        return maxSize;
    }

    @Override
    public long getHighestSessionCount() {
        return highestSessionCount.get();
    }

    @Override
    public long getActiveSessionCount() {
        return sessions.size();
    }

    @Override
    public long getExpiredSessionCount() {
        return expiredSessionCount;
    }

    @Override
    public long getRejectedSessions() {
        return rejectedSessionCount.get();

    }

    @Override
    public long getMaxSessionAliveTime() {
        return longestSessionLifetime;
    }

    @Override
    public synchronized long getAverageSessionAliveTime() {
        //this method needs to be synchronised to make sure the session count and the total are in sync
        if(expiredSessionCount == 0) {
            return 0;
        }
        return new BigDecimal(totalSessionLifetime).divide(BigDecimal.valueOf(expiredSessionCount), MathContext.DECIMAL128).longValue();
    }

    @Override
    public long getStartTime() {
        return startTime;
    }
}
