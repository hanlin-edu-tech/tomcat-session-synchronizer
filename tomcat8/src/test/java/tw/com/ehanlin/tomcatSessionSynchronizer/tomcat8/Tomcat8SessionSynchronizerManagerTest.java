package tw.com.ehanlin.tomcatSessionSynchronizer.tomcat8;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.junit.Test;
import tw.com.ehanlin.tomcatSessionSynchronizer.memory.MemorySynchronizer;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import static org.junit.Assert.*;

public class Tomcat8SessionSynchronizerManagerTest {

    private static final Log log = LogFactory.getLog(Tomcat8SessionSynchronizerManagerTest.class);

    @Test
    public void tomcat8SessionTest() throws LifecycleException, IOException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(28080);

        MemorySynchronizer synchronizer = new MemorySynchronizer();
        Tomcat8SessionSynchronizerManager manager = new Tomcat8SessionSynchronizerManager(synchronizer);

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());
        ctx.setSessionTimeout(10);
        ctx.setSessionCookiePath("/");
        ctx.setManager(manager);


        tomcat.addServlet(ctx, "Embedded", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                HttpSession session = req.getSession(true);

                Writer w = resp.getWriter();
                switch(req.getRequestURI()){
                    case "/id" :
                        w.write(session.getId());
                        break;
                    case "/set" :
                        for(String k : req.getParameterMap().keySet()){
                            session.setAttribute(k, req.getParameter(k));
                        }
                        w.write("true");
                        break;
                    case "/unset" :
                        for(String k : req.getParameterMap().keySet()){
                            session.removeAttribute(k);
                        }
                        w.write("true");
                        break;
                    case "/info":
                        StringBuilder stringBuilder = new StringBuilder();
                        Enumeration<String> attributeNames = session.getAttributeNames();
                        while (attributeNames.hasMoreElements()){
                            String attribute = attributeNames.nextElement();
                            stringBuilder.append(attribute);
                            stringBuilder.append(":");
                            stringBuilder.append(session.getAttribute(attribute));
                            stringBuilder.append("\n");
                        }
                        w.write(stringBuilder.toString());
                        break;
                    default:
                        w.write("not in path");
                }
                w.flush();
                w.close();
            }
        });

        ctx.addServletMappingDecoded("/*", "Embedded");
        tomcat.start();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();

        try(CloseableHttpResponse res = httpclient.execute(new HttpGet("http://127.0.0.1:28080/id"), context)){
            if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                throw new RuntimeException(Integer.toString(res.getStatusLine().getStatusCode()));
            }
            String cookieSessionId = "";
            for(Cookie cookie : context.getCookieStore().getCookies()){
                if(cookie.getPath().equals("/") && cookie.getName().equals("JSESSIONID")){
                    cookieSessionId = cookie.getValue();
                }
            }
            assertEquals(EntityUtils.toString(res.getEntity(), "UTF-8"), cookieSessionId);
        }

        try(CloseableHttpResponse res = httpclient.execute(new HttpGet("http://127.0.0.1:28080/set?a=a&b=b"), context)){
            if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                throw new RuntimeException(Integer.toString(res.getStatusLine().getStatusCode()));
            }
            assertEquals(EntityUtils.toString(res.getEntity(), "UTF-8"), "true");
        }

        try(CloseableHttpResponse res = httpclient.execute(new HttpGet("http://127.0.0.1:28080/info"), context)){
            if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                throw new RuntimeException(Integer.toString(res.getStatusLine().getStatusCode()));
            }
            assertEquals(EntityUtils.toString(res.getEntity(), "UTF-8"), "a:a\nb:b\n");
        }

        try(CloseableHttpResponse res = httpclient.execute(new HttpGet("http://127.0.0.1:28080/unset?a=a"), context)){
            if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                throw new RuntimeException(Integer.toString(res.getStatusLine().getStatusCode()));
            }
            assertEquals(EntityUtils.toString(res.getEntity(), "UTF-8"), "true");
        }

        try(CloseableHttpResponse res = httpclient.execute(new HttpGet("http://127.0.0.1:28080/info"), context)){
            if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                throw new RuntimeException(Integer.toString(res.getStatusLine().getStatusCode()));
            }
            assertEquals(EntityUtils.toString(res.getEntity(), "UTF-8"), "b:b\n");
        }

        try{
            if (tomcat.getServer() != null && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
                if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
                    tomcat.stop();
                }
                tomcat.destroy();
            }
        }catch(Throwable ex){

        }

    }

}