package tw.com.ehanlin.tomcatSessionSynchronizer.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class MongodbSynchronizer implements Synchronizer {

    private static final String SESSION_ID = "_id";
    private static final String CREATE_DATE = "_createDate";
    private static final String LAST_UPDATE_DATE = "_lastUpdateDate";

    private static final FindOneAndUpdateOptions findUpsertOptions = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);
    private static final UpdateOptions upsertOptions = new UpdateOptions().upsert(true);

    private static final Pattern systemKeyPattern = Pattern.compile("^_.*");
    private static boolean isSystemKey(String key) {
        return systemKeyPattern.matcher(key).matches();
    }

    private MongoCollection<Document> coll;

    public MongodbSynchronizer(String uri, String db, String collection) {
        this.coll = new MongoClient(new MongoClientURI(uri))
            .getDatabase(db)
            .getCollection(collection);
    }

    @Override
    public SynchronizableSession load(String id, boolean createIfNotExist) {
        if(createIfNotExist){
            long now = System.currentTimeMillis();
            Document sessionData = coll.findOneAndUpdate(
                eq(SESSION_ID, id),
                new Document("$setOnInsert", new Document(CREATE_DATE, now).append(LAST_UPDATE_DATE, now)),
                findUpsertOptions);
            SynchronizableSession session = new SynchronizableSession(id);
            sessionData.forEach((k, v) -> { if(k != null && v != null && !isSystemKey(k)) session.setAttribute(k, v); });
            return session;
        }else{
            Document sessionData = coll.find(eq(SESSION_ID, id)).first();
            if(sessionData != null){
                SynchronizableSession session = new SynchronizableSession(id);
                sessionData.forEach((k, v) -> { if(k != null && v != null && !isSystemKey(k)) session.setAttribute(k, v); });
                return session;
            }else{
                return null;
            }
        }
    }

    @Override
    public void save(SynchronizableSession session) {
        Document setDoc = new Document();
        for(String attribute : session.getAttributeNameSet()){
            if(!isSystemKey(attribute)) {
                setDoc.append(attribute, session.getAttribute(attribute));
            }
        }
        long now = System.currentTimeMillis();
        setDoc.append(LAST_UPDATE_DATE, now);
        coll.updateOne(
            eq(SESSION_ID, session.getId()),
            new Document("$set", setDoc).append("$setOnInsert", new Document(CREATE_DATE, now)),
            upsertOptions);
    }

    @Override
    public void saveOneAttribute(SynchronizableSession session, String attribute) {
        if(!isSystemKey(attribute)) {
            if (session.hasAttribute(attribute)) {
                coll.updateOne(
                    eq(SESSION_ID, session.getId()),
                    new Document("$set", new Document(attribute, session.getAttribute(attribute)).append(LAST_UPDATE_DATE, System.currentTimeMillis())),
                    upsertOptions);
            } else {
                coll.updateOne(
                    eq(SESSION_ID, session.getId()),
                    new Document("$set", new Document(LAST_UPDATE_DATE, System.currentTimeMillis()))
                        .append("$unset", new Document(attribute, "")));
            }
        }
    }
}
