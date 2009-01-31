package org.apache.ode.simpel;

import junit.framework.TestCase;
import org.antlr.runtime.RecognitionException;
import org.apache.ode.bpel.rtrep.v2.OProcess;
import org.apache.ode.Descriptor;
import org.apache.ode.simpel.util.DefaultErrorListener;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELCompilerTest extends TestCase {

    Descriptor desc;

    public SimPELCompilerTest() {
        super();
        desc = new Descriptor();
        desc.setRestful(false);
    }

    public void testAllOk() throws Exception {
        compileAllInFile("compile-tests-ok.simpel", true);
    }

    public void testAllKo() throws Exception {
        compileAllInFile("compile-tests-ko.simpel", false);
    }

    /**
     * If this was Ruby, I'd just dynamically create methods for each tested process
     * and we'd have one clean method for each test case. But this is Java so there's
     * only one reported test for all the processes.
     * @throws Exception
     */
    private void compileAllInFile(String file, boolean forSuccess) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(
                getClass().getClassLoader().getResource(file).getFile()));

        int testCount = 0;
        String line;
        StringBuffer processBody = new StringBuffer();
        SimPELCompiler comp = new SimPELCompiler();
        boolean failed = false;
        String testCaseName = "";

        // Priming the pump
        while (!reader.readLine().startsWith("#="));
        testCaseName = reader.readLine().trim().substring(2);
        reader.readLine();reader.readLine();

        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("#=")) {
                // Found next test case divider, process is complete so we can compile
                try {
                    comp.compileProcess(processBody.toString(), desc);
                    System.out.println("Test case " + testCaseName + " compiled properly.");
                    if (!forSuccess) failed = true;
                } catch (CompilationException e) {
                    System.out.println("There were errors while compiling test case " + testCaseName);
                    System.out.println(e);
                    if (forSuccess) failed = true;
                }
                testCount++;

                // Preparing for next test case
                testCaseName = reader.readLine().trim().substring(2);
                reader.readLine();reader.readLine();
                processBody = new StringBuffer();
                comp.setErrorListener(new DefaultErrorListener());
            } else {
                processBody.append(line).append("\n");
            }
        }

        // And the last one
        try {
            comp.compileProcess(processBody.toString(), desc);
        } catch (CompilationException e) {
            System.err.println("There were errors while compiling test case " + testCaseName);
            System.err.println(e);
            if (forSuccess) failed = true;
        }
        testCount++;

        if (failed) {
            fail("There were failures.");
        } else {
            System.out.println("\nTested " + testCount + " processes successfully.");
        }
    }

    public void testLoanApproval() throws Exception {
        SimPELCompiler c = compiler();
        c.compileProcess(readProcess("loan-approval.simpel"), desc);
        reportErrors("Loan approval", c);
    }

    public void testAuction() throws Exception {
        SimPELCompiler c = compiler();
        c.compileProcess(readProcess("auction.simpel"), desc);
        reportErrors("Auction service", c);
    }

    public void testTaskManager() throws Exception {
        SimPELCompiler c = compiler();
        c.compileProcess(readProcess("task-manager.simpel"), desc);
        reportErrors("Auction service", c);
    }

    private String readProcess(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(
                getClass().getClassLoader().getResource(fileName).getFile()));

        String line;
        StringBuffer processText = new StringBuffer();
        while ((line = reader.readLine()) != null) processText.append(line).append("\n");
        return processText.toString();
    }

    private SimPELCompiler compiler() {
        TestErrorListener l = new TestErrorListener();
        SimPELCompiler comp = new SimPELCompiler();
        comp.setErrorListener(l);
        return comp;
    }

    private void reportErrors(String testName, SimPELCompiler c) {
        if (((TestErrorListener)c.getErrorListener()).messages.toString().length() > 0) {
            System.out.println(testName+" failed to compile:\n");
            System.out.println(((TestErrorListener)c.getErrorListener()).messages.toString());
            fail("There were failures.");
        }
    }

    private static class TestErrorListener implements ErrorListener {
        public StringBuffer messages = new StringBuffer();

        public List<CompilationException.Error> getErrors() {
            return null;
        }

        public void reportRecognitionError(int line, int column, String message, Exception e) {
            messages.append(" - line ").append(line).append(": ").append(message).append("\n");
        }
    }
}
