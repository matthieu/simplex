package org.apache.ode.simpel;

import junit.framework.TestCase;
import org.apache.ode.EmbeddedServer;
import org.apache.ode.embed.MessageSender;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    public void testHelloWorld() throws Exception {
        EmbeddedServer server = new EmbeddedServer();
        server.start();
        server.deploy(HELLO_WORLD);

        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition/HelloWorld", "helloOpRequest");
        wrapper.setTextContent("Hello");

        Element result = server.sendMessage("myPl", "helloOp", wrapper);
        assertTrue(DOMUtils.domToString(result).indexOf("Hello World") > 0);
    }

    private static final String XML_DATA_MANIPULATION =
            "process XmlData {\n" +
            "    receive(dataPl, dataOp) { |msgIn|\n" +
            "        friendInfo = <friend></friend>;\n" +
            "        friendInfo.name = msgIn.firstName + \" \" + msgIn.lastName;\n" +
            "        friendInfo.phone = msgIn.phone;\n" +
            "        reply(friendInfo);\n" +
            "    }\n" +
            "}";

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
        // Eliminating the wrapper
        result = DOMUtils.getFirstChildElement(result);
        assertNotNull(DOMUtils.findChildByName(result, new QName(null, "phone")));
        assertNotNull(DOMUtils.findChildByName(result, new QName(null, "name")));
        assertEquals("(999)999-9999", DOMUtils.findChildByName(result, new QName(null, "phone")).getTextContent());
        assertEquals("John Doe", DOMUtils.findChildByName(result, new QName(null, "name")).getTextContent());
    }

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

    public static final String INVOKE_ONE_WAY =
            "process InvokeOneWay {\n" +
            "    receive(iowPl, iowOp) { |status|\n" +
            "        invoke(partnerPl, partnerOp, status);\n" +
            "        status = \"ok\";\n" +
            "        reply(status);\n" +
            "    }\n" +
            "}";

    public void testInvokeOneWay() throws Exception {
        final Boolean[] received = new Boolean[] { false };

        EmbeddedServer server = new EmbeddedServer();
        server.options.setMessageSender(new MessageSender() {
            public Node send(String recipient, String operation, Node message) {
                if (recipient.equals("partnerPl") && operation.equals("partnerOp")
                        && message.getNodeType() == Node.TEXT_NODE && message.getTextContent().equals("ok?"))
                    received[0] = true;
                return null;
            }
        });
        server.start();
        server.deploy(INVOKE_ONE_WAY);

        Element wrapper = DOMUtils.stringToDOM(
                "<xd:iowOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/InvokeOneWay\">ok?</xd:iowOpRequest>");
        Element result = server.sendMessage("iowPl", "iowOp", wrapper);
        assertNotNull(result);
        System.out.println(DOMUtils.domToString(result));
        assertEquals("ok", result.getTextContent());
        assertTrue(received[0]);
    }

    public static final String INVOKE_TWO_WAYS =
            "process InvokeTwoWays {\n" +
            "    receive(itwPl, itwOp) { |initial|\n" +
            "        operands = <operands></operands>; \n"+
            "        operands.op1 = initial; \n"+
            "        operands.op2 = 3; \n"+
            "        invoke(calculator, add, operands) { |result| \n" +
            "           response = result; \n" +
            "        }" +
            "        reply(result);\n" +
            "    }\n" +
            "}";

    public void testInvokeTwoWays() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.options.setMessageSender(new MessageSender() {
            public Node send(String recipient, String operation, Node elmt) {
                if (recipient.equals("calculator") && operation.equals("add")) {
                    Document doc = DOMUtils.newDocument();
                    int result = 0;
                    NodeList nl = elmt.getChildNodes();
                    for (int m = 0; m < nl.getLength(); m++)
                        if (nl.item(m).getNodeType() == Node.ELEMENT_NODE)
                            result += Integer.parseInt(nl.item(m).getTextContent());
                    return doc.createTextNode(""+result);
                } else return null;
            }
        });
        server.start();
        server.deploy(INVOKE_TWO_WAYS);

        Element wrapper = DOMUtils.stringToDOM(
                "<xd:itwOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/InvokeTwoWays\">7</xd:itwOpRequest>");
        Element result = server.sendMessage("itwPl", "itwOp", wrapper);
        assertNotNull(result);
        System.out.println(DOMUtils.domToString(result));
        assertEquals("10", result.getTextContent());
    }

    private static final String JS_GLOBAL_STATE =
            "var append = \" Yeeha!\"; \n" +
            "function helloPrepend(p) { return \"Hello \" + p; }; \n" +
            "\n" +
            "process JsGlobalState {\n" +
            "   receive(myPl, helloOp) { |msgIn|\n" +
            "       msgOut = helloPrepend(msgIn) + append;\n" +
            "       reply(msgOut);\n" +
            "   }\n" +
            "}";

    public void testJSState() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(JS_GLOBAL_STATE);

        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition/JsGlobalState", "helloOpRequest");
        wrapper.setTextContent("World.");

        Element result = server.sendMessage("myPl", "helloOp", wrapper);
        System.out.println(":: " + DOMUtils.domToString(result));
        assertTrue(DOMUtils.domToString(result).indexOf("Hello World. Yeeha!") > 0);
    }

    private static final String SIMPLE_CORRELATION =
            "function getExchangeId(msg) { return msg.id; }; \n" +
            "\n" +
            "process SimpleCorrelation { \n" +
            "   var cid unique; \n" +
            "   receive(myPl, firstOp) { |msgIn| \n" +
            "       cid = msgIn.id; \n" +
            "       text = msgIn.text; \n" +
            "   }\n" +
            "   receive(myPl, secondOp, {getExchangeId: cid}) { |secMsgIn| \n" +
            "       text = text + secMsgIn.text + cid; \n" +
            "       reply(text); \n" +
            "   }\n" +
            "}";

    public void testSimpleCorrelation() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(SIMPLE_CORRELATION);

        Element wrapper = DOMUtils.stringToDOM(
                "<xd:firstOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/SimpleCorrelation\">" +
                    "<exchange>" +
                        "<id>XYZ1</id>" +
                        "<text>foo</text>" +
                    "</exchange>" +
                "</xd:firstOpRequest>");
        server.sendMessage("myPl", "firstOp", wrapper);

        wrapper = DOMUtils.stringToDOM(
                "<xd:secondOpRequest xmlns:xd=\"http://ode.apache.org/simpel/1.0/definition/SimpleCorrelation\">" +
                    "<exchange>" +
                        "<id>XYZ1</id>" +
                        "<text>bar</text>" +
                    "</exchange>" +
                "</xd:secondOpRequest>");
        Element result = server.sendMessage("myPl", "secondOp", wrapper);
        assertTrue(DOMUtils.domToString(result).indexOf("foobar") > 0);
    }

    private static final String WHILE_LOOP =
            "process WhileLoop { \n" +
            "   receive(myPl, firstOp) { |counter| \n" +
            "       i = 0; j = 1; cur = 1; \n" +
            "       while (cur <= counter) { \n" +
            "           k = i; i = j; j = k+j; cur = cur+1; \n" +
            "       } \n" +
            "       reply(i); \n" +
            "   }\n" +
            "}";

    public void testWhile() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(WHILE_LOOP);

        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition/WhileLoop", "firstOpRequest");
        wrapper.setTextContent("20");

        Element result = server.sendMessage("myPl", "firstOp", wrapper);
        System.out.println(":: " + DOMUtils.domToString(result));
        assertTrue(DOMUtils.domToString(result).indexOf("6765") > 0);
    }

}
