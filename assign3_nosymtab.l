/* ============================================================
   Assignment 3: Lexical Analyzer using LEX
   For English Language Parts of Speech AND C Programming Language tokens
   -- WITHOUT Symbol Table --

   HOW TO COMPILE AND RUN (Linux / WSL Terminal):
   ================================================
   Step 1: Install flex and gcc (one time only):
             sudo apt update
             sudo apt install -y flex gcc

   Step 2: Navigate to the file directory:
             cd /mnt/d/LPCC/practical/final

   Step 3: Generate C source from LEX specification:
             flex assign3_nosymtab.l
             (This produces: lex.yy.c)

   Step 4: Compile the generated C file:
             gcc lex.yy.c -o assign3_nosymtab -lfl
             If -lfl gives error, try:
             gcc lex.yy.c -o assign3_nosymtab

   Step 5: Run the program:
             ./assign3_nosymtab

   Step 6: Type any English sentence or C code and press Enter.
           Press Ctrl+D when done to see final output.

   SAMPLE INPUTS TO TRY:
   ----------------------
   English: the big dog runs quickly
   C Code:  int x = 5 + 3;
   Mixed:   if (x > 0) the dog runs fast

   DIFFERENCE FROM assign3.l (WITH symbol table):
   ------------------------------------------------
   - This version does NOT maintain a symbol table
   - Identifiers are simply labeled as IDENTIFIER without tracking
   - assign3.l stores each unique identifier in a symbol table
   ================================================
   ============================================================ */

%{
#include <stdio.h>
#include <string.h>

/* ── English Parts of Speech word lists ────────────────────── */
const char *NOUNS[]        = {"dog","cat","city","car","tree","boy","girl",
                               "man","woman","book","house","bird","fish",NULL};
const char *VERBS[]        = {"run","eat","drive","jump","is","are","was",
                               "write","read","go","come","have","has","do",
                               "runs","eats","drives","jumps","goes",NULL};
const char *ADJECTIVES[]   = {"big","small","fast","beautiful","happy",
                               "slow","good","bad","new","old","tall","short",NULL};
const char *ADVERBS[]      = {"quickly","slowly","happily","very","well",
                               "often","always","never","fast",NULL};
const char *PRONOUNS[]     = {"he","she","it","they","we","i","you","me",
                               "him","her","us","them","my","his","her",NULL};
const char *PREPOSITIONS[] = {"in","on","at","under","over","with","from",
                               "to","of","by","into","onto","beside",NULL};
const char *CONJUNCTIONS[] = {"and","or","but","because","if","then",
                               "while","although","since","unless",NULL};
const char *ARTICLES[]     = {"a","an","the",NULL};

/* ── C Keywords ──────────────────────────────────────────── */
const char *C_KEYWORDS[]   = {"int","float","double","char","void","if","else",
                               "while","for","do","return","struct","switch",
                               "case","break","continue","include","define",
                               "printf","scanf","sizeof","typedef","enum",NULL};

/* ── Helper: check if word is in a list (case-insensitive) ── */
int inList(const char **list, const char *word) {
    for (int i = 0; list[i] != NULL; i++)
        if (strcasecmp(list[i], word) == 0) return 1;
    return 0;
}

/* ── Classify English word → return POS string or NULL ────── */
const char *classifyEnglish(const char *word) {
    if (inList(NOUNS,        word)) return "NOUN";
    if (inList(VERBS,        word)) return "VERB";
    if (inList(ADJECTIVES,   word)) return "ADJECTIVE";
    if (inList(ADVERBS,      word)) return "ADVERB";
    if (inList(PRONOUNS,     word)) return "PRONOUN";
    if (inList(PREPOSITIONS, word)) return "PREPOSITION";
    if (inList(CONJUNCTIONS, word)) return "CONJUNCTION";
    if (inList(ARTICLES,     word)) return "ARTICLE";
    return NULL; /* unknown — treat as IDENTIFIER */
}
%}

/* ── Pattern Definitions ────────────────────────────────── */
DIGIT    [0-9]
ALPHA    [a-zA-Z_]
ALNUM    [a-zA-Z0-9_]
WS       [ \t\r]+

%%

{WS}          { /* skip whitespace — no action */ }

\n            { printf("\n"); /* print newline for readability */ }

"//"[^\n]*    { printf("  [%-18s] %s\n", "C_LINE_COMMENT",   yytext); }

"/*"([^*]|\*+[^*/])*\*+"/"  {
                  printf("  [%-18s] %s\n", "C_BLOCK_COMMENT",  yytext);
              }

\"([^\"\\]|\\.)*\"  {
                  printf("  [%-18s] %s\n", "C_STRING_LITERAL", yytext);
              }

\'([^\'\\]|\\.)+\'  {
                  printf("  [%-18s] %s\n", "C_CHAR_LITERAL",   yytext);
              }

"#"{ALPHA}{ALNUM}* {
                  printf("  [%-18s] %s\n", "C_PREPROCESSOR",   yytext);
              }

{DIGIT}+("."{DIGIT}+)?([eE][+-]?{DIGIT}+)? {
                  printf("  [%-18s] %s\n", "NUMBER",            yytext);
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
            /* Unknown word — label as IDENTIFIER (no symbol table) */
            printf("  [%-18s] %s\n", "IDENTIFIER", yytext);
        }
    }
}

/* ── C Operators (multi-char first, then single) ─────────── */
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
    printf("==========================================\n");
    printf("  Lexical Analyzer (LEX) - Assignment 3  \n");
    printf("  WITHOUT Symbol Table                   \n");
    printf("  Recognizes: English POS + C Tokens     \n");
    printf("==========================================\n");
    printf("  Enter text below (Ctrl+D to finish):\n");
    printf("------------------------------------------\n\n");
    printf("  %-20s  %s\n", "Token Type", "Lexeme");
    printf("  %-20s  %s\n", "----------", "------");

    yylex();

    printf("\n==========================================\n");
    printf("  [Done] No Symbol Table maintained.\n");
    printf("==========================================\n");
    return 0;
}
