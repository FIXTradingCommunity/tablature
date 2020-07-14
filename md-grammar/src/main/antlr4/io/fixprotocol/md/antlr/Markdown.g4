/* Grammar for a subset of markdown */
grammar Markdown;

document
:
	block+ EOF
;

block
:
	heading
	| paragraph
	| list
	| blockquote
	| table
	| NEWLINE
;

heading
:
	NEWLINE* HEADINGLINE NEWLINE
;

paragraph
:
	NEWLINE* paragraphline+
;

paragraphline
:
	PARAGRAPHLINE
	(
		NEWLINE
		| EOF
	)
;

list
:
	NEWLINE* listline+
;

listline
:
	LISTLINE
	(
		NEWLINE
		| EOF
	)
;

blockquote
:
	NEWLINE* quoteline+
;

quoteline
:
	QUOTELINE
	(
		NEWLINE
		| EOF
	)
;

table
:
	NEWLINE* tableheading tabledelimiterrow tablerow+
;

tableheading
:
	tablerow
;

tablerow
:
	cell+ PIPE?
	(
		NEWLINE
		| EOF
	)
;

cell
:
	CELLTEXT
;

tabledelimiterrow
:
	TABLEDELIMINATORCELL+ PIPE? NEWLINE
;

LITERAL
:
	BACKTICK ' '? LITERALCHAR+ ' '? BACKTICK
;

HEADINGLINE
:
	HASH+ ' '? LINECHAR+
;

QUOTELINE
:
	GT ' '? ( LINECHAR+ | LITERAL)+
;

LISTLINE
:
	WS*
	(
		BULLET
		| LISTNUMBER
	) WS+ LINECHAR*
;

PARAGRAPHLINE
:
	INITIALPARACHAR (LINECHAR+ | LITERAL)*
;

TABLEDELIMINATORCELL
:
	PIPE? ':'? '-'+ ':'?
;

IGNORE_WS
:
	WS -> skip
;

NEWLINE
:
	'\r'? '\n'
;

/* low priority since it can match empty string */
CELLTEXT
:
	PIPE IGNORE_WS (CELLCHAR+ | LITERAL)*
;

/* Unescaped backtick character; not preceded by backslash */
BACKTICK
:
	{_input.LA(-1) != 92}?

	'`'
;

/* Unescaped greater-than character; not preceded by backslash */
GT
:
	{_input.LA(-1) != 92}?

	'>'
;

/* Unescaped hash character; not preceded by backslash */
HASH
:
	{_input.LA(-1) != 92}?

	'#'
;

/* Unescaped pipe character; not preceded by backslash */
PIPE
:
	{_input.LA(-1) != 92}?

	'|'
;

/* disallow unescaped pipe, newline, literal within a table cell */
fragment
CELLCHAR
:
	(
		ESCAPEDCHAR
		| ESCAPEDPIPE
		| WS
		| ALPHANUMERIC
		| PUNCTUATION
	)
;

/* Escaped punctuation, includes escaped backslash */
fragment
ESCAPEDCHAR
:
	'\\' [\\!"#$%&'()*+,\-./:;<=>?@[\]^_{|}~`]
;

fragment
ALPHANUMERIC
:
	[a-zA-Z0-9\u0080-\uFFFF]
;

fragment
PUNCTUATION
:
	[!"#$%&'()*+,\-./:;<=>?@[\]^_{}]
;

fragment
ESCAPEDPIPE
:
	{_input.LA(-1) == 92}?

	'|'
;

fragment
WS
:
	[  \t]
;

fragment
LISTNUMBER
:
	[1-9] [.)]
;

fragment
BULLET
:
	[-+*]
;

fragment
INITIALPARACHAR
:
	~[#>|\n\r]
;

fragment
LITERALCHAR
:
	~[`]
;

fragment
LINECHAR
:
	~[\n\r`]
;


