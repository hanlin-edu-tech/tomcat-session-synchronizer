package tw.com.ehanlin.tomcatSessionSynchronizer.memory;

import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemorySynchronizer implements Synchronizer {

    private Map<String, SynchronizableSession> _storage = new ConcurrentHashMap<>();

    @Override
    public SynchronizableSession load(String id, boolean createIfNotExist) {
        if(!_storage.containsKey(id) && createIfNotExist){
            _storage.put(id, new SynchronizableSession(id));
        }
        if(_storage.containsKey(id)){
            return _storage.get(id);
        }else{
            return null;
        }
    }

    @Override
    public void save(SynchronizableSession session) {
        _storage.put(session.getId(), session);
    }

    @Override
    public void saveOneAttribute(SynchronizableSession session, String attribute) {
        SynchronizableSession storageSession = load(session.getId(), true);
        if(session.hasAttribute(attribute)){
            storageSession.setAttribute(attribute, session.getAttribute(attribute));
        }else{
            storageSession.unsetAttribute(attribute);
        }
    }
}
