/* Grammar for a subset of markdown */
grammar Markdown;

document
:
	block+ EOF
;

block
:
	heading | paragraph | list | table
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
	PARAGRAPHLINE (NEWLINE | EOF)
;

list
:
	NEWLINE* listline+
;

listline
:
	LISTLINE (NEWLINE | EOF)
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
	cell+ PIPE? (NEWLINE | EOF)
;

cell
:
	CELLTEXT
;

tabledelimiterrow
:
	TABLEDELIMINATORCELL+ PIPE? NEWLINE
;

HEADINGLINE
:
	'#'+ IGNORE_WS LINECHAR+
;

LISTLINE
:
	WS* (BULLET | LISTNUMBER) WS+ LINECHAR*
;

PARAGRAPHLINE
:
	INITIALPARACHAR LINECHAR*
;

/* low priority since it can match empty string */
CELLTEXT
:
	PIPE IGNORE_WS CELLCHAR*
;

TABLEDELIMINATORCELL
:
	PIPE? ':'? '-'+ ':'?
;

NEWLINE
:
	'\r'? '\n'
;

IGNORE_WS
:
	WS -> skip
;

PIPE
:
	'|'
;

fragment WS
: 
	[  \t] 
;

fragment LISTNUMBER
:
	[1-9] [.)]
;

fragment BULLET
:
	[-+*]
;

fragment
CELLCHAR
:
	~[|\n\r]
;

fragment
INITIALPARACHAR
:
	~[#|\n\r]
;

fragment
LINECHAR
:
	~[\n\r]
;


