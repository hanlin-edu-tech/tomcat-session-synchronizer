package tw.com.ehanlin.tomcatSessionSynchronizer.teams;

import tw.com.ehanlin.tomcatSessionSynchronizer.mongodb.MongodbSynchronizer;
import tw.com.ehanlin.tomcatSessionSynchronizer.tomcat8.Tomcat8SessionSynchronizerManager;

public final class Teams103SessionSynchronizerManager extends Tomcat8SessionSynchronizerManager {

    public Teams103SessionSynchronizerManager() {
        super(new MongodbSynchronizer("mongodb://session-mongo:27017", "teams", "session"));
    }

}
