/* ============================================================
   Assignment 3: Lexical Analyzer using LEX
   For English Language Parts of Speech AND C Programming Language tokens
   -- WITH Symbol Table support --

   HOW TO COMPILE AND RUN (Linux / WSL / Git Bash):
   ==================================================
   Step 1: Install flex and gcc if not present:
             sudo apt update
             sudo apt install -y flex gcc

   Step 2: Save this file as assign3.l

   Step 3: Generate the C source from LEX:
             flex assign3.l

   Step 4: Compile the generated file:
             gcc lex.yy.c -o assign3 -lfl
             (if -lfl not found try: gcc lex.yy.c -o assign3)

   Step 5: Run:
             ./assign3
             (Type sentences or C code, press Ctrl+D to finish)

   Step 6: On Windows CMD / PowerShell, use WSL or MinGW with flex installed.
   ==================================================

   WHAT IT DOES:
   - Recognizes English POS: NOUN, VERB, ADJECTIVE, ADVERB, PRONOUN,
     PREPOSITION, CONJUNCTION, ARTICLE
   - Recognizes C tokens: keywords, identifiers, numbers, operators, strings
   - Maintains a Symbol Table for identifiers (without duplicate entries)
   - Prints token type for every token found
   ============================================================ */

%{
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/* ── Symbol Table ──────────────────────────────────── */
#define MAX_SYM 200
char symTable[MAX_SYM][64];
int  symCount = 0;

/* Add symbol if not already present; return index (1-based) */
int addSymbol(const char *name) {
    for (int i = 0; i < symCount; i++)
        if (strcmp(symTable[i], name) == 0) return i + 1;
    if (symCount < MAX_SYM) {
        strncpy(symTable[symCount], name, 63);
        symTable[symCount][63] = '\0';
        return ++symCount;
    }
    return -1; /* table full */
}

void printSymbolTable() {
    printf("\n============ SYMBOL TABLE ============\n");
    printf("%-5s  %-30s\n", "Idx", "Identifier");
    printf("--------------------------------------\n");
    for (int i = 0; i < symCount; i++)
        printf("%-5d  %-30s\n", i + 1, symTable[i]);
    if (symCount == 0) printf("  (empty)\n");
    printf("======================================\n");
}

/* ── English POS dictionaries ──────────────────────── */
const char *NOUNS[]        = {"dog","cat","city","car","tree","boy","girl",
                               "man","woman","book","house",NULL};
const char *VERBS[]        = {"run","eat","drive","jump","is","are","was",
                               "write","read","go","come","have","has","do",NULL};
const char *ADJECTIVES[]   = {"big","small","fast","beautiful","happy",
                               "slow","good","bad","new","old",NULL};
const char *ADVERBS[]      = {"quickly","slowly","happily","very","well",NULL};
const char *PRONOUNS[]     = {"he","she","it","they","we","i","you","me","him",
                               "her","us","them",NULL};
const char *PREPOSITIONS[] = {"in","on","at","under","over","with","from",
                               "to","of","by",NULL};
const char *CONJUNCTIONS[] = {"and","or","but","because","if","then",
                               "while","although",NULL};
const char *ARTICLES[]     = {"a","an","the",NULL};

/* C Keywords */
const char *C_KEYWORDS[]   = {"int","float","double","char","void","if","else",
                               "while","for","do","return","struct","switch",
                               "case","break","continue","include","define",
                               "printf","scanf",NULL};

int inList(const char **list, const char *word) {
    for (int i = 0; list[i] != NULL; i++)
        if (strcasecmp(list[i], word) == 0) return 1;
    return 0;
}

const char *classifyEnglish(const char *word) {
    if (inList(NOUNS,        word)) return "NOUN";
    if (inList(VERBS,        word)) return "VERB";
    if (inList(ADJECTIVES,   word)) return "ADJECTIVE";
    if (inList(ADVERBS,      word)) return "ADVERB";
    if (inList(PRONOUNS,     word)) return "PRONOUN";
    if (inList(PREPOSITIONS, word)) return "PREPOSITION";
    if (inList(CONJUNCTIONS, word)) return "CONJUNCTION";
    if (inList(ARTICLES,     word)) return "ARTICLE";
    return NULL;
}
%}

DIGIT    [0-9]
ALPHA    [a-zA-Z_]
ALNUM    [a-zA-Z0-9_]
WS       [ \t\r]+

%%

{WS}          { /* skip whitespace */ }

\n            { printf("\n"); }

"//"[^\n]*    { printf("  [%-18s] %s\n", "C_LINE_COMMENT", yytext); }

"/*"([^*]|\*+[^*/])*\*+"/"  {
                  printf("  [%-18s] %s\n", "C_BLOCK_COMMENT", yytext);
              }

\"([^\"\\]|\\.)*\"  {
                  printf("  [%-18s] %s\n", "C_STRING_LITERAL", yytext);
              }

\'([^\'\\]|\\.)+\'  {
                  printf("  [%-18s] %s\n", "C_CHAR_LITERAL", yytext);
              }

"#"{ALPHA}{ALNUM}* {
                  printf("  [%-18s] %s\n", "C_PREPROCESSOR", yytext);
              }

{DIGIT}+("."{DIGIT}+)?([eE][+-]?{DIGIT}+)? {
                  printf("  [%-18s] %s\n", "NUMBER", yytext);
              }

{ALPHA}{ALNUM}* {
    /* First check C keywords */
    if (inList(C_KEYWORDS, yytext)) {
        printf("  [%-18s] %s\n", "C_KEYWORD", yytext);
    } else {
        /* Check English POS */
        const char *pos = classifyEnglish(yytext);
        if (pos != NULL) {
            printf("  [%-18s] %s\n", pos, yytext);
        } else {
            /* It's an identifier → add to symbol table */
            int idx = addSymbol(yytext);
            printf("  [%-18s] %s  (SymTab[%d])\n", "IDENTIFIER", yytext, idx);
        }
    }
}

"+="  { printf("  [%-18s] %s\n", "OP_ADD_ASSIGN", yytext); }
"-="  { printf("  [%-18s] %s\n", "OP_SUB_ASSIGN", yytext); }
"*="  { printf("  [%-18s] %s\n", "OP_MUL_ASSIGN", yytext); }
"/="  { printf("  [%-18s] %s\n", "OP_DIV_ASSIGN", yytext); }
"=="  { printf("  [%-18s] %s\n", "OP_EQ",         yytext); }
"!="  { printf("  [%-18s] %s\n", "OP_NEQ",        yytext); }
"<="  { printf("  [%-18s] %s\n", "OP_LE",         yytext); }
">="  { printf("  [%-18s] %s\n", "OP_GE",         yytext); }
"++"  { printf("  [%-18s] %s\n", "OP_INC",        yytext); }
"--"  { printf("  [%-18s] %s\n", "OP_DEC",        yytext); }
"&&"  { printf("  [%-18s] %s\n", "OP_AND",        yytext); }
"||"  { printf("  [%-18s] %s\n", "OP_OR",         yytext); }
"+"   { printf("  [%-18s] %s\n", "OP_PLUS",       yytext); }
"-"   { printf("  [%-18s] %s\n", "OP_MINUS",      yytext); }
"*"   { printf("  [%-18s] %s\n", "OP_MUL",        yytext); }
"/"   { printf("  [%-18s] %s\n", "OP_DIV",        yytext); }
"%"   { printf("  [%-18s] %s\n", "OP_MOD",        yytext); }
"<"   { printf("  [%-18s] %s\n", "OP_LT",         yytext); }
">"   { printf("  [%-18s] %s\n", "OP_GT",         yytext); }
"="   { printf("  [%-18s] %s\n", "OP_ASSIGN",     yytext); }
"!"   { printf("  [%-18s] %s\n", "OP_NOT",        yytext); }
";"   { printf("  [%-18s] %s\n", "SEMICOLON",     yytext); }
","   { printf("  [%-18s] %s\n", "COMMA",         yytext); }
"."   { printf("  [%-18s] %s\n", "DOT",           yytext); }
"("   { printf("  [%-18s] %s\n", "LPAREN",        yytext); }
")"   { printf("  [%-18s] %s\n", "RPAREN",        yytext); }
"{"   { printf("  [%-18s] %s\n", "LBRACE",        yytext); }
"}"   { printf("  [%-18s] %s\n", "RBRACE",        yytext); }
"["   { printf("  [%-18s] %s\n", "LBRACKET",      yytext); }
"]"   { printf("  [%-18s] %s\n", "RBRACKET",      yytext); }

.     { printf("  [%-18s] %s\n", "UNKNOWN",        yytext); }

%%

int yywrap() { return 1; }

int main() {
    printf("========================================\n");
    printf("  Lexical Analyzer (LEX) — Assign 3\n");
    printf("  English POS + C Language Tokens\n");
    printf("  Enter text (Ctrl+D to end):\n");
    printf("========================================\n\n");
    printf("  %-20s  %-30s\n", "Token Type", "Lexeme");
    printf("  %s\n", "----------------------------------------------------");
    yylex();
    printSymbolTable();
    return 0;
}
