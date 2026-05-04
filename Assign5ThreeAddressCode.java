import java.util.*;

/**
 * Assignment 5: Three Address Code (TAC) Generator
 * For C-style arithmetic expressions with assignments and if-statements.
 *
 * Handles:
 *  - Simple assignments:  result = a + b * c - d / e
 *  - Nested parentheses:  x = (a + b) * (c - d)
 *  - if statements:       if (a > b) then x = a + b
 *  - Unary minus:         x = -a
 *
 * HOW TO COMPILE AND RUN:
 * ========================
 * Step 1: Open terminal in the folder containing this file.
 * Step 2: Compile:
 *           javac Assign5ThreeAddressCode.java
 * Step 3: Run:
 *           java Assign5ThreeAddressCode
 * Step 4: The program first shows TAC for hardcoded sample expressions,
 *         then enters interactive mode where you can type your own.
 *         Type 'exit' to quit.
 *
 * SAMPLE INPUTS:
 *   result = a + b * c - d
 *   x = (a + b) * (c - d)
 *   if (a > b) then x = a + b
 *   z = -a + b
 * ========================
 */
public class Assign5ThreeAddressCode {

    static int tempCount = 1;
    static int labelCount = 1;
    static List<String> tacCode = new ArrayList<>();

    // ─── Generate a new temporary variable ───────────────────────────────────
    static String newTemp() {
        return "t" + (tempCount++);
    }

    // ─── Generate a new label ────────────────────────────────────────────────
    static String newLabel() {
        return "L" + (labelCount++);
    }

    // ─── Emit a TAC instruction ──────────────────────────────────────────────
    static void emit(String instr) {
        tacCode.add(instr);
        System.out.println("  " + instr);
    }

    // ─── Find the split point for an operator at lowest precedence ───────────
    // Scans RIGHT-TO-LEFT so left-associativity is respected.
    static int findOperator(String expr, String ops) {
        int depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (depth == 0 && ops.indexOf(c) != -1) {
                // Guard: don't split on unary minus at position 0
                if ((c == '-' || c == '+') && i == 0) continue;
                // Don't split on '-' or '+' that follows an operator (unary context)
                if (i > 0) {
                    char prev = expr.charAt(i - 1);
                    if ("+-*/(".indexOf(prev) != -1) continue;
                }
                return i;
            }
        }
        return -1;
    }

    // ─── Recursive TAC Generator ─────────────────────────────────────────────
    // Returns the variable/temp holding the result of expr
    static String generateTAC(String expr) {
        expr = expr.trim();

        // Strip outer parentheses
        if (expr.startsWith("(") && expr.endsWith(")") && matchingClose(expr, 0) == expr.length() - 1) {
            return generateTAC(expr.substring(1, expr.length() - 1));
        }

        // --- Pass 1: lowest precedence = + and -
        int idx = findOperator(expr, "+-");
        if (idx != -1) {
            String left  = expr.substring(0, idx).trim();
            String right = expr.substring(idx + 1).trim();
            char   op    = expr.charAt(idx);
            String t1 = generateTAC(left);
            String t2 = generateTAC(right);
            String t  = newTemp();
            emit(t + " = " + t1 + " " + op + " " + t2);
            return t;
        }

        // --- Pass 2: * and /
        idx = findOperator(expr, "*/");
        if (idx != -1) {
            String left  = expr.substring(0, idx).trim();
            String right = expr.substring(idx + 1).trim();
            char   op    = expr.charAt(idx);
            String t1 = generateTAC(left);
            String t2 = generateTAC(right);
            String t  = newTemp();
            emit(t + " = " + t1 + " " + op + " " + t2);
            return t;
        }

        // --- Unary minus
        if (expr.startsWith("-")) {
            String operand = generateTAC(expr.substring(1));
            String t = newTemp();
            emit(t + " = -" + operand);
            return t;
        }

        // --- Base case: literal or variable
        return expr;
    }

    // ─── Find index of the closing ')' matching '(' at position open ─────────
    static int matchingClose(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '(') depth++;
            else if (s.charAt(i) == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // ─── Handle assignment: LHS = RHS ────────────────────────────────────────
    static void handleAssignment(String line) {
        int eq = line.indexOf('=');
        if (eq == -1) {
            System.out.println("  [ERROR] No '=' found in: " + line);
            return;
        }
        String lhs = line.substring(0, eq).trim();
        String rhs = line.substring(eq + 1).trim();
        String t = generateTAC(rhs);
        emit(lhs + " = " + t);
    }

    // ─── Handle: if (condition) then statement ────────────────────────────────
    static void handleIf(String line) {
        // Expect: if (cond) then LHS = RHS
        int condStart = line.indexOf('(');
        int condEnd   = matchingClose(line, condStart);
        if (condStart == -1 || condEnd == -1) {
            System.out.println("  [ERROR] Malformed if-statement: " + line);
            return;
        }
        String cond  = line.substring(condStart + 1, condEnd).trim();
        String rest  = line.substring(condEnd + 1).trim();
        if (rest.startsWith("then")) rest = rest.substring(4).trim();

        String lTrue  = newLabel();
        String lFalse = newLabel();

        // Condition: split on relational operator
        for (String relOp : new String[]{"==","!=","<=",">=","<",">"}) {
            int ri = cond.indexOf(relOp);
            if (ri != -1) {
                String cl = cond.substring(0, ri).trim();
                String cr = cond.substring(ri + relOp.length()).trim();
                String tl = generateTAC(cl);
                String tr = generateTAC(cr);
                String tc = newTemp();
                emit(tc + " = " + tl + " " + relOp + " " + tr);
                emit("if " + tc + " goto " + lTrue);
                emit("goto " + lFalse);
                emit(lTrue + ":");
                handleAssignment(rest);
                emit(lFalse + ":");
                return;
            }
        }
        System.out.println("  [ERROR] No relational operator in condition: " + cond);
    }

    // ─── Reset counters between examples ─────────────────────────────────────
    static void reset() {
        tempCount = 1;
        labelCount = 1;
        tacCode.clear();
    }

    // ─── Run one expression and display its TAC ───────────────────────────────
    static void run(String input) {
        System.out.println("\n>> Input: " + input);
        System.out.println("   TAC Instructions:");
        reset();
        String trimmed = input.trim();
        if (trimmed.startsWith("if")) {
            handleIf(trimmed);
        } else if (trimmed.contains("=")) {
            handleAssignment(trimmed);
        } else {
            String t = generateTAC(trimmed);
            emit("(result) = " + t);
        }
        System.out.println(">> Done.");
    }

    // ─── Main ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" Assignment 5: Three Address Code Generator");
        System.out.println(" C-Style Expressions");
        System.out.println("==============================================");

        // ── Hardcoded sample demonstrations ──
        String[] samples = {
            "result = a + b * c - d",
            "x = (a + b) * (c - d)",
            "y = a * b + c * d - e",
            "z = a + b * (c - d / e)",
            "w = -a + b",
            "if (a > b) then x = a + b",
            "if (x == y) then z = x * 2"
        };

        System.out.println("\n--- SAMPLE EXPRESSIONS ---");
        for (String s : samples) run(s);

        // ── Interactive Mode ──
        System.out.println("\n==============================================");
        System.out.println(" Interactive Mode");
        System.out.println(" Enter an expression/assignment/if-statement");
        System.out.println(" Type 'exit' to quit");
        System.out.println("==============================================");

        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.isEmpty()) continue;
            run(line);
        }
        sc.close();
        System.out.println("\nGoodbye!");
    }
}
