/* ============================================================
   Assignment 4: YACC — Arithmetic Expression Evaluator
   Supports: arithmetic expressions, built-in functions, variables
   Files needed: assign4.l (lexer) + assign4.y (parser, this file)

   HOW TO COMPILE AND RUN (Linux / WSL / Git Bash):
   ==================================================
   Step 1: Install tools:
             sudo apt update
             sudo apt install -y flex bison gcc

   Step 2: Save this file as assign4.y
           Save the lexer below (at end) as assign4.l

   Step 3: Generate parser source:
             bison -d assign4.y
             (produces assign4.tab.c and assign4.tab.h)

   Step 4: Generate lexer source:
             flex assign4.l
             (produces lex.yy.c)

   Step 5: Compile everything together:
             gcc lex.yy.c assign4.tab.c -o assign4 -lm
             (link math library with -lm)

   Step 6: Run:
             ./assign4
             Type expressions like:
               3 + 4 * 2
               x = 5
               x + 3
               sqrt(16)
               sin(0)
               y = x * 2 + 1
             Press Ctrl+D (or Ctrl+Z on Windows) to exit.
   ==================================================
   ============================================================ */

%{
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>

void yyerror(const char *s);
int  yylex();

/* ── Symbol Table for Variables ───────────────────── */
#define MAX_VARS 100
typedef struct {
    char  name[64];
    double value;
    int   defined;
} VarEntry;

VarEntry varTable[MAX_VARS];
int varCount = 0;

int lookupVar(const char *name) {
    for (int i = 0; i < varCount; i++)
        if (strcmp(varTable[i].name, name) == 0) return i;
    return -1;
}

double getVar(const char *name) {
    int i = lookupVar(name);
    if (i == -1) { printf("  [ERROR] Undefined variable '%s'\n", name); return 0; }
    return varTable[i].value;
}

void setVar(const char *name, double val) {
    int i = lookupVar(name);
    if (i == -1) {
        if (varCount < MAX_VARS) {
            strncpy(varTable[varCount].name, name, 63);
            varTable[varCount].value   = val;
            varTable[varCount].defined = 1;
            varCount++;
        }
    } else {
        varTable[i].value = val;
    }
}

void printVarTable() {
    printf("\n====== VARIABLE (SYMBOL) TABLE ======\n");
    printf("%-5s  %-15s  %-10s\n", "Idx", "Name", "Value");
    printf("-------------------------------------\n");
    for (int i = 0; i < varCount; i++)
        printf("%-5d  %-15s  %-10.4f\n", i+1, varTable[i].name, varTable[i].value);
    if (!varCount) printf("  (empty)\n");
    printf("=====================================\n");
}

double callFunc(const char *fname, double arg) {
    if      (strcmp(fname,"sqrt") == 0) return sqrt(arg);
    else if (strcmp(fname,"sin")  == 0) return sin(arg);
    else if (strcmp(fname,"cos")  == 0) return cos(arg);
    else if (strcmp(fname,"tan")  == 0) return tan(arg);
    else if (strcmp(fname,"log")  == 0) return log(arg);
    else if (strcmp(fname,"log2") == 0) return log2(arg);
    else if (strcmp(fname,"exp")  == 0) return exp(arg);
    else if (strcmp(fname,"abs")  == 0) return fabs(arg);
    else if (strcmp(fname,"ceil") == 0) return ceil(arg);
    else if (strcmp(fname,"floor")== 0) return floor(arg);
    else { printf("  [ERROR] Unknown function '%s'\n", fname); return 0; }
}
%}

/* ── Token types ─────────────────────────────────── */
%union {
    double  dval;
    char    sval[64];
}

%token <dval>  NUMBER
%token <sval>  NAME
%token         NEWLINE

%type  <dval>  expr statement

/* ── Operator Precedence (lowest to highest) ─────── */
%right '='
%left  '+' '-'
%left  '*' '/'
%right UMINUS

%%

program:
    /* empty */
  | program line
  ;

line:
    NEWLINE
  | statement NEWLINE    { printf("  => Result: %.6g\n\n", $1); printVarTable(); }
  | error NEWLINE        { yyerrok; printf("  [SYNTAX ERROR]\n\n"); }
  ;

statement:
    expr                 { $$ = $1; }
  | NAME '=' expr        {
        setVar($1, $3);
        printf("  => %s = %.6g\n", $1, $3);
        $$ = $3;
    }
  ;

expr:
    NUMBER                        { $$ = $1; }
  | NAME                          { $$ = getVar($1); }
  | NAME '(' expr ')'             { $$ = callFunc($1, $3); }
  | expr '+' expr                 { $$ = $1 + $3; }
  | expr '-' expr                 { $$ = $1 - $3; }
  | expr '*' expr                 { $$ = $1 * $3; }
  | expr '/' expr                 {
        if ($3 == 0) { printf("  [ERROR] Division by zero\n"); $$ = 0; }
        else $$ = $1 / $3;
    }
  | '-' expr %prec UMINUS         { $$ = -$2; }
  | '(' expr ')'                  { $$ = $2; }
  ;

%%

void yyerror(const char *s) {
    fprintf(stderr, "  [PARSE ERROR]: %s\n", s);
}

int main() {
    printf("=======================================\n");
    printf("  YACC Arithmetic Expression Evaluator\n");
    printf("  Assignment 4\n");
    printf("  Supported: +, -, *, /, (), variables,\n");
    printf("  Built-ins: sqrt sin cos tan log exp abs\n");
    printf("  Type expressions (Ctrl+D to quit):\n");
    printf("=======================================\n\n");
    yyparse();
    return 0;
}
