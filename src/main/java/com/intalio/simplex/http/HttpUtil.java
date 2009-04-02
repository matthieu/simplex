package com.intalio.simplex.http;

import java.text.MessageFormat;

public class HttpUtil {
    private static final MessageFormat HTML_HEADER = new MessageFormat(
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>\n" +
            "    <title>Simplex, the lightweight SimPEL runtime</title>\n" +
            "    <link rel=\"stylesheet\" href=\"/css/default.css\" type=\"text/css\"/>\n" +
            "    <link rel=\"stylesheet\" href=\"/css/syntax.css\" type=\"text/css\"/>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div id=\"wrap\">\n" +
            " \n" +
            "      <h1 id=\"header\">\n" +
            "        <a href=\"#\">{0}</a>\n" +
            "      </h1>\n" +
            "      <div id=\"content\">");

    private static final String HTML_FOOTER =
            "      </div>\n" +
            " \n" +
            "      <div id=\"footer\">\n" +
            "        Copyright (C) 2008-2009 <a href=\"http://www.intalio.com\">Intalio, Inc</a>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "  </body>\n" +
            "</html>";

    public static String htmlHeader(String title) {
        return HTML_HEADER.format(new Object[] {title});
    }
    
    public static String htmlFooter() {
        return HTML_FOOTER;
    }
}
