package tw.com.ehanlin.tomcatSessionSynchronizer.tomcat8;

import org.apache.catalina.util.StandardSessionIdGenerator;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

public class SynchronizableSessionIdGenerator extends StandardSessionIdGenerator {

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
    public String generateSessionId(String route) {
        String id = null;

        do {
            id = super.generateSessionId(route);
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
