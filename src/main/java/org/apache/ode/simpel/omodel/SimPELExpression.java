package org.apache.ode.simpel.omodel;

import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OExpressionLanguage;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELExpression extends OExpression {
    private String expr;

    public SimPELExpression(OProcess oProcess, String expr) {
        super(oProcess);
        this.expr = expr;
        OExpressionLanguage oelang = new OExpressionLanguage(oProcess, null);
        oelang.expressionLanguageUri = "http://ode.apache.org/simpel/1.0/expr";
        expressionLanguage = oelang;
    }
}
