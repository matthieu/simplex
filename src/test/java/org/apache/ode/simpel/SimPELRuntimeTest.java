package org.apache.ode.simpel;

import junit.framework.TestCase;
import org.apache.ode.EmbeddedServer;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELRuntimeTest extends TestCase {

    private static final String HELLO_WORLD =
            "process HelloWorld {\n" +
            "   receive(myPl, helloOp) { |msgIn|\n" +
            "       msgOut = msgIn + \" World\";\n" +
            "       reply(msgOut);\n" +
            "   }\n" +
            "}";

    private static final String XML_DATA_MANIPULATION =
            "process XmlData {\n" +
            "    receive(dataPl, dataOp) { |msgIn|\n" +
            "        friendInfo = <friend></friend>;\n" + // TODO check template style E4X
            "        friendInfo.name = msgIn.person.firstName + \" \" + msgIn.person.lastName;\n" +
            "        friendInfo.phone = msgIn.person.phone;\n" +
            "        reply(friendInfo);\n" +
            "    }\n" +
            "}";

    private static final String SIMPLE_IF =
            "process SimpleIf {\n" +
            "    receive(ifPl, ifOp) { |quantity|\n" +
            "        if (quantity > 20) {\n" +
            "            status = 0; \n" +
            "        } else { \n" +
            "            status = 1; \n" +
            "        }\n" +
            "        reply(status);\n" +
            "    }\n" +
            "}";

    public void testHelloWorldComplete() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(HELLO_WORLD);

        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition/HelloWorld", "helloOpRequest");
        wrapper.setTextContent("Hello");

        Element result = server.sendMessage("myPl", "helloOp", wrapper);
        assertTrue(DOMUtils.domToString(result).indexOf("Hello World") > 0);
    }

    public void testXmlData() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(XML_DATA_MANIPULATION);
        Element wrapper = DOMUtils.stringToDOM(
                "<xd:dataOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/XmlData\">\n" +
                "    <person>\n" +
                "       <firstName>John</firstName>\n" +
                "       <lastName>Doe</lastName>\n" +
                "       <phone>(999)999-9999</phone>\n" +
                "    </person>\n" +
                "</xd:dataOpRequest>");

        Element result = server.sendMessage("dataPl", "dataOp", wrapper);
        assertNotNull(result);
        System.out.println(DOMUtils.domToString(result));
        assertNotNull(DOMUtils.findChildByName(result, new QName(null, "phone")));
        assertNotNull(DOMUtils.findChildByName(result, new QName(null, "name")));
        assertEquals("(999)999-9999", DOMUtils.findChildByName(result, new QName(null, "phone")).getTextContent());
        assertEquals("John Doe", DOMUtils.findChildByName(result, new QName(null, "name")).getTextContent());
    }

    public void testSimpleIf() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(SIMPLE_IF);
        Element wrapper = DOMUtils.stringToDOM(
                "<xd:ifOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/SimpleIf\">30</xd:ifOpRequest>");
        Element result = server.sendMessage("ifPl", "ifOp", wrapper);
        assertNotNull(result);
        assertEquals(0.0f, Float.parseFloat(result.getTextContent()));
        System.out.println(DOMUtils.domToString(result));

        wrapper = DOMUtils.stringToDOM(
                "<xd:ifOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/SimpleIf\">10</xd:ifOpRequest>");
        result = server.sendMessage("ifPl", "ifOp", wrapper);
        assertNotNull(result);
        assertEquals(1.0f, Float.parseFloat(result.getTextContent()));
    }
}
