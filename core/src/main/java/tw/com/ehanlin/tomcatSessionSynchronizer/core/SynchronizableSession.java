package tw.com.ehanlin.tomcatSessionSynchronizer.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizableSession {

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
    public Object setAttribute(String attribute){
        _attributes.keySet();
        return _attributes.get(attribute);
    }
    public void unsetAttribute(String attribute){
        _attributes.remove(attribute);
    }
    public Set<String> getAttributeNameSet(){
        return _attributes.keySet();
    }
    public boolean hasAttribute(String attribute){
        return _attributes.containsKey(attribute);
    }
}
