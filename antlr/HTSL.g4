grammar HTSL;

program
    : (NEWLINE | statement)* EOF
    ;

statement
    : ifStatement
    | randomStatement
    | actionStatement
    | defineStatment
    | loopStatement
    | jsCodeStatement
    | gotoStatement
    ;

ifStatement
    : IF logicOperator? '(' (conditionStatement (',' conditionStatement)*)? ')' block (ELSE block)?
    ;

logicOperator
    : AND | OR | BOOLEAN
    ;

block
    : '{' NEWLINE* statement (NEWLINE+ statement)* NEWLINE* '}'
    | '{' NEWLINE* '}'
    ;

jsCodeStatement
    : '{' jsToken* '}'
    ;

jsToken
    : STRING
    | NUMBER
    | BOOLEAN
    | IDENTIFIER
    | COMPARATOR
    | OPERATOR
    | PLACEHOLDER
    | '(' | ')' | ',' | '.' | ';' | ':' | '+' | '-' | '*' | '/' | '!' | '&' | '|' | '[' | ']' | '?' | '%' | '^' | '~' | '++' | '--' | '<<' | '>>' | '>>>'
    ;

randomStatement
    : RANDOM block
    ;

defineStatment
    : DEFINE IDENTIFIER argument*
    ;

loopStatement
    : LOOP NUMBER IDENTIFIER block
    ;

actionStatement
    : IDENTIFIER argument*
    ;

conditionStatement
    : ('!')? IDENTIFIER argument*
    ;

gotoStatement
    : 'goto' IDENTIFIER argument*
    ;

argument: STRING | NUMBER | BOOLEAN | IDENTIFIER | COMPARATOR | OPERATOR | AND | OR | PLACEHOLDER | NULL | jsCodeStatement ;

arguments: argument*;

IF: 'if';
RANDOM: 'random';
DEFINE: 'define';
LOOP: 'loop';
ELSE: 'else';
AND: 'and';
OR: 'or';
NULL: 'null';
BOOLEAN: 'true' | 'false';
COMPARATOR: '==' | '!=' | '<' | '<=' | '>' | '>=' | 'equals' | 'notEquals' | 'lessThan' | 'lessThanOrEqual' | 'greaterThan' | 'greaterThanOrEqual';
OPERATOR: '+=' | '-=' | '=' | '*=' | '/=' | '&=' | '|=' | '^=' | '<<=' | '>>=' | '>>>=' | 'unset' | 'set' | 'increment' | 'decrement' | 'inc' | 'dec' | 'multiply' | 'divide' | 'xor' | 'shiftLeft' | 'shiftRight' | 'shiftRightUnsigned';
STRING: '"' ~["\r\n]* '"';
PLACEHOLDER: '%' ~[%\r\n]* '%';
NUMBER: '-'? [0-9]+(',' [0-9]+)* ('.' [0-9]+)? ('L' | 'D')? { text = text.replace(",", "") }; //EW, but it works ;)
IDENTIFIER: ID_START ID_PART*;
fragment ID_START: ~[ \t\r\n(){},"!%/0-9=<>+\-*|^];
fragment ID_PART:  ~[ \t\r\n(){},"];COMMENT: '//' ~[\r\n]* -> skip;
MULTI_LINE_COMMENT: '/*' .*? '*/' -> skip;
NEWLINE: '\r'? '\n';
WS: [ \t]+ -> skip;

