package org.apache.ode.simpel;

import junit.framework.TestCase;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeParser;
import org.apache.ode.simpel.antlr.SimPELLexer;
import org.apache.ode.simpel.antlr.SimPELParser;
import org.apache.ode.simpel.antlr.SimPELWalker;
import uk.co.badgersinfoil.e4x.antlr.*;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.HashMap;

public class SimPELTest extends TestCase {
    private static String PROCESS =
//        "function myFunc(p1, p2) {" +
//        "   return (p1 + p2 < 10);" +
//        "}" +
        "process ExternalCounter {\n" +
        "  receive(my_pl, start_op) (msg_in) {\n" +
        "    resp = <root><count start=\"0\">0</count></root>\n" +
        "    while(resp < 10) {\n" +
        "      partner_pl.partner_start_op(msg_in)\n" +
        "      resp = receive(partner_pl, partner_reply_op)\n" +
        "    }\n" +
        "    reply resp\n" +
        "  }\n" +
        "}";

    public static void testSimpleProcess() throws Exception {
        SimPELCompiler.parseProcess(PROCESS);
    }

}

