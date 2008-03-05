package org.apache.ode.simpel.omodel;

import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;

import java.util.HashMap;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELExpr extends OExpression {
    private String expr;
    private HashMap<String,OScope.Variable> _referencedVariables = new HashMap<String,OScope.Variable>();

    public SimPELExpr(OProcess oProcess) {
        super(oProcess);
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public void addVariable(OScope.Variable var) {
        _referencedVariables.put(var.name, var);
    }

    public OScope.Variable getReferencedVariable(String name) {
        return _referencedVariables.get(name);
    }
}
