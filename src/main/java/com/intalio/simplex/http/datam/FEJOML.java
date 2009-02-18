/*
 * Simplex, lightweight SimPEL server
 * Copyright (C) 2008-2009  Intalio, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.intalio.simplex.http.datam;

import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * This ugly name stands for Form Encoded - JSON - X(HT)ML in case you were wondering, which are all
 * the formats this attempts to translate. They're all based on a common data model that's a sane subset
 * of each format.
 * TODO this is barely better than a mock
 */
public class FEJOML {

    public static final String JSON = MediaType.APPLICATION_JSON;
    public static final String XML = MediaType.APPLICATION_XML;
    public static final String XHTML = MediaType.APPLICATION_XHTML_XML;
    public static final String FUE = MediaType.APPLICATION_FORM_URLENCODED;
    public static final String PLAIN = "text/plain";

    public static boolean recognizeType(String cntType) {
        if (cntType.equals(JSON) || cntType.equals(FUE) || cntType.equals(XHTML) || cntType.equals(XML) || cntType.equals(XML))
           return true;
        else return false;
    }

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

    public static Element toXML(String in, String from) throws IOException, SAXException {
        if (from.equals(XML)) return DOMUtils.stringToDOM(in);
        else if (from.equals(PLAIN)) return plainToXML(in);
        else throw new UnsupportedOperationException("not yet");
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

    /**
     * The conversion ignores the first element, considering it as a simple hash wrapper. Then it
     * handles each sub-element using a Rails-style conversion like foo[bar]=baz
     */
    private static String xmlToForm(Node in) {
        Element root = null;
        if (in.getNodeType() == Node.TEXT_NODE) return ((Text)in).getWholeText() + "=";
        else if (in.getNodeType() == Node.DOCUMENT_NODE) root = ((Document)in).getDocumentElement();
        else if (in.getNodeType() != Node.ELEMENT_NODE)
            throw new RuntimeException("Don't know how to convert node type " + in.getNodeType() + ": " + in);

        if (root == null) root = (Element) in;

        StringBuffer res = new StringBuffer();
        NodeList firstChildren = root.getChildNodes();
        for (int m = 0; m < firstChildren.getLength(); m++) {
            Node c = firstChildren.item(m);
            if (c.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) c;
                // TODO escape characters
                if (DOMUtils.getFirstChildElement(child) == null) {
                    if (res.length() > 0) res.append("&");
                    res.append(child.getNodeName()).append("=").append(child.getTextContent());
                } else {
                    throw new UnsupportedOperationException("Only know how to handle simple text elements for now.");
                }
            }
        }
        return res.toString();
    }

    private static Element plainToXML(String cnt) {
        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElement("wrapper");
        wrapper.setTextContent(cnt);
        doc.appendChild(wrapper);
        return wrapper;
    }

    private static String pluralize(String s) {
        // Doing it the lazy way for now TODO get all pluralization rules from Rails
        return s + "s";
    }

}
