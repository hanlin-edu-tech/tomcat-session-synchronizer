package tw.com.ehanlin.quarkusSessionSynchronizer.quarkus;

import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

public final class QuarkusInMemorySessionSynchronizerManagerFactory implements SessionManagerFactory {

    private final int maxSessions;
    private final boolean expireOldestUnusedSessionOnMax;
    private final Synchronizer _synchronizer;

    private static final Logger log = LoggerFactory.getLogger(QuarkusInMemorySessionSynchronizerManagerFactory.class);

    public QuarkusInMemorySessionSynchronizerManagerFactory(Synchronizer synchronizer) {
        this(-1, false, synchronizer);
    }

    public QuarkusInMemorySessionSynchronizerManagerFactory(int maxSessions, Synchronizer synchronizer) {
        this(maxSessions, false, synchronizer);
    }

    public QuarkusInMemorySessionSynchronizerManagerFactory(int maxSessions, boolean expireOldestUnusedSessionOnMax,
                                                            Synchronizer synchronizer) {
        this.maxSessions = maxSessions;
        this.expireOldestUnusedSessionOnMax = expireOldestUnusedSessionOnMax;
        this._synchronizer = synchronizer;
    }

    @Override
    public SessionManager createSessionManager(Deployment deployment) {
        return new QuarkusInMemorySessionSynchronizerManager(deployment.getDeploymentInfo().getSessionIdGenerator(),
                deployment.getDeploymentInfo().getDeploymentName(), maxSessions, expireOldestUnusedSessionOnMax,
                deployment.getDeploymentInfo().getMetricsCollector() != null, _synchronizer);
    }

}
