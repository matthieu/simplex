package org.apache.ode.simpel;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.Token;
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

public class SimPELCompiler {

    public static void parseProcess(String process) throws Exception {
        ANTLRReaderStream charstream = new ANTLRReaderStream(new StringReader(process));
        SimPELLexer lexer = new SimPELLexer(charstream);
        LinkedListTokenSource linker = new LinkedListTokenSource(lexer);
        LinkedListTokenStream tokenStream = new LinkedListTokenStream(linker);
        SimPELParser parser = new SimPELParser(tokenStream);
        parser.setTreeAdaptor(new LinkedListTreeAdaptor());
        parser.setInput(lexer, charstream);

        SimPELParser.program_return result = parser.program();
        // pull out the tree and cast it
        Tree t = (Tree)result.getTree();

        if (t == null) {
            System.out.println("There were parser errors.");
        } else {
            // Pass the tree to the walker for compilation
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
            SimPELWalker walker = new SimPELWalker(nodes);
            HashMap<Integer, Integer> tokenMapping = buildTokenMap(E4XParser.tokenNames, E4XLexer.class, SimPELWalker.class);
            System.out.println("=> " + tokenMapping);
            rewriteTokens(tokenMapping, E4XParser.tokenNames, (LinkedListTree) t, walker, false);
            System.out.println("\n"+t.toStringTree()); // print out the tree

            nodes.setTokenStream(tokenStream);
            walker.program();
        }
    }

    private static void rewriteTokens(HashMap<Integer, Integer> tokenMapping, String[] tokenNames,
                                      LinkedListTree t, TreeParser targetLexer, boolean xmlNode) {
//        System.out.println("### " + t.token);
        if (t.token != null && tokenMapping.get(t.token.getType()) != null && (in(tokenNames, t.token.getText()) || xmlNode)) {
            t.token.setType(tokenMapping.get(t.token.getType()));
            xmlNode = true;
            System.out.print(t.token.getText() + "(" + t.token.getType() + ") ");
        }
        for(int m = 0; m < t.getChildCount(); m++) {
            rewriteTokens(tokenMapping, tokenNames, (LinkedListTree) t.getChild(m), targetLexer, xmlNode);
        }
    }

    /**
     * Maps all token types from the source to a token type for the target when source and target
     * have tokens with matching names.
     * @param tokenNames
     * @param source
     * @param target
     * @return
     */
    private static HashMap<Integer, Integer> buildTokenMap(String[] tokenNames, Class source, Class target) {
        HashMap<Integer, Integer> tokenMapping = new HashMap<Integer, Integer>();
        for (String name : tokenNames) {
            try {
                Field targetField = target.getDeclaredField(name);
                Field sourceField = source.getDeclaredField(name);
                tokenMapping.put((Integer)sourceField.get(null), (Integer)targetField.get(null));
            } catch (Exception e) { /* Exception means no such token */ }
        }
        return tokenMapping;
    }

    private static boolean in(String[] arr, String elmt) {
        for (String s : arr)
            if (s.equals(elmt)) return true;
        return false;
    }

}

