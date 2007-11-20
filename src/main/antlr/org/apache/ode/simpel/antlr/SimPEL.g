grammar SimPEL;

options {
    output=AST; 
    language=Java;
    ASTLabelType=LinkedListTree;
}
tokens {
    PROCESS; PICK; SEQUENCE; FLOW; IF; ELSEIF; ELSE; WHILE; UNTIL; FOREACH; FORALL; INVOKE;
    RECEIVE; REPLY; ASSIGN; THROW; WAIT; EXIT; TIMEOUT; 
    EXPR; EXT_EXPR; XML_LITERAL; FUNCTION;
}
@parser::header {
package org.apache.ode.simpel.antlr;

import uk.co.badgersinfoil.e4x.antlr.LinkedListTokenStream;
import uk.co.badgersinfoil.e4x.antlr.LinkedListTree;
import uk.co.badgersinfoil.e4x.E4XHelper;
}
@lexer::header {
package org.apache.ode.simpel.antlr;
}

@parser::members {
    public static final int CHANNEL_PLACEHOLDER = 999;

    private SimPELLexer lexer;
    private CharStream cs;
    
    public void setInput(SimPELLexer lexer, CharStream cs) {
        this.lexer = lexer;
        this.cs = cs;
    }

    /** Handle 'island grammar' for embeded XML-literal elements. */
    private LinkedListTree parseXMLLiteral() throws RecognitionException {
        return E4XHelper.parseXMLLiteral(lexer, cs, (LinkedListTokenStream)input);
    }
}

// MAIN BPEL SYNTAX

program	:	declaration+;
declaration
	:	function | process;

// Process
process	:	'process' ID block -> ^(PROCESS ID block);

process_stmt
	:	(pick | sequence | flow | if_ex | while_ex | until_ex | foreach | forall
		| invoke | receive | reply | assign | throw_ex | wait_ex |  exit)+;
		
block	:	'{' process_stmt '}' -> ^(SEQUENCE process_stmt);

// Structured activities
pick	:	'pick' '{' receive* timeout* '}' -> ^(PICK receive* timeout*);
timeout	:	'timeout' '(' expr ')' block -> ^(TIMEOUT expr block); 

sequence:	'sequence' block -> ^(SEQUENCE block);

// TODO links
flow	:	'parrallel' '{' exprs+=process_stmt '}' ('and' '{' exprs+=process_stmt '}')* -> ^(FLOW $exprs);

if_ex	:	'if' '(' expr ')' block
		('else if' '(' expr ')' block)?
		('else' block)? -> ^(IF expr block ^(ELSEIF expr block)? ^(ELSE expr block)?);

while_ex:	'while' '(' expr ')' block -> ^(WHILE expr block);

until_ex:	'do' block 'until' '(' expr ')' -> ^(UNTIL expr block);

foreach	:	'for' '(' ID '=' init=expr ';' cond=expr ';' assign ')' block -> ^(FOREACH ID $init $cond assign block);
forall	:	'forall' '(' ID '=' from=expr '..' to=expr ')' block -> ^(FORALL ID $from $to);

// Simple activities
invoke	:	p=ID '.' o=ID '(' in=ID? ')' -> ^(INVOKE $p $o $in?);

receive	:	'receive' '(' p=ID ',' o=ID ')' ('(' m=ID ')')? block? -> ^(RECEIVE $p $o $m? block?);

reply	:	'reply' ID -> ^(REPLY ID);

assign	:	ID '=' rvalue -> ^(ASSIGN ID rvalue);
rvalue
	:	 receive | invoke | expr | xml_literal;
	
throw_ex:	'throw' ID -> ^(THROW ID);

wait_ex	:	'wait' expr -> ^(WAIT expr);

exit	:	'exit' -> ^(EXIT);

// TODO This will not work for any function whose code contains braces
function:	'function' f=ID '(' ID? (',' ID)* ')' '{' (options {greedy=false;} : .)* '}';

// Expressions
expr	:	s_expr | EXT_EXPR;

s_expr	:	condExpr;
condExpr:	aexpr ( ('==' ^|'!=' ^|'<' ^|'>' ^|'<=' ^|'>=' ^) aexpr )?;
aexpr	:	mexpr (('+'|'-') ^ mexpr)*;
mexpr	:	atom (('*'|'/') ^ atom)* ;
atom	:	ID | INT | '(' s_expr ')' -> s_expr;

// In-line XML

xml_literal
@init {
    LinkedListTree xml = null;
}
	:	// We have to have the LT in the outer grammar for lookahead
		// in AS3Parser to be able to predict that the xmlLiteral rule
		// should be used.
		'<' { xml=parseXMLLiteral(); } -> { xml };

EXT_EXPR
	:	'`' (options {greedy=false;} : .)* '`';

// Basic tokens
ID	:	(LETTER | '_' ) (LETTER | DIGIT | '_' )*;
INT	:	(DIGIT )+ ;
CR	:	('\r' | '\n' )+ { $channel = HIDDEN; };
WS	:	( ' ' | '\t' )+ { skip(); };
fragment NAMECHAR
    : LETTER | DIGIT | '.' | '-' | '_' | ':';
fragment DIGIT
    :    '0'..'9';
fragment LETTER
    : 'a'..'z' | 'A'..'Z';
	
