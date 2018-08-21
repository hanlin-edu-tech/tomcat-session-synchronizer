package tw.com.ehanlin.tomcatSessionSynchronizer.core;

import org.junit.Test;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SynchronizableSessionTest {

    @Test
    public void setId() {
        SynchronizableSession session = new SynchronizableSession();
        String id = UUID.randomUUID().toString();
        session.setId(id);
        assertEquals(session.getId(), id);
    }

    @Test
    public void getId() {
        String id = UUID.randomUUID().toString();
        SynchronizableSession session = new SynchronizableSession(id);
        assertEquals(session.getId(), id);
    }

    @Test
    public void setAttribute() {
        SynchronizableSession session = new SynchronizableSession();
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

        assertEquals(session.getAttribute("str"), str);
        assertEquals(session.getAttribute("b"), b);
        assertEquals(session.getAttribute("i"), i);
        assertEquals(session.getAttribute("l"), l);
        assertEquals(session.getAttribute("f"), f);
        assertEquals(session.getAttribute("d"), d);
    }

    @Test
    public void unsetAttribute() {
        SynchronizableSession session = new SynchronizableSession();
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

        assertEquals(session.getAttribute("str"), str);
        assertEquals(session.getAttribute("b"), b);
        assertEquals(session.getAttribute("i"), i);
        assertEquals(session.getAttribute("l"), l);
        assertEquals(session.getAttribute("f"), f);
        assertEquals(session.getAttribute("d"), d);

        session.unsetAttribute("str");
        session.unsetAttribute("b");
        session.unsetAttribute("i");
        session.unsetAttribute("l");
        session.unsetAttribute("f");
        session.unsetAttribute("d");

        assertNull(session.getAttribute("str"));
        assertNull(session.getAttribute("b"));
        assertNull(session.getAttribute("i"));
        assertNull(session.getAttribute("l"));
        assertNull(session.getAttribute("f"));
        assertNull(session.getAttribute("d"));
    }

    @Test
    public void getAttributeNameSet() {
        SynchronizableSession session = new SynchronizableSession();
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

        Set<String> attributeNameSet = session.getAttributeNameSet();
        assertEquals(attributeNameSet.size(), 6);
        assertTrue(attributeNameSet.contains("str"));
        assertTrue(attributeNameSet.contains("b"));
        assertTrue(attributeNameSet.contains("i"));
        assertTrue(attributeNameSet.contains("l"));
        assertTrue(attributeNameSet.contains("f"));
        assertTrue(attributeNameSet.contains("d"));
    }

    @Test
    public void getAttribute() {
        SynchronizableSession session = new SynchronizableSession();
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

        assertEquals(session.getAttribute("str"), str);
        assertEquals(session.getAttribute("b"), b);
        assertEquals(session.getAttribute("i"), i);
        assertEquals(session.getAttribute("l"), l);
        assertEquals(session.getAttribute("f"), f);
        assertEquals(session.getAttribute("d"), d);
    }

    @Test
    public void hasAttribute() {
        SynchronizableSession session = new SynchronizableSession();
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

        assertTrue(session.hasAttribute("str"));
        assertTrue(session.hasAttribute("b"));
        assertTrue(session.hasAttribute("i"));
        assertTrue(session.hasAttribute("l"));
        assertTrue(session.hasAttribute("f"));
        assertTrue(session.hasAttribute("d"));
        assertFalse(session.hasAttribute("abc"));
    }

    @Test
    public void SynchronizableSessionClone() {
        SynchronizableSession session = new SynchronizableSession();
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

        SynchronizableSession sessionClone = session.clone();

        assertTrue(session != sessionClone);
        assertTrue(session.getAttributeNameSet().equals(sessionClone.getAttributeNameSet()));
        assertEquals(session.getAttribute("str"), sessionClone.getAttribute("str"));
        assertEquals(session.getAttribute("b"), sessionClone.getAttribute("b"));
        assertEquals(session.getAttribute("i"), sessionClone.getAttribute("i"));
        assertEquals(session.getAttribute("l"), sessionClone.getAttribute("l"));
        assertEquals(session.getAttribute("f"), sessionClone.getAttribute("f"));
        assertEquals(session.getAttribute("d"), sessionClone.getAttribute("d"));
        assertEquals(session.getId(), sessionClone.getId());
    }
}