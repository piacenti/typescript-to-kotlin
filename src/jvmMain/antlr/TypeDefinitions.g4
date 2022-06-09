grammar TypeDefinitions;

IMPORT: 'import';
STAR:'*';
FROM:'from';
SEMI_COLON:';';
DECLARE:'declare';
TYPE:'type';
EQUALS:'=';
COLON:':';
OPEN_BRACE:'{';
CLOSE_BRACE:'}';
OPEN_PAREN:'(';
CLOSE_PAREN:')';
OPEN_BRACKET:'[';
CLOSE_BRACKET:']';
LESS_THAN:'<';
GREATER_THAN:'>';
COMMA:',';
PIPE:'|';
DOUBLE_QUOTED_STRING:'"'('\\"'|~'"')*'"';
SINGLE_QUOTED_STRING:'\''('\\\''|~'\'')*'\'';
EXTENDS: 'extends';
LAMBDA_OPERATOR: '=>';
AMPERSAND: '&';
EXPORT: 'export';
QUESTION: '?';
FUNCTION: 'function';
AS: 'as';
PERIOD: '.';
CONST: 'const';
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;
SPACES: [ \t\n\r]->channel(HIDDEN);
COMMENT: '/*'.*?'*/'->channel(HIDDEN);


main: importDeclaration* (typeDeclaration | functionDefinition | variableDefinition | reference)* export EOF;
importDeclaration: IMPORT importIdentifier FROM string semiColon?;
importIdentifier: STAR AS identifier | OPEN_BRACE identifier (COMMA identifier)* CLOSE_BRACE | identifier;
string: DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING;
typeDeclaration:  typeAlias | restrictedValues;
restrictedValues: DECLARE TYPE identifier EQUALS string (PIPE string)+ semiColon?;
classContext:OPEN_BRACE variableDefinition* CLOSE_BRACE;
typeAlias: DECLARE TYPE typeDefinition=namedType EQUALS (unionType| ((extensions+=type | classContext) (AMPERSAND (extensions+=type|classContext))*) ) semiColon?;
unionType: type (PIPE type)+;
type:namedType|lambdaType ;
namedType: typeName generic? (OPEN_BRACKET CLOSE_BRACKET)?;
typeName:identifier (PERIOD identifier)*;
generic:LESS_THAN genericParameter (COMMA genericParameter)* GREATER_THAN;
genericParameter:identifier EXTENDS type EQUALS type | identifier EXTENDS type| identifier EQUALS type | classContext| PIPE genericParameter | unionType | type;
functionDefinition: DECLARE FUNCTION functionName=identifier generic? OPEN_PAREN parameters* CLOSE_PAREN COLON (unionType|type) semiColon?;
reference: DECLARE CONST identifier COLON type semiColon?;
lambdaType: generic? OPEN_PAREN parameters* CLOSE_PAREN LAMBDA_OPERATOR type;
parameters: identifier COLON (unionType|type) (COMMA identifier COLON (unionType|type))* | destructuringParameters;
destructuringParameters: OPEN_BRACE identifier (COMMA identifier)* COMMA? CLOSE_BRACE COLON (type|classContext);
variableDefinition: identifier QUESTION? COLON (unionType|type) semiColon?;
export: EXPORT OPEN_BRACE exportName (COMMA exportName)* CLOSE_BRACE semiColon?;
exportName: identifier AS identifier | identifier;
identifier:IDENTIFIER;
semiColon:SEMI_COLON;
unsupported:    IMPORT
                |STAR
                |FROM
                |SEMI_COLON
                |DECLARE
                |TYPE
                |EQUALS
                |COLON
                |OPEN_BRACE
                |CLOSE_BRACE
                |OPEN_PAREN
                |CLOSE_PAREN
                |OPEN_BRACKET
                |CLOSE_BRACKET
                |LESS_THAN
                |GREATER_THAN
                |COMMA
                |PIPE
                |DOUBLE_QUOTED_STRING
                |SINGLE_QUOTED_STRING
                |EXTENDS
                |LAMBDA_OPERATOR
                |AMPERSAND
                |EXPORT
                |QUESTION
                |FUNCTION
                |AS
                |PERIOD
                |CONST
                |IDENTIFIER;



