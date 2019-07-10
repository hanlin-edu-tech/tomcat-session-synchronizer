package tw.com.ehanlin.micronautSessionSynchronizer.micronaut;

import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.session.InMemorySession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.time.Duration;
import java.time.Instant;


public class SynchronizableInMemorySession extends InMemorySession {

    private Synchronizer _synchronizer;
    private SynchronizableSession _synchronizableSession = new SynchronizableSession();

    protected SynchronizableInMemorySession(String id, Duration maxInactiveInterval, Synchronizer synchronizer) {
        super(id, maxInactiveInterval);
        for(CharSequence attribute : this.attributeMap.keySet()){
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
        this._synchronizer = synchronizer;
    }


    protected SynchronizableInMemorySession(String id, Instant creationTime, Duration maxInactiveInterval, Synchronizer synchronizer) {
        super(id, creationTime, maxInactiveInterval);
        this._synchronizer = synchronizer;
    }

    @Override
    public MutableConvertibleValues<Object> put(CharSequence name, Object value) {

        if(value != null){
            this._synchronizableSession.setAttribute(name.toString(), value);
        }else{
            this._synchronizableSession.unsetAttribute(name.toString());
        }
        this._synchronizer.saveOneAttribute(this._synchronizableSession, name.toString());
        return  super.put(name, value);
    }

    protected MutableConvertibleValues<Object> removeAttributeInternal(CharSequence key, boolean sync) {
        this._synchronizableSession.unsetAttribute(key.toString());
        if(sync){
            this._synchronizer.saveOneAttribute(this._synchronizableSession, key.toString());
        }
        return super.remove(key);
    }

    @Override
    public MutableConvertibleValues<Object> remove(CharSequence key) {
        return  this.removeAttributeInternal(key, true);
    }

}
