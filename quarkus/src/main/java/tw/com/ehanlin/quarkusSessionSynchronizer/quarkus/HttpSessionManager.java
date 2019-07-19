package tw.com.ehanlin.quarkusSessionSynchronizer.quarkus;

import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.core.SessionListenerBridge;
import io.undertow.servlet.spec.ServletContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class HttpSessionManager implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(HttpSessionManager.class);

    private Synchronizer _synchronizer;

    public void setSynchronizer(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }
    public Synchronizer getSynchronizer(){
        return this._synchronizer;
    }

    public HttpSessionManager(Synchronizer synchronizer){
        this._synchronizer = synchronizer;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext cotext  = sce.getServletContext();
        if(cotext instanceof ServletContextImpl){
            ServletContextImpl ctx = ((ServletContextImpl) cotext);
            DeploymentInfo di = ctx.getDeployment().getDeploymentInfo();
            if(!(di.getSessionManagerFactory() instanceof  QuarkusInMemorySessionSynchronizerManagerFactory)){
                di.setSessionIdGenerator(new SynchronizableSessionIdGenerator(_synchronizer));
                di.setSessionManagerFactory(new QuarkusInMemorySessionSynchronizerManagerFactory(_synchronizer));
                if(ctx.getDeployment() instanceof DeploymentImpl){
                    DeploymentImpl dep = (DeploymentImpl) ctx.getDeployment();
                    Method method = null;
                    try {
                        method = DeploymentImpl.class.getDeclaredMethod("setSessionManager", SessionManager.class);
                        method.setAccessible(true);
                        method.invoke(dep, di.getSessionManagerFactory().createSessionManager(dep));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    dep.getSessionManager().setDefaultSessionTimeout(di.getDefaultSessionTimeout());
                    dep.getSessionManager().registerSessionListener(new SessionListenerBridge(dep, dep.getApplicationListeners(), ctx));
                    for(SessionListener listener : di.getSessionListeners()) {
                        dep.getSessionManager().registerSessionListener(listener);
                    }
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}


