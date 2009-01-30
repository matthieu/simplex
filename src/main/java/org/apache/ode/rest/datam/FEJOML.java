package org.apache.ode.rest.datam;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.ode.utils.DOMUtils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.List;

/**
 * This ugly name stands for Form Encoded - JSON - X(HT)ML in case you were wondering, which are all
 * the formats this attempts to translate. They're all based on a common data model that's a sane subset
 * of each format.
 * TODO this is barely better than a mock
 */
public class FEJOML {

    public static final String JSON = "application/json";
    public static final String XML = "application/xml";
    public static final String XHTML = "application/xhtml+xml";
    public static final String FUE = "application/x-www-form-urlencoded";

    public static String convert(String in, String from, String to) {
        throw new UnsupportedOperationException("not yet");
    }

    public static Element formToXML(MultivaluedMap<String, String> formParams) {
        Document doc = DOMUtils.newDocument();
        Element form = doc.createElement("form");
        doc.appendChild(form);
        for (Map.Entry<String, List<String>> param : formParams.entrySet()) {
            if (param.getValue().size() == 1) {
                Element k = doc.createElement(param.getKey());
                k.setTextContent(param.getValue().get(0));
                form.appendChild(k);
            } else {
                // TODO support Rails encoding conventions as well
                Element container = doc.createElement(pluralize(param.getKey()));
                for (String s : param.getValue()) {
                    Element e = doc.createElement(param.getKey());
                    e.setTextContent(s);
                    container.appendChild(e);
                }
            }
        }
        return form;
    }

    public static String fromXML(Node in, String to) {
        if (to.equals(XML)) return DOMUtils.domToString(in);
        else if (to.equals(FUE)) return xmlToHtml(in);
        else throw new UnsupportedOperationException("Not yet");
    }

    private static String xmlToHtml(Node in) {
        StringBuffer html = new StringBuffer();
        html.append("<html>\n    <body>\n");
        if (in.getNodeType() == Node.TEXT_NODE || in.getTextContent() != null && in.getTextContent().trim().length() > 0)
            html.append("<p>").append(in.getTextContent()).append("</p>");
        else {
            // todo this is super quick and dirty
            NodeList children = in.getChildNodes();
            for (int m = 0; m < children.getLength(); m++) {
                if (children.item(m).getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) children.item(m);
                    html.append("        <p><label>").append(child.getNodeName())
                            .append("</label>: ").append(child.getTextContent());
                }
            }
        }
        html.append("    </body>\n</html>");
        return html.toString();
    }

    private static String pluralize(String s) {
        // Doing it the lazy way for now TODO get all pluralization rules from Rails
        return s + "s";
    }

}
