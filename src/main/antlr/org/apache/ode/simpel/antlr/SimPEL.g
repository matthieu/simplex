grammar SimPEL;

options {
    output=AST; 
    language=Java;
    ASTLabelType=LinkedListTree;
}
tokens {
    ROOT; PROCESS; PICK; SEQUENCE; FLOW; IF; ELSEIF; ELSE; WHILE; UNTIL; FOREACH; FORALL; INVOKE;
    RECEIVE; REPLY; ASSIGN; THROW; WAIT; EXIT; TIMEOUT; TRY; CATCH; CATCH_ALL; SCOPE; EVENT;
    ALARM; COMPENSATION; COMPENSATE; CORRELATION; CORR_MAP; PARTNERLINK; VARIABLE; BLOCK_PARAM; 
    SIGNAL; JOIN; WITH; MAP;
    EXPR; EXT_EXPR; XML_LITERAL; CALL; NAMESPACE; NS; PATH;
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

program	:	declaration+ -> ^(ROOT declaration+);
declaration
	:	funct | process | namespace;

// Process
process	:	'process' ns_id body -> ^(PROCESS ns_id body);

proc_stmt
	:	pick | flow | if_ex | while_ex | until_ex | foreach | forall | try_ex | scope_ex | with_ex
		| receive | invoke | ((reply | assign | throw_ex | wait_ex | exit | signal | join
		| variables | partner_link) SEMI!);

block	:	'{' proc_stmt+ '}' -> ^(SEQUENCE proc_stmt+);
param_block
	:	'{' ('|' in+=ID (',' in+=ID)* '|')? proc_stmt+ '}' -> ^(SEQUENCE $in proc_stmt+);
body	:	block | proc_stmt;

// Structured activities
pick	:	'pick' '{' receive* timeout* '}' -> ^(PICK receive* timeout*);
timeout	:	'timeout' '(' expr ')' block -> ^(TIMEOUT expr block); 

flow	:	'parallel' b+=body ('and' b+=body)* -> ^(FLOW $b);
signal	:	'signal' '('ID (',' expr)? ')' -> ^(SIGNAL ID expr?);
join	:	'join' '(' k+=ID (',' k+=ID)* (',' expr)? ')' -> ^(JOIN $k expr?);

if_ex	:	'if' '(' expr ')' ifb=body ('else' eb=body)? -> ^(IF expr $ifb (^(ELSE $eb))?);

while_ex:	'while' '(' expr ')' body -> ^(WHILE expr body);

until_ex:	'do' body 'until' '(' expr ')' -> ^(UNTIL expr body);

foreach	:	'for' '(' ID '=' init=expr ';' cond=expr ';' assign ')' body -> ^(FOREACH ID $init $cond assign body);
forall	:	'forall' '(' ID '=' from=expr 'to' to=expr ')' body -> ^(FORALL ID $from $to body);

try_ex	:	'try' body catch_ex* -> ^(TRY body catch_ex*);		
catch_ex:	'catch' '(' ns_id ')' param_block -> ^(CATCH ns_id param_block);

scope_ex:	'scope' ('(' ID ')')? body scope_stmt* -> ^(SCOPE ID? body scope_stmt*);
scope_stmt
	:	event | alarm | compensation;

event	:	'event' '(' p=ID ',' o=ID ')' param_block -> ^(EVENT $p $o param_block);
alarm	:	'alarm' '(' expr ')' body -> ^(ALARM expr body);
compensation
	:	'compensation' body -> ^(COMPENSATION body);

with_ex :
                'with' '(' wm+=with_map (',' wm+=with_map)* ')' body -> ^(WITH $wm* body);
with_map:       ID ':' path_expr -> ^(MAP ID path_expr);

// Simple activities

invoke
options {backtrack=true;}
        :	invoke_base SEMI -> ^(INVOKE invoke_base)
            | invoke_base param_block -> ^(INVOKE invoke_base) param_block;
invoke_base
        :	'invoke' '(' p=ID ',' o=ID (',' in=ID)? ')' -> ^($p $o $in?);

receive	
options {backtrack=true;}
        :	receive_base SEMI -> ^(RECEIVE receive_base) |
            receive_base param_block -> ^(RECEIVE receive_base) param_block;
receive_base
	    :	'receive' '(' p=ID ',' o=ID (',' correlation)? ')' -> ^($p $o correlation?);

reply	:	'reply' '(' ID (',' ID ',' ID)? ')' -> ^(REPLY ID (ID ID)?);

assign	:	path_expr '=' rvalue -> ^(ASSIGN path_expr rvalue);
rvalue
	    :	receive_base -> ^(RECEIVE receive_base)
		    | invoke | expr | xml_literal;
	
throw_ex:	'throw' '('? ns_id ')'? -> ^(THROW ns_id);

wait_ex	:	'wait' '('expr')' -> ^(WAIT expr);

compensate
	:	'compensate' ('(' ID ')')? -> ^(COMPENSATE ID?);

exit	:	'exit' -> ^(EXIT);


// Others
namespace
	:	'namespace' ID '=' STRING SEMI -> ^(NAMESPACE ID STRING);
		
variables
	:	'var'! v+=variable (','! v+=variable)*;
variable:	ID VAR_MODS* -> ^(VARIABLE ID VAR_MODS*);

partner_link
	:	'partnerLink' pl+=ID (',' pl+=ID)* -> ^(PARTNERLINK $pl);

correlation
	:	'{' corr_mapping (',' corr_mapping)* '}' -> ^(CORRELATION corr_mapping*);
corr_mapping
	:	fn=ID ':' var=ID -> ^(CORR_MAP $fn $var);

funct	:	'function'^ f=ID '(' ID? (','! ID)* ')' js_block;

// Expressions
expr	:	s_expr | EXT_EXPR;

funct_call
	    :	fn=ID '(' (e+=expr)? (',' e+=expr)* ')' -> ^(CALL ID $e*);
// TODO add && || !
s_expr	:	condExpr;
condExpr:	aexpr ( ('==' ^|'!=' ^|'<' ^|'>' ^|'<=' ^|'>=' ^) aexpr )?;
aexpr	:	mexpr (('+'|'-') ^ mexpr)*;
mexpr	:	atom (('*'|'/') ^ atom)* | STRING;
atom	:	path_expr | INT | '(' s_expr ')' -> s_expr | funct_call;
path_expr
	:	pelmt+=ns_id ('.' pelmt+=ns_id)* -> ^(PATH $pelmt);

ns_id	:	(pr=ID '::')? loc=ID ('()')? -> ^(NS $pr? $loc);

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
ID	:	(LETTER | '_' ) (LETTER | DIGIT | '_' | '-' )*;
INT	:	(DIGIT )+ ;
STRING	:	'"' ( ESCAPE_SEQ | ~('\\'|'"') )* '"';
ESCAPE_SEQ
	:	'\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\');

SL_COMMENTS
	:	'//' .* CR { $channel = HIDDEN; };
CR	:	('\r' | '\n' )+ { $channel = HIDDEN; };
WS	:	( ' ' | '\t' )+ { skip(); };
fragment DIGIT
    :    '0'..'9';
fragment LETTER
    : 'a'..'z' | 'A'..'Z';
