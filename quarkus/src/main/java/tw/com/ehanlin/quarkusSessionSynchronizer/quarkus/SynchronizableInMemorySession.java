package tw.com.ehanlin.quarkusSessionSynchronizer.quarkus;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;
import io.undertow.util.WorkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioExecutor;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class SynchronizableInMemorySession implements Session {

    private Synchronizer _synchronizer;
    private SynchronizableSession _synchronizableSession = new SynchronizableSession();

    private static final Logger log = LoggerFactory.getLogger(QuarkusInMemorySessionSynchronizerManagerFactory.class);

    public SynchronizableInMemorySession(final QuarkusInMemorySessionSynchronizerManager sessionManager, final String sessionId,
                                            final SessionConfig sessionCookieConfig, final XnioIoThread executor,
                                            final XnioWorker worker, final Object evictionToken, final int maxInactiveInterval,
                                            final Synchronizer synchronizer) {
        this.sessionManager = sessionManager;
        this.sessionId = sessionId;
        this.sessionCookieConfig = sessionCookieConfig;
        this.executor = executor;
        this.worker = worker;
        this.evictionToken = evictionToken;
        creationTime = lastAccessed = System.currentTimeMillis();
        this.maxInactiveInterval = maxInactiveInterval;
        this._synchronizer = sessionManager.getSynchronizer();
        syncSessionId(sessionId);
    }

    final AttachmentKey<Long> FIRST_REQUEST_ACCESS = AttachmentKey.create(Long.class);
    final QuarkusInMemorySessionSynchronizerManager sessionManager;
    final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
    volatile long lastAccessed;
    final long creationTime;
    volatile int maxInactiveInterval;

    static volatile AtomicReferenceFieldUpdater<SynchronizableInMemorySession, Object> evictionTokenUpdater;
    static {
        //this is needed in case there is unprivileged code on the stack
        //it needs to delegate to the createTokenUpdater() method otherwise the creation will fail
        //as the inner class cannot access the member
        evictionTokenUpdater = AccessController.doPrivileged(new PrivilegedAction<AtomicReferenceFieldUpdater<SynchronizableInMemorySession, Object>>() {
            @Override
            public AtomicReferenceFieldUpdater<SynchronizableInMemorySession, Object> run() {
                return createTokenUpdater();
            }
        });
    }

    private static AtomicReferenceFieldUpdater<SynchronizableInMemorySession, Object> createTokenUpdater() {
        return AtomicReferenceFieldUpdater.newUpdater(SynchronizableInMemorySession.class, Object.class, "evictionToken");
    }


    private String sessionId;
    private volatile Object evictionToken;
    private final SessionConfig sessionCookieConfig;
    private volatile long expireTime = -1;
    private volatile boolean invalid = false;
    private volatile boolean invalidationStarted = false;

    final XnioIoThread executor;
    final XnioWorker worker;

    XnioExecutor.Key timerCancelKey;

    Runnable cancelTask = new Runnable() {
        @Override
        public void run() {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis();
                    if(currentTime >= expireTime) {
                        invalidate(null, SessionListener.SessionDestroyedReason.TIMEOUT);
                    } else {
                        timerCancelKey = WorkerUtils.executeAfter(executor, cancelTask, expireTime - currentTime, TimeUnit.MILLISECONDS);
                    }
                }
            });
        }
    };


    synchronized void bumpTimeout() {
        if(invalidationStarted) {
            return;
        }

        final int maxInactiveInterval = getMaxInactiveInterval();
        if (maxInactiveInterval > 0) {
            long newExpireTime = System.currentTimeMillis() + (maxInactiveInterval * 1000L);
            if(timerCancelKey != null && (newExpireTime < expireTime)) {
                // We have to re-schedule as the new maxInactiveInterval is lower than the old one
                if (!timerCancelKey.remove()) {
                    return;
                }
                timerCancelKey = null;
            }
            expireTime = newExpireTime;
            UndertowLogger.SESSION_LOGGER.tracef("Bumping timeout for session %s to %s", sessionId, expireTime);
            if(timerCancelKey == null) {
                //+1, to make sure that the time has actually expired
                //we don't re-schedule every time, as it is expensive
                //instead when it expires we check if the timeout has been bumped, and if so we re-schedule
                timerCancelKey = executor.executeAfter(cancelTask, (maxInactiveInterval * 1000L) + 1L, TimeUnit.MILLISECONDS);
            }
        } else {
            expireTime = -1;
            if(timerCancelKey != null) {
                timerCancelKey.remove();
                timerCancelKey = null;
            }
        }
        if (evictionToken != null) {
            Object token = evictionToken;
            if (evictionTokenUpdater.compareAndSet(this, token, null)) {
                sessionManager.evictionQueue.removeToken(token);
                this.evictionToken = sessionManager.evictionQueue.offerLastAndReturnToken(sessionId);
            }
        }
    }


    @Override
    public String getId() {
        return sessionId;
    }

    void requestStarted(HttpServerExchange serverExchange) {
        Long existing = serverExchange.getAttachment(FIRST_REQUEST_ACCESS);
        if(existing == null) {
            if (!invalid) {
                serverExchange.putAttachment(FIRST_REQUEST_ACCESS, System.currentTimeMillis());
            }
        }
    }

    @Override
    public void requestDone(final HttpServerExchange serverExchange) {
        Long existing = serverExchange.getAttachment(FIRST_REQUEST_ACCESS);
        if(existing != null) {
            lastAccessed = existing;
        }
    }

    @Override
    public long getCreationTime() {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return lastAccessed;
    }

    @Override
    public void setMaxInactiveInterval(final int interval) {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        UndertowLogger.SESSION_LOGGER.debugf("Setting max inactive interval for %s to %s", sessionId, interval);
        maxInactiveInterval = interval;
        bumpTimeout();
    }

    @Override
    public int getMaxInactiveInterval() {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(final String name) {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        bumpTimeout();
        return attributes.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        bumpTimeout();
        return attributes.keySet();
    }

    @Override
    public Object setAttribute(final String name, final Object value) {
        if (value == null) {
            return removeAttribute(name);
        }
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        final Object existing = attributes.put(name, value);
        this._synchronizableSession.setAttribute(name, value);
        this._synchronizer.saveOneAttribute(this._synchronizableSession, name);
        if (existing == null) {
            sessionManager.sessionListeners.attributeAdded(this, name, value);
        } else {
            sessionManager.sessionListeners.attributeUpdated(this, name, value, existing);
        }
        bumpTimeout();
        UndertowLogger.SESSION_LOGGER.tracef("Setting session attribute %s to %s for session %s", name, value, sessionId);
        return existing;
    }

    @Override
    public Object removeAttribute(final String name) {
        if (invalid) {
            throw UndertowMessages.MESSAGES.sessionIsInvalid(sessionId);
        }
        final Object existing = attributes.remove(name);
        this._synchronizableSession.unsetAttribute(name);
        this._synchronizer.saveOneAttribute(this._synchronizableSession, name);
        sessionManager.sessionListeners.attributeRemoved(this, name, existing);
        bumpTimeout();
        UndertowLogger.SESSION_LOGGER.tracef("Removing session attribute %s for session %s", name, sessionId);
        return existing;
    }

    @Override
    public void invalidate(final HttpServerExchange exchange) {
        invalidate(exchange, SessionListener.SessionDestroyedReason.INVALIDATED);
        if(exchange != null) {
            exchange.removeAttachment(sessionManager.NEW_SESSION);
        }
        Object evictionToken = this.evictionToken;
        if(evictionToken != null) {
            sessionManager.evictionQueue.removeToken(evictionToken);
        }
    }

    void invalidate(final HttpServerExchange exchange, SessionListener.SessionDestroyedReason reason) {
        synchronized(SynchronizableInMemorySession.this) {
            if (timerCancelKey != null) {
                timerCancelKey.remove();
            }
            SynchronizableInMemorySession sess = sessionManager.sessions.remove(sessionId);
            if (sess == null) {
                if (reason == SessionListener.SessionDestroyedReason.INVALIDATED) {
                    throw UndertowMessages.MESSAGES.sessionAlreadyInvalidated();
                }
                return;
            }
            invalidationStarted = true;
        }
        UndertowLogger.SESSION_LOGGER.debugf("Invalidating session %s for exchange %s", sessionId, exchange);

        sessionManager.sessionListeners.sessionDestroyed(this, exchange, reason);
        invalid = true;

        if(sessionManager.statisticsEnabled) {
            long life = System.currentTimeMillis() - creationTime;
            synchronized (sessionManager) {
                sessionManager.expiredSessionCount++;
                sessionManager.totalSessionLifetime = sessionManager.totalSessionLifetime.add(BigInteger.valueOf(life));
                if(sessionManager.longestSessionLifetime < life) {
                    sessionManager.longestSessionLifetime = life;
                }
            }
        }
        if (exchange != null) {
            sessionCookieConfig.clearSession(exchange, this.getId());
        }
    }

    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public String changeSessionId(final HttpServerExchange exchange, final SessionConfig config) {
        final String oldId = sessionId;
        String newId = sessionManager.sessionIdGenerator.createSessionId();
        this.sessionId = newId;
        syncSessionId(newId);
        if(!invalid) {
            sessionManager.sessions.put(newId, this);
            config.setSessionId(exchange, this.getId());
        }
        sessionManager.sessions.remove(oldId);
        sessionManager.sessionListeners.sessionIdChanged(this, oldId);
        UndertowLogger.SESSION_LOGGER.debugf("Changing session id %s to %s", oldId, newId);

        return newId;
    }

    private void syncSessionId(String id) {
        for(String attribute : this.attributes.keySet()){
            this.attributes.remove(attribute);
        }
        this._synchronizableSession = _synchronizer.load(id, true);
        if(this._synchronizableSession != null){
            for(String attribute : this._synchronizableSession.getAttributeNameSet()){
                if(attribute != null && this._synchronizableSession.getAttribute(attribute) != null){
                    this.attributes.put(attribute, this._synchronizableSession.getAttribute(attribute));
                }
            }
        }
    }

    synchronized void destroy() {
        if (timerCancelKey != null) {
            timerCancelKey.remove();
        }
        cancelTask = null;
    }

}