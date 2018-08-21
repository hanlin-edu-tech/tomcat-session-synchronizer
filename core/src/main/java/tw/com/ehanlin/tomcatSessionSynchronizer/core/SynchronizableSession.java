package tw.com.ehanlin.tomcatSessionSynchronizer.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizableSession {

    public SynchronizableSession(){

    }

    public SynchronizableSession(String id){
        this._id = id;
    }

    private SynchronizableSession(SynchronizableSession session){
        this._id = session._id;
        this._attributes.putAll(session._attributes);
    }

    private String _id = "";
    private Map<String, Object> _attributes = new ConcurrentHashMap<>();

    public void setId(String id){
        this._id = id;
    }
    public String getId(){
        return _id;
    }

    public void setAttribute(String attribute, Object value){
        _attributes.put(attribute, value);
    }
    public void unsetAttribute(String attribute){
        _attributes.remove(attribute);
    }
    public Set<String> getAttributeNameSet(){
        return _attributes.keySet();
    }
    public Object getAttribute(String attribute){
        return _attributes.get(attribute);
    }
    public boolean hasAttribute(String attribute){
        return _attributes.containsKey(attribute);
    }

    public SynchronizableSession clone(){
        return new SynchronizableSession(this);
    }
}
