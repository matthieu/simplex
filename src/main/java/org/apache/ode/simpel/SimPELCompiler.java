package org.apache.ode.simpel;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeParser;
import org.apache.ode.simpel.antlr.SimPELLexer;
import org.apache.ode.simpel.antlr.SimPELParser;
import org.apache.ode.simpel.antlr.SimPELWalker;
import org.apache.ode.simpel.util.DefaultErrorListener;
import uk.co.badgersinfoil.e4x.antlr.*;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class SimPELCompiler {

    private ErrorListener el;

    public ErrorListener getErrorListener() {
        return el;
    }

    public void setErrorListener(ErrorListener el) {
        this.el = el;
    }

    public void compileProcess(String process) throws Exception {
        ANTLRReaderStream charstream = new ANTLRReaderStream(new StringReader(process));
        ErrorListener errListener = (el == null ? new DefaultErrorListener() : el);

        SimPELLexer lexer = new SimPELLexer(charstream);
        lexer.setErrorListener(errListener);
        LinkedListTokenSource linker = new LinkedListTokenSource(lexer);
        LinkedListTokenStream tokenStream = new LinkedListTokenStream(linker);

        SimPELParser parser = new SimPELParser(tokenStream);
        parser.setTreeAdaptor(new LinkedListTreeAdaptor());
        parser.setInput(lexer, charstream);
        parser.setErrorListener(errListener);

        SimPELParser.program_return result = parser.program();
        // pull out the tree and cast it
        Tree t = (Tree)result.getTree();

        if (t != null) {
            //  Handle functions separately
            handleFunctions(t);

            // Pass the tree to the walker for compilation
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
            SimPELWalker walker = new SimPELWalker(nodes);
            walker.setErrorListener(el);
            HashMap<Integer, Integer> tokenMapping = buildTokenMap(E4XParser.tokenNames, E4XLexer.class, SimPELWalker.class);
            rewriteTokens(tokenMapping, E4XParser.tokenNames, (LinkedListTree) t, walker, false);
//            System.out.println("\n"+t.toStringTree()); // print out the tree

            nodes.setTokenStream(tokenStream);
            walker.program();
        }
    }

    private void handleFunctions(Tree t) {
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for(int m = 0; m < t.getChildCount(); m++) {
            if ("function".equals(t.getChild(m).getText())) {
                Tree funcTree = t.getChild(m);

                // Extracting function structure
                ArrayList<String> params = new ArrayList<String>();
                StringBuffer body = new StringBuffer();
                boolean signature = true;
                for (int p = 2; p < funcTree.getChildCount(); p++) {
                    String txt = funcTree.getChild(p).getText();
                    if (")".equals(txt)) { signature = false; continue; }
                    
                    if (signature) params.add(txt);
                    else body.append(txt);
                }
                System.out.println("Found function: " + funcTree.getChild(0) + "(" + params + ") {"
                        + body.toString() + "}");

                toRemove.add(m);
            }
        }
        // Voluntarily not using an iterator, we want to be index based
        for(int m = 0; m < toRemove.size(); m++) {
            t.deleteChild(toRemove.get(m));
            for(int n = 0; n < toRemove.size(); n++) {
                if (toRemove.get(n) > toRemove.get(m)) toRemove.set(n, toRemove.get(n) - 1);
            }
        }
    }

    private void rewriteTokens(HashMap<Integer, Integer> tokenMapping, String[] tokenNames,
                                      LinkedListTree t, TreeParser targetLexer, boolean xmlNode) {
        if (t.token != null && tokenMapping.get(t.token.getType()) != null && (in(tokenNames, t.token.getText()) || xmlNode)) {
            t.token.setType(tokenMapping.get(t.token.getType()));
            xmlNode = true;
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
    private HashMap<Integer, Integer> buildTokenMap(String[] tokenNames, Class source, Class target) {
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

    private boolean in(String[] arr, String elmt) {
        for (String s : arr)
            if (s.equals(elmt)) return true;
        return false;
    }

}

