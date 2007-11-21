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
}

program	:	declaration+;
declaration
	:	process;

// Process
process	:	^(PROCESS ID block) { System.out.println("PROCESS " + $ID.text); };

process_stmt
	:	(pick | sequence | flow | if_ex | while_ex | until_ex | foreach | forall
		| invoke | receive | reply | assign | throw_ex | wait_ex |  exit)+;
		
block	:	^(SEQUENCE process_stmt);

// Structured activities
pick	:	^(PICK receive* timeout*);
timeout	:	^(TIMEOUT expr block); 

sequence:	^(SEQUENCE block);

// TODO links
flow	:	^(FLOW process_stmt);

if_ex	:	^(IF expr block ^(ELSEIF expr block) ^(ELSE expr block));

while_ex:	^(WHILE expr block);

until_ex:	^(UNTIL expr block);

foreach	:	^(FOREACH ID init=expr cond=expr assign block);
forall	:	^(FORALL ID from=expr to=expr);

// Simple activities
invoke	:	^(INVOKE p=ID o=ID in=ID?);

receive	:	^(RECEIVE p=ID o=ID m=ID? block?);

reply	:	^(REPLY ID);

assign	:	^(ASSIGN ID rvalue);
rvalue
	:	receive | invoke | expr | xmlElement;
	
throw_ex:	^(THROW ID);

wait_ex	:	^(WAIT expr);

exit	:	EXIT;

function:	^(FUNCTION ID);

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
expr	:	s_expr | EXT_EXPR;

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
	|	ID | INT;
