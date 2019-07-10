package tw.com.ehanlin.micronautSessionSynchronizer.micronaut;

import io.micronaut.context.annotation.Primary;
import io.micronaut.session.DefaultSessionIdGenerator;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import javax.inject.Singleton;

@Singleton
@Primary
public class SynchronizableSessionIdGenerator extends DefaultSessionIdGenerator {

    private Synchronizer _synchronizer;
    public void setSynchronizer(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }
    public Synchronizer getSynchronizer(){
        return this._synchronizer;
    }

    public SynchronizableSessionIdGenerator(Synchronizer synchronizer){
        super();
        this._synchronizer = synchronizer;
    }

    @Override
    public String generateId() {
        String id = null;

        do {
            id = super.generateId();
            if(this._synchronizer != null){
                try{
                    if(this._synchronizer.load(id, false) != null){
                        id = null;
                    }
                }catch(Throwable ex){

                }
            }
        } while (id == null);

        return id;
    }
}
