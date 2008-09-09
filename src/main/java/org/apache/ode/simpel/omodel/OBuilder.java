package org.apache.ode.simpel.omodel;

import org.apache.log4j.Logger;

import org.apache.ode.bpel.compiler.v2.BaseCompiler;
import org.apache.ode.bpel.rtrep.v2.*;

import org.apache.ode.bpel.compiler.bom.Bpel20QNames;
import org.apache.ode.simpel.wsdl.SimPELInput;
import org.apache.ode.simpel.wsdl.SimPELOperation;
import org.apache.ode.simpel.wsdl.SimPELOutput;
import org.apache.ode.simpel.wsdl.SimPELPortType;
import org.apache.ode.utils.GUID;

import javax.wsdl.PortType;
import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class OBuilder extends BaseCompiler {
    private static final Logger __log = Logger.getLogger(OBuilder.class);
    private static final String SIMPEL_NS = "http://ode.apache.org/simpel/1.0/definition";

    private OExpressionLanguage _exprLang;
    private OExpressionLanguage _konstExprLang;
    private String _processNS;
    private HashMap<String,String> namespaces = new HashMap<String,String>();
    private HashMap<String, OPartnerLink> partnerLinks = new HashMap<String,OPartnerLink>();
    private HashMap<String,OScope.Variable> variables = new HashMap<String,OScope.Variable>();
    private boolean firstReceive = true;

    public OBuilder() {
        HashMap<String, String> exprRuntime = new HashMap<String, String>();
        exprRuntime.put("runtime-class", "org.apache.ode.simpel.expr.E4XExprRuntime");
        _exprLang = new OExpressionLanguage(_oprocess, exprRuntime);
        _exprLang.expressionLanguageUri = SIMPEL_NS + "/exprLang";
    }

    public StructuredActivity build(Class oclass, OScope oscope, StructuredActivity parent, Object... params) {
        try {
            OActivity oactivity = (OActivity) oclass
                    .getConstructor(OProcess.class, OActivity.class).newInstance(_oprocess, parent.getOActivity());
            Method buildMethod = null;
            for (Method method : OBuilder.class.getMethods())
                if (method.getName().equals("build" + oclass.getSimpleName().substring(1))) buildMethod = method;

            if (buildMethod == null) throw new RuntimeException("No builder for class " + oclass.getSimpleName());

            Object[] buildParams = new Object[params.length + 2];
            System.arraycopy(params, 0, buildParams, 2, params.length);
            buildParams[0] = oactivity;
            buildParams[1] = oscope;
            StructuredActivity result = (StructuredActivity) buildMethod.invoke(this, buildParams);
            if (result != null) parent.run((OActivity) result.getOActivity());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't build activity of type " + oclass, e);
        }
    }

    public static abstract class StructuredActivity<T> {
        private T _oact;
        public StructuredActivity(T oact) {
            _oact = oact;
        }
        public T getOActivity() {
            return _oact;
        }
        public abstract void run(OActivity child);
    }
    public static class SimpleActivity<T> extends StructuredActivity<T> {
        public SimpleActivity(T oact) {
            super(oact);
        }
        public void run(OActivity child) { /* Do nothing */ }
    }

    public StructuredActivity<OScope> buildProcess(String prefix, String name) {
        _oprocess = new OProcess(Bpel20QNames.NS_WSBPEL2_0_FINAL_EXEC);
        _oprocess.processName = name;
        _oprocess.guid = new GUID().toString();
        _oprocess.constants = makeConstants();
        _oprocess.compileDate = new Date();
        if (namespaces.get(prefix) == null) _oprocess.targetNamespace = SIMPEL_NS;
        else _oprocess.targetNamespace = namespaces.get(prefix);

        _oprocess.expressionLanguages.add(_exprLang);
        _processNS = SIMPEL_NS + "/" + name;

        _konstExprLang = new OExpressionLanguage(_oprocess, null);
        _konstExprLang.expressionLanguageUri = "uri:www.fivesight.com/konstExpression";
        _konstExprLang.properties.put("runtime-class",
                "org.apache.ode.bpel.rtrep.v2.KonstExpressionLanguageRuntimeImpl");
        _oprocess.expressionLanguages.add(_konstExprLang);

        final OScope processScope = new OScope(_oprocess, null);
        processScope.name = "__PROCESS_SCOPE:" + name;
        _oprocess.procesScope = processScope;
        return buildScope(processScope, null);
    }

    public StructuredActivity<OScope> buildScope(final OScope oscope, OScope parentScope) {
        return new StructuredActivity<OScope>(oscope) {
            public void run(OActivity child) {
                oscope.activity = child;
            }
        };
    }

    public StructuredActivity<OSwitch> buildSwitch(final OSwitch oswitch, OScope parentScope, SimPELExpr condExpr) {
        final OSwitch.OCase success = new OSwitch.OCase(_oprocess);
        success.expression = condExpr;
        success.expression.expressionLanguage = _exprLang;
        oswitch.addCase(success);

        return new StructuredActivity<OSwitch>(oswitch) {
            public void run(OActivity child) {
                if (success.activity == null) success.activity = child;
                else {
                    OSwitch.OCase opposite = new OSwitch.OCase(_oprocess);
                    opposite.expression = booleanExpr(true);
                    opposite.activity = child;
                    oswitch.addCase(opposite);
                }
            }
        };
    }

    public SimpleActivity buildPickReceive(OPickReceive receive, OScope oscope, String partnerLink, String operation) {
        OPickReceive.OnMessage onMessage = new OPickReceive.OnMessage(_oprocess);
        onMessage.partnerLink = buildPartnerLink(oscope, partnerLink, operation, true);
        onMessage.operation = onMessage.partnerLink.myRolePortType.getOperation(operation, null, null);

        if (firstReceive) {
            firstReceive = false;
            onMessage.partnerLink.addCreateInstanceOperation(onMessage.operation);
            receive.createInstanceFlag = true;
        }

        onMessage.activity = new OEmpty(_oprocess, receive);
        receive.onMessages.add(onMessage);

        return new SimpleActivity<OPickReceive>(receive);
    }

    public StructuredActivity buildSequence(final OSequence seq, OScope oscope) {
        return new StructuredActivity<OSequence>(seq) {
            public void run(OActivity child) {
                seq.sequence.add(child);
            }
        };
    }

    public SimpleActivity buildAssign(OAssign oassign, OScope oscope, String lexpr, SimPELExpr rexpr) {
        OAssign.Copy ocopy = new OAssign.Copy(_oprocess);
        oassign.operations.add(ocopy);

        OAssign.VariableRef vref = new OAssign.VariableRef(_oprocess);
        String lvar = lexpr.split("\\.")[0];
        vref.variable = resolveVariable(oscope, lvar);
        vref.part = new OMessageVarType.Part(_oprocess, "payload",
                new OElementVarType(_oprocess, new QName(_processNS, "simpelWrapper")));
        ocopy.to = vref;

        rexpr.setLValue(lexpr);
        rexpr.setLVariable(lvar);
        rexpr.expressionLanguage = _exprLang;
        ocopy.from = new OAssign.Expression(_oprocess, rexpr);
        return new SimpleActivity<OAssign>(oassign);
    }

    public SimpleActivity buildReply(OReply oreply, OScope oscope, OPickReceive oreceive,
                             String var, String partnerLink, String operation) {
        oreply.variable = resolveVariable(oscope, var, operation, false);
        if (partnerLink == null) {
            if (oreceive == null) throw new RuntimeException("No parent receive but reply with var " + var +
                    " has no partnerLink/operation information.");
            oreply.partnerLink = oreceive.onMessages.get(0).partnerLink;
            oreply.operation = oreceive.onMessages.get(0).operation;
        } else {
            oreply.partnerLink = buildPartnerLink(oscope, partnerLink, operation, true);
            oreply.operation = oreply.partnerLink.myRolePortType.getOperation(operation, null, null); 
        }
        // Adding partner role
        buildPartnerLink(oscope, oreply.partnerLink.name, oreply.operation.getName(), false);
        oreply.operation.setOutput(new SimPELOutput(new QName(_processNS, operation + "Response")));
        return new SimpleActivity<OReply>(oreply);
    }

    public void setBlockParam(OScope oscope, OActivity blockActivity, String varName) {
        // The AST for block activities is something like:
        //    (SEQUENCE (activity) (SEQUENCE varIds otherActivities))
        // The parent here is the first sequence so we just set the varIds on its first child activity
        if (blockActivity == null || !(blockActivity instanceof OSequence)) {
            __log.warn("Can't set block parameter with block parent activity " + blockActivity);
            return;
        }
        OActivity oact = ((OSequence)blockActivity).sequence.get(0);
        if (oact instanceof OPickReceive) {
            OPickReceive.OnMessage rec = ((OPickReceive)oact).onMessages.get(0);
            rec.variable = resolveVariable(oscope, varName, rec.operation.getName(), true);
        } else __log.warn("Can't set block parameter on activity " + oact);
    }

    public void addExprVariable(OScope oscope, SimPELExpr expr, String varName) {
        if (expr == null) {
            // TODO Temporary plug until all activities are implemented
            __log.warn("Skipping expression building, null expr");
            return;
        }
        expr.addVariable(resolveVariable(oscope, varName));
    }

    public OProcess getProcess() {
        return _oprocess;
    }

    private OPartnerLink buildPartnerLink(OScope oscope, String name, String operation, boolean myRole) {
        // TODO Handle partnerlinks declared with an associated endpoint
        OPartnerLink resolved = partnerLinks.get(name);
        // TODO this will not work in case of variable name conflicts in different scopes
        if (resolved == null) {
            resolved = new OPartnerLink(_oprocess);
            resolved.name = name;
            resolved.declaringScope = oscope;
            partnerLinks.put(name, resolved);
            _oprocess.allPartnerLinks.add(resolved);
            oscope.partnerLinks.put(name, resolved);
        }
        if (myRole) {
            PortType pt = resolved.myRolePortType;
            if (pt == null) pt = resolved.myRolePortType = new SimPELPortType();
            SimPELOperation op = new SimPELOperation(operation);
            op.setInput(new SimPELInput(new QName(_processNS, operation + "Request")));
            pt.addOperation(op);
        } else {
            PortType pt = resolved.partnerRolePortType;
            if (pt == null) pt = resolved.partnerRolePortType = new SimPELPortType();
            SimPELOperation op = new SimPELOperation(operation);
            op.setOutput(new SimPELOutput(new QName(_processNS, operation + "Response")));
            pt.addOperation(op);
        }
        return resolved;
    }

    private OScope.Variable resolveVariable(OScope oscope, String name) {
        return resolveVariable(oscope, name, null, false);
    }

    private OScope.Variable resolveVariable(OScope oscope, String name, String operation, boolean request) {
        OScope.Variable resolved = variables.get(name);
        // TODO this will not work in case of variable name conflicts in different scopes
        if (resolved == null) {
            LinkedList<OMessageVarType.Part> parts = new LinkedList<OMessageVarType.Part>();
            parts.add(new OMessageVarType.Part(_oprocess, "payload",
                    new OElementVarType(_oprocess, new QName(_processNS, "simpelWrapper"))));
            OMessageVarType omsgType = new OMessageVarType(_oprocess, new QName(_processNS, "simpelMessage"), parts);
            resolved = new OScope.Variable(_oprocess, omsgType);
            resolved.name = name;
            resolved.declaringScope = oscope;
            variables.put(name, resolved);
        }

        // If an operation name has been provided with which to associate this variable, we
        // use a better naming for the part element.
        if (operation != null) {
            String elmtName = operation + (request ? "Request" : "Response");
            LinkedList<OMessageVarType.Part> parts = new LinkedList<OMessageVarType.Part>();
            parts.add(new OMessageVarType.Part(_oprocess, elmtName,
                    new OElementVarType(_oprocess, new QName(_processNS, elmtName))));
            resolved.type = new OMessageVarType(_oprocess, new QName(_processNS, operation), parts); 
        }
        return resolved;
    }

    private OExpression booleanExpr(boolean value) {
        OConstantExpression ce = new OConstantExpression(_oprocess, value ? Boolean.TRUE : Boolean.FALSE);
        ce.expressionLanguage = _konstExprLang;
        return ce;
    }


    protected String getBpwsNamespace() {
        return "http://ode.apache.org/simpel/1.0";
    }

}
