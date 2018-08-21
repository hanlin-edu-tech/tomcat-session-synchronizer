package tw.com.ehanlin.tomcatSessionSynchronizer.memory;

import org.junit.Test;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.SynchronizableSession;
import tw.com.ehanlin.tomcatSessionSynchronizer.core.Synchronizer;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MemorySynchronizerTest {

    public SynchronizableSession createSession() {
        SynchronizableSession session = new SynchronizableSession(UUID.randomUUID().toString());
        String str = UUID.randomUUID().toString();
        Random random = new Random();
        Boolean b = random.nextBoolean();
        Integer i = random.nextInt();
        Long l = random.nextLong();
        Float f = random.nextFloat();
        Double d = random.nextDouble();

        session.setAttribute("str", str);
        session.setAttribute("b", b);
        session.setAttribute("i", i);
        session.setAttribute("l", l);
        session.setAttribute("f", f);
        session.setAttribute("d", d);

        return session;
    }

    @Test
    public void saveAndLoad() {
        Synchronizer synchronizer = new MemorySynchronizer();
        SynchronizableSession session1 = createSession();
        synchronizer.save(session1);

        SynchronizableSession session1Load = synchronizer.load(session1.getId(), false);
        assertTrue(session1Load != session1);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttributeNameSet(), session1.getAttributeNameSet());
        assertEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));
        assertEquals(session1Load.getAttribute("i"), session1.getAttribute("i"));
        assertEquals(session1Load.getAttribute("l"), session1.getAttribute("l"));
        assertEquals(session1Load.getAttribute("f"), session1.getAttribute("f"));
        assertEquals(session1Load.getAttribute("d"), session1.getAttribute("d"));

        session1Load = synchronizer.load(session1.getId(), true);
        assertTrue(session1Load != session1);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttributeNameSet(), session1.getAttributeNameSet());
        assertEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));
        assertEquals(session1Load.getAttribute("i"), session1.getAttribute("i"));
        assertEquals(session1Load.getAttribute("l"), session1.getAttribute("l"));
        assertEquals(session1Load.getAttribute("f"), session1.getAttribute("f"));
        assertEquals(session1Load.getAttribute("d"), session1.getAttribute("d"));

        session1.setAttribute("str", "testStr");
        session1.setAttribute("i", 929);
        session1.setAttribute("testKey", "testValue");
        synchronizer.save(session1);

        session1Load = synchronizer.load(session1.getId(), false);
        assertTrue(session1Load != session1);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttributeNameSet(), session1.getAttributeNameSet());
        assertEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));
        assertEquals(session1Load.getAttribute("i"), session1.getAttribute("i"));
        assertEquals(session1Load.getAttribute("l"), session1.getAttribute("l"));
        assertEquals(session1Load.getAttribute("f"), session1.getAttribute("f"));
        assertEquals(session1Load.getAttribute("d"), session1.getAttribute("d"));
        assertEquals(session1Load.getAttribute("testKey"), session1.getAttribute("testKey"));

        session1Load = synchronizer.load(session1.getId(), true);
        assertTrue(session1Load != session1);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttributeNameSet(), session1.getAttributeNameSet());
        assertEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));
        assertEquals(session1Load.getAttribute("i"), session1.getAttribute("i"));
        assertEquals(session1Load.getAttribute("l"), session1.getAttribute("l"));
        assertEquals(session1Load.getAttribute("f"), session1.getAttribute("f"));
        assertEquals(session1Load.getAttribute("d"), session1.getAttribute("d"));
        assertEquals(session1Load.getAttribute("testKey"), session1.getAttribute("testKey"));
    }

    @Test
    public void loadEmpty() {
        Synchronizer synchronizer = new MemorySynchronizer();
        String id = UUID.randomUUID().toString();
        SynchronizableSession session1Load = synchronizer.load(id, false);
        assertNull(session1Load);

        session1Load = synchronizer.load(id, true);
        assertNotNull(session1Load);
        assertEquals(session1Load.getId(), id);
        assertEquals(session1Load.getAttributeNameSet().size(), 0);
    }

    @Test
    public void saveOneAttribute() {
        Synchronizer synchronizer = new MemorySynchronizer();
        SynchronizableSession session1 = createSession();
        synchronizer.save(session1);

        session1.setAttribute("str", "testSTR");
        synchronizer.saveOneAttribute(session1, "str");

        SynchronizableSession session1Load = synchronizer.load(session1.getId(), false);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));

        session1.setAttribute("str", "testSTR2");
        session1.setAttribute("testKey", "testKEY");
        synchronizer.saveOneAttribute(session1, "testKey");
        session1Load = synchronizer.load(session1.getId(), false);
        assertEquals(session1Load.getId(), session1.getId());
        assertEquals(session1Load.getAttribute("testKey"), session1.getAttribute("testKey"));
        assertEquals(session1Load.getAttribute("b"), session1.getAttribute("b"));
        assertNotEquals(session1Load.getAttribute("str"), session1.getAttribute("str"));
    }
}