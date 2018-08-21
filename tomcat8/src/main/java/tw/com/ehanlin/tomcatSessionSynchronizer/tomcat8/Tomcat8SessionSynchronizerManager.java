package tw.com.ehanlin.tomcatSessionSynchronizer.tomcat8;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.io.IOException;

public class Tomcat8SessionSynchronizerManager extends ManagerBase {

    private Synchronizer _synchronizer;
    public void setSynchronizer(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }
    public Synchronizer getSynchronizer(){
        return this._synchronizer;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        this.setSessionIdGenerator(new SynchronizableSessionIdGenerator(this._synchronizer));
    }

    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();
        this.setState(LifecycleState.STARTING);
    }

    @Override
    public Session findSession(String id) throws IOException {
        return this.createSession(id);
    }

    @Override
    protected StandardSession getNewSession() {
        return new SynchronizableStandardSession(this, this._synchronizer);
    }


    @Override
    public void load() throws ClassNotFoundException, IOException {

    }

    @Override
    public void unload() throws IOException {

    }
}
