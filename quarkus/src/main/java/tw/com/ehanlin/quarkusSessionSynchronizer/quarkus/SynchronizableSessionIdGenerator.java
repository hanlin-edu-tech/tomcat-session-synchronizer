package tw.com.ehanlin.quarkusSessionSynchronizer.quarkus;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;


public class SynchronizableSessionIdGenerator extends SecureRandomSessionIdGenerator {

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
    public String createSessionId() {
        String id = null;
        do {
            id = super.createSessionId();
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
