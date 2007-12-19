tree grammar SimPELWalker;

options {
    tokenVocab=SimPEL;
    ASTLabelType=LinkedListTree;
}
tokens {
    XML_ELEMENT; XML_ATTRIBUTE; XML_NAME; XML_ATTRIBUTE_VALUE; XML_TEXT; XML_WS; XML_COMMENT; XML_CDATA; XML_PI;
}
@header {
package org.apache.ode.simpel.antlr;
import uk.co.badgersinfoil.e4x.antlr.LinkedListTree;
import org.apache.ode.simpel.ErrorListener;
}

@members {
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

}

program	:	declaration+;
declaration
	:	process | namespace;

namespace
	:	^(NAMESPACE ID STRING);

// Process
process	:	^(PROCESS ^(NS pr=ID? nm=ID) body) { System.out.println("PROCESS " + $nm.text); };

proc_stmt
	:	pick | flow | if_ex | while_ex | until_ex | foreach | forall | try_ex | scope_ex
		| invoke | receive | reply | assign | throw_ex | wait_ex | exit | signal | join
		| variable | partner_link;
block	:	^(SEQUENCE proc_stmt+);
param_block
	:	^(SEQUENCE ID+ proc_stmt+);
body	:	block | proc_stmt;
		

// Structured activities
pick	:	^(PICK receive* timeout*);
timeout	:	^(TIMEOUT expr block); 

// TODO links
flow	:	^(FLOW body*);
signal	:	^(SIGNAL ID expr?);
join	:	^(JOIN ID* expr?);


//if_ex	:	^(IF expr block (^(ELSEIF expr block))* (^(ELSE expr block))?);
if_ex	:	^(IF expr body (^(ELSE body))?);

while_ex:	^(WHILE expr body);

until_ex:	^(UNTIL expr body);

foreach	:	^(FOREACH ID init=expr cond=expr assign body);
forall	:	^(FORALL ID from=expr to=expr body);

try_ex	:	^(TRY body catch_ex*);
catch_ex:	^(CATCH ^(NS ID ID?) param_block);

scope_ex:	^(SCOPE ID? body scope_stmt*);
scope_stmt
	:	event | alarm | compensation;

event	:	^(EVENT ID ID param_block);
alarm	:	^(ALARM expr body);
compensation
	:	^(COMPENSATION body);

// Simple activities
invoke	:	^(INVOKE p=ID o=ID in=ID?);

receive	:	^(RECEIVE ^(ID ID correlation?) param_block?);
	
reply	:	^(REPLY ID (ID ID)?);

assign	:	^(ASSIGN ID rvalue);
rvalue
	:	receive | invoke | expr | xmlElement;
	
throw_ex:	^(THROW ID);

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
	:	^(XML_ELEMENT XML_NAME xmlAttribute* xmlElementContent*) { System.out.println("ELMT " + $XML_NAME.text); };
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
