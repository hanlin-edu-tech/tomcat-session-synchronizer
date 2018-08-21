package tw.com.ehanlin.tomcatSessionSynchronizer.core;

public interface Synchronizer {

    SynchronizableSession load(String id, boolean createIfNotExist);

    void save(SynchronizableSession session);
    void saveOneAttribute(SynchronizableSession session, String attribute);

}
