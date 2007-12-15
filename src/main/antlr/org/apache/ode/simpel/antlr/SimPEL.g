grammar SimPEL;

options {
    output=AST; 
    language=Java;
    ASTLabelType=LinkedListTree;
}
tokens {
    PROCESS; PICK; SEQUENCE; FLOW; IF; ELSEIF; ELSE; WHILE; UNTIL; FOREACH; FORALL; INVOKE;
    RECEIVE; REPLY; ASSIGN; THROW; WAIT; EXIT; TIMEOUT; TRY; CATCH; CATCH_ALL; SCOPE; EVENT;
    ALARM; COMPENSATION; COMPENSATE; CORRELATION; CORR_MAP; PARTNERLINK; VARIABLE; BLOCK_PARAM; 
    SIGNAL; JOIN;
    EXPR; EXT_EXPR; XML_LITERAL; CALL;
}
@parser::header {
package org.apache.ode.simpel.antlr;

import uk.co.badgersinfoil.e4x.antlr.LinkedListTokenStream;
import uk.co.badgersinfoil.e4x.antlr.LinkedListTree;
import uk.co.badgersinfoil.e4x.E4XHelper;
import org.apache.ode.simpel.ErrorListener;
import org.apache.ode.simpel.util.JSHelper;
}
@lexer::header {
package org.apache.ode.simpel.antlr;
import org.apache.ode.simpel.ErrorListener;
}

@lexer::members {
    private ErrorListener el;

    public void setErrorListener(ErrorListener el) {
    	this.el = el;
    }
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
    	el.reportRecognitionError(tokenNames, e.line, getErrorMessage(e, tokenNames), e);
    }
}

@parser::members {
    public static final int CHANNEL_PLACEHOLDER = 999;

    private SimPELLexer lexer;
    private CharStream cs;
    private ErrorListener el;
    
    public void setInput(SimPELLexer lexer, CharStream cs) {
        this.lexer = lexer;
        this.cs = cs;
    }
    public void setErrorListener(ErrorListener el) {
    	this.el = el;
    }

    /** Handle 'island grammar' for embeded XML-literal elements. */
    private LinkedListTree parseXMLLiteral() throws RecognitionException {
        return E4XHelper.parseXMLLiteral(lexer, cs, (LinkedListTokenStream)input);
    }
    /** Handle 'island grammar' for embeded JavaScript-literal elements. */
    private LinkedListTree parseJSLiteral() throws RecognitionException {
        return JSHelper.parseJSLiteral(lexer, cs, (LinkedListTokenStream)input);
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

// MAIN BPEL SYNTAX

program	:	declaration+;
declaration
	:	funct | process;

// Process
process	:	'process' ns_id block -> ^(PROCESS ns_id block);

process_stmt
	:	(pick | flow | if_ex | while_ex | until_ex | foreach | forall | try_ex | scope_ex
		| receive | ((invoke | reply | assign | throw_ex | wait_ex | exit | signal | join
		| variables) SEMI!) )+;

block	:	'{' process_stmt '}' -> ^(SEQUENCE process_stmt);
param_block
	:	'{' ('|' in+=ID (',' in+=ID)* '|')? process_stmt '}' -> ^($in process_stmt);
		
// Structured activities
pick	:	'pick' '{' receive* timeout* '}' -> ^(PICK receive* timeout*);
timeout	:	'timeout' '(' expr ')' block -> ^(TIMEOUT expr block); 

// TODO links
flow	:	'parrallel' '{' exprs+=process_stmt '}' ('and' '{' exprs+=process_stmt '}')* -> ^(FLOW $exprs);
signal	:	'signal' '('ID (',' expr)? ')' -> ^(SIGNAL ID expr?);
join	:	'join' '(' k+=ID (',' k+=ID)* (',' expr)? ')' -> ^(JOIN $k expr?);

if_ex	:	'if' '(' expr ')' block
		('else if' '(' expr ')' block)?
		('else' block)? -> ^(IF expr block ^(ELSEIF expr block)? ^(ELSE expr block)?);

while_ex:	'while' '(' expr ')' block -> ^(WHILE expr block);

until_ex:	'do' block 'until' '(' expr ')' -> ^(UNTIL expr block);

foreach	:	'for' '(' ID '=' init=expr ';' cond=expr ';' assign ')' block -> ^(FOREACH ID $init $cond assign block);
forall	:	'forall' '(' ID '=' from=expr 'to' to=expr ')' block -> ^(FORALL ID $from $to block);

try_ex	:	'try' tb=block catch_ex* ('catch' '(' ID ')' cb=block)? -> ^(TRY $tb catch_ex* ^(CATCH_ALL ID $cb)?);
		
catch_ex:	'catch' '(' ns_id ID ')' block -> ^(CATCH ns_id ID block);

scope_ex:	'scope' ('(' ID ')')? block scope_stmt* -> ^(SCOPE ID? block scope_stmt*);
scope_stmt
	:	event | alarm | compensation;

event	:	'event' '(' p=ID ',' o=ID ')' param_block -> ^(EVENT $p $o param_block);
alarm	:	'alarm' '(' expr ')' block -> ^(ALARM expr block);
compensation
	:	'compensation' block -> ^(COMPENSATION block);

// Simple activities
invoke	:	'invoke' '(' p=ID ',' o=ID (',' in=ID)? ')' -> ^(INVOKE $p $o $in?);

receive	:	receive_base (param_block | SEMI) -> ^(RECEIVE receive_base param_block?);
receive_base
	:	'receive' '(' p=ID ',' o=ID (',' correlation)? ')' -> ^($p $o correlation?);

reply	:	'reply' '(' ID ')' -> ^(REPLY ID);

assign	:	ID '=' rvalue -> ^(ASSIGN ID rvalue);
rvalue
	:	 receive_base -> ^(RECEIVE receive_base)
		| invoke | expr | xml_literal;
	
throw_ex:	'throw' '('ID')' -> ^(THROW ID);

wait_ex	:	'wait' '('expr')' -> ^(WAIT expr);

compensate
	:	'compensate' ('(' ID ')')? -> ^(COMPENSATE ID?);

exit	:	'exit' -> ^(EXIT);


// Others
variables
	:	'var'! v+=variable (','! v+=variable)*;
variable:	ID VAR_MODS* -> ^(VARIABLE ID VAR_MODS*);

partner_link
	:	'partnerLink' pl+=ID (',' pl+=ID)* -> ^(PARTNERLINK $pl);

correlation
	:	'{' corr_mapping (',' corr_mapping)* '}' -> ^(CORRELATION corr_mapping*);
corr_mapping
	:	f1=ID ':' expr -> ^(CORR_MAP $f1 expr);

funct	:	'function'^ f=ID '(' ID? (','! ID)* ')' js_block;

// Expressions
expr	:	s_expr | EXT_EXPR | funct_call;

funct_call
	:	p+=ID '(' p+=ID* ')' -> ^(CALL ID+);
s_expr	:	condExpr;
condExpr:	aexpr ( ('==' ^|'!=' ^|'<' ^|'>' ^|'<=' ^|'>=' ^) aexpr )?;
aexpr	:	mexpr (('+'|'-') ^ mexpr)*;
mexpr	:	atom (('*'|'/') ^ atom)* | STRING;
atom	:	ID | INT | '(' s_expr ')' -> s_expr;

ns_id	:	(ID '::')? ID;

// In-line XML

xml_literal
@init { LinkedListTree xml = null; }
	:	// We have to have the LT in the outer grammar for lookahead
		// in AS3Parser to be able to predict that the xmlLiteral rule
		// should be used.
		'<' { xml=parseXMLLiteral(); } -> { xml };

js_block
@init { LinkedListTree js = null; }
	:	'{' { js=parseJSLiteral(); } -> { js };

EXT_EXPR
	:	'[' (options {greedy=false;} : .)* ']';

// Basic tokens
VAR_MODS:	'unique' | 'external' | ('string' | 'int' | 'float');
SEMI	:	';';
ID	:	(LETTER | '_' ) (LETTER | DIGIT | '_' )*;
INT	:	(DIGIT )+ ;
STRING	:	'"' ( ESCAPE_SEQ | ~('\\'|'"') )* '"';
ESCAPE_SEQ
	:	'\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\');

SL_COMMENTS
	:	('#'|'//') .* CR { $channel = HIDDEN; };
CR	:	('\r' | '\n' )+ { $channel = HIDDEN; };
WS	:	( ' ' | '\t' )+ { skip(); };
fragment DIGIT
    :    '0'..'9';
fragment LETTER
    : 'a'..'z' | 'A'..'Z';
