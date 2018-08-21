package tw.com.ehanlin.tomcatSessionSynchronizer.tomcat8;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Manager;
import org.apache.catalina.TomcatPrincipal;
import org.apache.catalina.session.StandardSession;
import org.apache.tomcat.util.ExceptionUtils;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SynchronizableStandardSession extends StandardSession {

    private Synchronizer _synchronizer;
    private SynchronizableSession _synchronizableSession = new SynchronizableSession();

    public SynchronizableStandardSession(Manager manager) {
        super(manager);
        this._synchronizer = ((Tomcat8SessionSynchronizerManager) manager).getSynchronizer();
    }

    public SynchronizableStandardSession(Manager manager, Synchronizer synchronizer) {
        super(manager);
        this._synchronizer = synchronizer;
    }

    @Override
    public void setId(String id, boolean notify) {
        super.setId(id, notify);
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

    @Override
    public void setAttribute(String name, Object value, boolean notify) {
        super.setAttribute(name, value, notify);
        if(value != null){
            this._synchronizableSession.setAttribute(name, value);
        }else{
            this._synchronizableSession.unsetAttribute(name);
        }
        this._synchronizer.saveOneAttribute(this._synchronizableSession, name);
    }

    protected void removeAttributeInternal(String name, boolean notify, boolean sync) {
        super.removeAttributeInternal(name, notify);
        this._synchronizableSession.unsetAttribute(name);
        if(sync){
            this._synchronizer.saveOneAttribute(this._synchronizableSession, name);
        }
    }

    @Override
    protected void removeAttributeInternal(String name, boolean notify) {
        this.removeAttributeInternal(name, notify, true);
    }


    @Override
    public void expire(boolean notify) {
        if (this.isValid) {
            synchronized(this) {
                if (!this.expiring && this.isValid) {
                    if (this.manager != null) {
                        this.expiring = true;
                        Context context = this.manager.getContext();
                        if (notify) {
                            ClassLoader oldContextClassLoader = null;

                            try {
                                oldContextClassLoader = context.bind(Globals.IS_SECURITY_ENABLED, (ClassLoader)null);
                                Object[] listeners = context.getApplicationLifecycleListeners();
                                if (listeners != null && listeners.length > 0) {
                                    HttpSessionEvent event = new HttpSessionEvent(this.getSession());

                                    for(int i = 0; i < listeners.length; ++i) {
                                        int j = listeners.length - 1 - i;
                                        if (listeners[j] instanceof HttpSessionListener) {
                                            HttpSessionListener listener = (HttpSessionListener)listeners[j];

                                            try {
                                                context.fireContainerEvent("beforeSessionDestroyed", listener);
                                                listener.sessionDestroyed(event);
                                                context.fireContainerEvent("afterSessionDestroyed", listener);
                                            } catch (Throwable var29) {
                                                ExceptionUtils.handleThrowable(var29);

                                                try {
                                                    context.fireContainerEvent("afterSessionDestroyed", listener);
                                                } catch (Exception var28) {
                                                    ;
                                                }

                                                this.manager.getContext().getLogger().error(sm.getString("standardSession.sessionEvent"), var29);
                                            }
                                        }
                                    }
                                }
                            } finally {
                                context.unbind(Globals.IS_SECURITY_ENABLED, oldContextClassLoader);
                            }
                        }

                        if (ACTIVITY_CHECK) {
                            this.accessCount.set(0);
                        }

                        this.manager.remove(this, true);
                        if (notify) {
                            this.fireSessionEvent("destroySession", (Object)null);
                        }

                        if (this.principal instanceof TomcatPrincipal) {
                            TomcatPrincipal gp = (TomcatPrincipal)this.principal;

                            try {
                                gp.logout();
                            } catch (Exception var27) {
                                this.manager.getContext().getLogger().error(sm.getString("standardSession.logoutfail"), var27);
                            }
                        }

                        this.setValid(false);
                        this.expiring = false;
                        String[] keys = this.keys();
                        ClassLoader oldContextClassLoader = null;

                        try {
                            oldContextClassLoader = context.bind(Globals.IS_SECURITY_ENABLED, (ClassLoader)null);

                            for(int i = 0; i < keys.length; ++i) {
                                this.removeAttributeInternal(keys[i], notify, false);
                            }
                        } finally {
                            context.unbind(Globals.IS_SECURITY_ENABLED, oldContextClassLoader);
                        }

                    }
                }
            }
        }
    }
}
