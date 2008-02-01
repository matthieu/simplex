tree grammar SimPELWalker;

options {
    tokenVocab=SimPEL;
    ASTLabelType=LinkedListTree;
}
tokens {
    XML_ELEMENT; XML_ATTRIBUTE; XML_NAME; XML_ATTRIBUTE_VALUE; XML_TEXT; XML_WS; XML_COMMENT; XML_CDATA; XML_PI;
}
scope BPELScope { OScope oscope; }
scope Block { OActivity blockActivity; }
scope Parent { OActivity activity; }

@header {
package org.apache.ode.simpel.antlr;
import uk.co.badgersinfoil.e4x.antlr.LinkedListTree;
import org.apache.ode.simpel.ErrorListener;
import org.apache.ode.simpel.omodel.OBuilder;
import org.apache.ode.bpel.o.*;
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

}

program	:	^(ROOT declaration+);
declaration
	:	process | namespace;

namespace
	:	^(NAMESPACE ID STRING);

// Process
process
scope BPELScope Parent;
	:	^(PROCESS ^(NS pr=ID? nm=ID) body) 
		{ OScope scope = builder.buildProcess(text($pr), text($nm));
		  $BPELScope::oscope = scope; 
		  $Parent::activity = scope;
		};

proc_stmt
	:	pick | flow | if_ex | while_ex | until_ex | foreach | forall | try_ex | scope_ex | with_ex
		| invoke | receive | reply | assign | throw_ex | wait_ex | exit | signal | join
		| variable | partner_link;
block
scope Parent;
	:	^(SEQUENCE proc_stmt+) 
		{ OSequence seq = builder.buildSequence($Parent[-1]::activity); 
		  $Parent::activity = seq;
		};
param_block
scope Parent;
	:	^(SEQUENCE ID+ proc_stmt+) 
		{ OSequence seq = builder.buildSequence($Parent[-1]::activity); 
		  $Parent::activity = seq;
		  builder.setBlockParam($Block::blockActivity, $ID.text); 
		};
body	:	block | proc_stmt;
		

// Structured activities
pick	
scope Parent;
	:	^(PICK receive* timeout*);
timeout	:	^(TIMEOUT expr block); 

// TODO links
flow	
scope Parent;
	:	^(FLOW body*);
signal	:	^(SIGNAL ID expr?);
join	:	^(JOIN ID* expr?);

if_ex	
scope Parent;
	:	^(IF expr body (^(ELSE body))?);

while_ex
scope Parent;
	:	^(WHILE expr body);

until_ex
scope Parent;
	:	^(UNTIL expr body);

foreach	
scope Parent;
	:	^(FOREACH ID init=expr cond=expr assign body);
forall	
scope Parent;
	:	^(FORALL ID from=expr to=expr body);

try_ex
scope BPELScope Parent;
	:	^(TRY body catch_ex*);
catch_ex:	^(CATCH ^(NS ID ID?) param_block);

scope_ex
scope BPELScope Parent;
	:	^(SCOPE ID? body scope_stmt*);
scope_stmt
	:	event | alarm | compensation;

event	:	^(EVENT ID ID param_block);
alarm	:	^(ALARM expr body);
compensation
	:	^(COMPENSATION body);

with_ex 
scope Parent;
	:       ^(WITH with_map* body);
with_map:       ^(MAP ID path_expr);

// Simple activities
invoke	:	^(INVOKE p=ID o=ID in=ID?);


reply	:	^(REPLY ID (ID ID)?);
receive	
scope Block;
scope Parent;
	:	^(RECEIVE ^(p=ID o=ID correlation?) (prb=(param_block))?) 
		{ OPickReceive rec = builder.buildReceive($Parent[-1]::activity, text($p), text($o)); 
		  if ($prb != null) {
		      $Parent::activity = rec;
		      $Block::blockActivity = rec;
		   }	  
		};
	

assign	:	^(ASSIGN ID rvalue);
rvalue
	:	receive | invoke | expr | xmlElement;
	
throw_ex:	^(THROW ns_id);

wait_ex	:	^(WAIT expr);

exit	:	EXIT;

// Other
variable:	^(VARIABLE ID VAR_MODS*);

partner_link
	:	^(PARTNERLINK ID*);
correlation
	:	^(CORRELATION corr_mapping*);
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
expr	:	s_expr | EXT_EXPR | funct_call;

funct_call
	:	^(CALL ID*);
path_expr
	:	^(PATH ns_id*);
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
	|	path_expr | INT | STRING;
