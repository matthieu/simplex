tree grammar SimPELWalker;

options {
    tokenVocab=SimPEL;
    ASTLabelType=LinkedListTree;
}
tokens {
    XML_ELEMENT; XML_ATTRIBUTE; XML_NAME; XML_ATTRIBUTE_VALUE; XML_TEXT; XML_WS; XML_COMMENT; XML_CDATA; XML_PI;
}
scope BPELScope { OScope oscope; }
scope Parent { OBuilder.StructuredActivity activity; }
scope ReceiveBlock { OActivity activity; }
scope ExprContext { SimPELExpr expr; }

@header {
package org.apache.ode.simpel.antlr;
import uk.co.badgersinfoil.e4x.antlr.LinkedListTree;
import uk.co.badgersinfoil.e4x.antlr.LinkedListToken;
import org.apache.ode.simpel.ErrorListener;
import org.apache.ode.simpel.omodel.OBuilder;
import org.apache.ode.simpel.omodel.SimPELExpr;
import org.apache.ode.bpel.rtrep.v2.*;
}

@members {
    // Grammar level members
    
    private ErrorListener el;
    
    public void setErrorListener(ErrorListener el) {
    	this.el = el;
    }
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
    	el.reportRecognitionError(tokenNames, e.line, getErrorMessage(e, tokenNames), e);
    }
    
    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
	List stack = getRuleInvocationStack(e, this.getClass().getName());
    	String msg = null;
    	if ( e instanceof NoViableAltException ) {
       	    NoViableAltException nvae = (NoViableAltException)e;
       	    msg = " no viable alt; token="+e.token+" (decision="+nvae.decisionNumber+" state "+nvae.stateNumber+")"+
                  " decision=<<"+nvae.grammarDecisionDescription+">>";
        } else {
           msg = super.getErrorMessage(e, tokenNames);
        }
        return stack+" "+msg;
    }
    
    public String getTokenErrorDisplay(Token t) {
        return t.toString();
    }
    
    // Lamguage level members
    
    private OBuilder builder = new OBuilder();

    public OBuilder getBuilder() {
	return builder;
    }
    
    private String text(org.antlr.runtime.tree.Tree t) {
    	if (t == null) return null;
    	else return t.getText();
    }

    private String deepText(org.antlr.runtime.tree.Tree t) {
    	LinkedListTree llt = ((LinkedListTree)t);
    	StringBuffer b = new StringBuffer();
    	LinkedListToken tok = ((LinkedListTree)t).getStartToken();
    	b.append(tok.getText());
    	while(tok != llt.getStopToken() && (tok = tok.getNext()) != null)
	    if (tok.getText() != null) b.append(tok.getText());
        return b.toString();
    }

}

program	:	^(ROOT declaration+);
declaration
	:	process | namespace;

namespace
	:	^(NAMESPACE ID STRING);

// Process
process
scope BPELScope Parent;
	:	^(PROCESS ^(NS pr=ID? nm=ID) 
		{ OBuilder.StructuredActivity<OScope> scope = builder.buildProcess(text($pr), text($nm));
		  $BPELScope::oscope = scope.getOActivity(); 
		  $Parent::activity = scope;
		} 
		body);

proc_stmt
	:	pick | flow | if_ex | while_ex | until_ex | foreach | forall | try_ex | scope_ex | with_ex
		| invoke | receive | reply | assign | throw_ex | wait_ex | exit | signal | join
		| variable | partner_link;
block
scope Parent;
	:	^(SEQUENCE 
		{ OBuilder.StructuredActivity seq = builder.build(OSequence.class, $BPELScope::oscope, $Parent[-1]::activity); 
		  $Parent::activity = seq;
		}
		proc_stmt+);
param_block
scope Parent;
	:	^(SEQUENCE ID+
		{ OBuilder.StructuredActivity seq = builder.build(OSequence.class, $BPELScope::oscope, $Parent[-1]::activity); 
		  $Parent::activity = seq;
		  builder.setBlockParam($BPELScope::oscope, (OSequence)seq.getOActivity(), $ID.text); 
		}
		proc_stmt+);
body	:	block | proc_stmt;
		

// Structured activities
pick	
	:	^(PICK receive* timeout*);
timeout	:	^(TIMEOUT expr block); 

// TODO links
flow	
	:	^(FLOW body*);
signal	:	^(SIGNAL ID expr?);
join	:	^(JOIN ID* expr?);

if_ex	
scope ExprContext Parent;
	:	^(IF {
        $ExprContext::expr = new SimPELExpr(builder.getProcess());
    }
    e=(expr) {
        $ExprContext::expr.setExpr(deepText($e));
        OBuilder.StructuredActivity<OSwitch> oswitch = builder.build(OSwitch.class, $BPELScope::oscope, $Parent[-1]::activity, $ExprContext::expr);
        $Parent::activity = oswitch;
    } b1=(body)
    (^(ELSE b2=(body)))?);

while_ex
scope ExprContext;
	:	^(WHILE {
        $ExprContext::expr = new SimPELExpr(builder.getProcess());
    }
    expr body);

until_ex
scope ExprContext;
	:	^(UNTIL expr body);

foreach	
	:	^(FOREACH ID init=expr cond=expr assign body);
forall	
	:	^(FORALL ID from=expr to=expr body);

try_ex
scope BPELScope;
	:	^(TRY {
            OBuilder.StructuredActivity<OScope> oscope = builder.build(OScope.class, null, $Parent::activity);
            $BPELScope::oscope = oscope.getOActivity();
        }
        body catch_ex*);
catch_ex:	^(CATCH ^(NS ID ID?) param_block);

scope_ex
scope BPELScope;
	:	^(SCOPE {
            OBuilder.StructuredActivity<OScope> oscope = builder.build(OScope.class, null, $Parent::activity);
            $BPELScope::oscope = oscope.getOActivity();
        }
	    ID? body scope_stmt*);
scope_stmt
	:	event | alarm | compensation;

event	:	^(EVENT ID ID param_block);
alarm	:	^(ALARM expr body);
compensation
	:	^(COMPENSATION body);

with_ex 
scope ExprContext;
	: ^(WITH {
        $ExprContext::expr = new SimPELExpr(builder.getProcess());
    }
    with_map* body);
with_map:       ^(MAP ID path_expr);

// Simple activities

invoke
scope ReceiveBlock;
        :	^(INVOKE ^(p=ID o=ID in=ID?)) {
                OBuilder.StructuredActivity<OInvoke> inv = builder.build(OInvoke.class, $BPELScope::oscope,
                    $Parent::activity, text($p), text($o), text($in));
		        $ReceiveBlock::activity = inv.getOActivity();
		    }
            (prb=(param_block))?;

reply	
  :	^(REPLY msg=ID (pl=ID var=ID)?) {
      if (ReceiveBlock_stack.size() > 0)
        builder.build(OReply.class, $BPELScope::oscope, $Parent::activity,
			      $ReceiveBlock::activity, text($msg), text($pl), text($var));
      else
        builder.build(OReply.class, $BPELScope::oscope, $Parent::activity,
			      null, text($msg), text($pl), text($var));
    };
receive	
scope ReceiveBlock;
	:	^(RECEIVE ^(p=ID o=ID correlation?)) {
            OBuilder.StructuredActivity<OPickReceive> rec = builder.build(OPickReceive.class, $BPELScope::oscope,
                $Parent::activity, text($p), text($o));
		    $ReceiveBlock::activity = rec.getOActivity();
		}
		(prb=(param_block))?;

assign	
scope ExprContext;
	:	^(ASSIGN {
        $ExprContext::expr = new SimPELExpr(builder.getProcess());
    }
    lv=(path_expr) rv=(rvalue)) {
        $ExprContext::expr.setExpr(deepText($rv));
		OBuilder.StructuredActivity<OAssign> assign =
            builder.build(OAssign.class, $BPELScope::oscope, $Parent::activity, deepText($lv), $ExprContext::expr);
        // The long, winding road of abstraction
        $ExprContext::expr = (SimPELExpr) ((OAssign.Expression)((OAssign.Copy)assign.
            getOActivity().operations.get(0)).from).expression;
    };
rvalue	:	receive | invoke | expr | xmlElement;
	
throw_ex:	^(THROW ns_id);

wait_ex	:	^(WAIT expr);

exit	:	EXIT;

// Other
variable:	^(VARIABLE ID VAR_MODS*);

partner_link
	:	^(PARTNERLINK ID*);

correlation
scope ExprContext;
	:	^(CORRELATION { 
        $ExprContext::expr = new SimPELExpr(builder.getProcess());
    }
    corr_mapping*);
corr_mapping
	:	^(CORR_MAP ID expr);

// XML
xmlElement
	:	^(XML_EMPTY_ELEMENT XML_NAME xmlAttribute*) | ^(XML_ELEMENT XML_NAME xmlAttribute* xmlElementContent*) 
                { System.out.println("ELMT " + $XML_NAME.text); };
xmlAttribute
	:	^(XML_ATTRIBUTE XML_NAME XML_ATTRIBUTE_VALUE) { System.out.println("ATTR " + $XML_NAME.text); };
xmlElementContent
	:	xmlMarkup | xmlText | xmlElement;
xmlText :	XML_TEXT | XML_NAME | XML_WS;
xmlMarkup
	:	XML_COMMENT | XML_CDATA | XML_PI;

// Expressions
expr	:	s_expr | EXT_EXPR;

funct_call
	:	^(CALL ID expr*);
path_expr
	:	^(PATH ids=(ns_id*)) { 
        builder.addExprVariable($BPELScope::oscope, $ExprContext::expr, deepText($ids));
    };
ns_id	:	^(NS ID? ID);

s_expr	:	^('==' s_expr s_expr) 
	|	^('!=' s_expr s_expr) 
	|	^('<' s_expr s_expr) 
	|	^('>' s_expr s_expr) 
	|	^('>=' s_expr s_expr) 
	|	^('<=' s_expr s_expr) 
	|	^('+' s_expr s_expr) 
	|	^('-' s_expr s_expr) 
	|	^('*' s_expr s_expr) 
	|	^('/' s_expr s_expr) 
	|	path_expr | INT | STRING | funct_call;
