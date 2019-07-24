package tw.com.ehanlin.micronautSessionSynchronizer.micronaut;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.session.InMemorySession;
import io.micronaut.session.InMemorySessionStore;
import io.micronaut.session.SessionConfiguration;
import io.micronaut.session.SessionIdGenerator;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@Primary
public class MicronautSessionStoreSynchronizer extends InMemorySessionStore {

    private final SessionConfiguration sessionConfiguration;
    private final SessionIdGenerator sessionIdGenerator;
    private Synchronizer _synchronizer;
    public void setSynchronizer(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }
    public Synchronizer getSynchronizer(){
        return this._synchronizer;
    }


    public MicronautSessionStoreSynchronizer(
            SessionIdGenerator sessionIdGenerator,
            SessionConfiguration sessionConfiguration,
            ApplicationEventPublisher eventPublisher,
            Synchronizer synchronizer) {
        super(sessionIdGenerator ,sessionConfiguration, eventPublisher);
        this._synchronizer = synchronizer;
        this.sessionIdGenerator = sessionIdGenerator;
        this.sessionConfiguration = sessionConfiguration;
    }

    @Override
    public InMemorySession newSession() {
        return new SynchronizableInMemorySession(sessionIdGenerator.generateId(), sessionConfiguration.getMaxInactiveInterval(),
                this._synchronizer);
    }

    @Override
    public CompletableFuture<Optional<InMemorySession>> findSession(String id) {
        InMemorySession session = newSession();
        return CompletableFuture.completedFuture(
                Optional.ofNullable(session != null && !session.isExpired() ? session : null)
        );
    }
}
