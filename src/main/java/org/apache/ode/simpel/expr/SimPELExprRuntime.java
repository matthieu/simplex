package org.apache.ode.simpel.expr;

import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.explang.ConfigurationException;
import org.apache.ode.bpel.explang.EvaluationContext;
import org.apache.ode.bpel.explang.ExpressionLanguageRuntime;
import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OMessageVarType;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.simpel.omodel.SimPELExpr;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.xsd.Duration;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Delegator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.xml.XMLLib;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELExprRuntime implements ExpressionLanguageRuntime {
    public void initialize(Map map) throws ConfigurationException {
    }

    public String evaluateAsString(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return null;
    }

    public boolean evaluateAsBoolean(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Number evaluateAsNumber(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List evaluate(OExpression oexpr, EvaluationContext evaluationContext) throws FaultException {
        Context cx = ContextFactory.getGlobal().enterContext();
        ODEDelegator scope = new ODEDelegator(cx.initStandardObjects(), evaluationContext, (SimPELExpr)oexpr);

        Object res = cx.evaluateString(scope, ((SimPELExpr)oexpr).getExpr(), "<expr>", 0, null);
        ArrayList<Node> resList = new ArrayList<Node>(1);

        if (res instanceof String) {
            Document doc = DOMUtils.newDocument();
            Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition", "simpelWrapper");
            wrapper.setTextContent((String) res);
            resList.add(wrapper);
        }
        if (res instanceof Node) resList.add((Node) res);
        return resList;
    }

    public Calendar evaluateAsDate(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Duration evaluateAsDuration(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Node evaluateNode(OExpression oExpression, EvaluationContext evaluationContext) throws FaultException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class ODEDelegator extends Delegator {
        private EvaluationContext _evaluationContext;
        private XMLLib _xmlLib;
        private SimPELExpr _expr;

        private ODEDelegator(Scriptable obj, EvaluationContext evaluationContext, SimPELExpr expr) {
            super(obj);
            _evaluationContext = evaluationContext;
            _expr = expr;
        }

        public void setXmlLib(XMLLib _xmlLib) {
            this._xmlLib = _xmlLib;
        }

        public Object get(String name, Scriptable start) {
            try {
                // TODO this assumes message type with a single part for all variables, valid?
                OScope.Variable v = _expr.getReferencedVariable(name);
                Node node = _evaluationContext.readVariable(v,
                        ((OMessageVarType)v.type).parts.values().iterator().next());
                if (node.getTextContent() != null) return node.getTextContent();
                // TODO wrap xml nodes
                return node;
            } catch (Exception e) {
                throw new RuntimeException("Variable " + name + " has never been initialized.");
            }
        }

        public boolean has(String name, Scriptable start) {
            return get(name, start) != null;
        }
    }
}
