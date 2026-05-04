import java.util.*;

/**
 * Assignment 6: Code Optimization Techniques on Three Address Code (TAC)
 *
 * Techniques Implemented:
 *  1. Constant Folding       — evaluate constant expressions at compile time
 *                               e.g.  t1 = 3 + 4  →  t1 = 7
 *  2. Constant Propagation   — substitute known constant values into expressions
 *                               e.g.  a = 5 ; t1 = a + 2  →  t1 = 5 + 2  →  t1 = 7
 *  3. Dead Code Elimination  — remove assignments whose result is never used
 *  4. Copy Propagation       — replace a variable that is a simple copy of another
 *                               e.g.  x = y ; z = x + 1  →  z = y + 1
 *  5. Algebraic Simplification — x * 1 → x, x + 0 → x, x * 0 → 0, x / 1 → x
 *
 * HOW TO COMPILE AND RUN:
 * ========================
 * Step 1: Open terminal in the folder containing this file.
 * Step 2: Compile:
 *           javac Assign6CodeOptimization.java
 * Step 3: Run:
 *           java Assign6CodeOptimization
 * Step 4: The program first shows optimization for hardcoded sample TAC,
 *         then lets you enter your own TAC interactively.
 *         Type each TAC line, then type 'end' to trigger optimization.
 *         Type 'exit' to quit the program.
 *
 * TAC FORMAT (supported):
 *   var = constant            e.g.  a = 5
 *   var = var                 e.g.  x = a
 *   var = op1 OP op2          e.g.  t1 = a + b
 *   label:                    e.g.  L1:
 *   if cond goto LABEL        e.g.  if t1 goto L1
 *   goto LABEL                e.g.  goto L2
 * ========================
 */
public class Assign6CodeOptimization {

    // ─── Represents one TAC instruction ──────────────────────────────────────
    static class TAC {
        String raw;          // original text
        String dest;         // LHS variable (null if label/goto/if)
        String op1, op, op2; // RHS operands (op2 may be null for copy)
        boolean isLabel;     // e.g. "L1:"
        boolean isGoto;      // e.g. "goto L1"
        boolean isIf;        // e.g. "if t1 goto L1"
        boolean dead;        // mark for dead-code elimination

        TAC(String raw) {
            this.raw = raw.trim();
            parse();
        }

        void parse() {
            String s = raw.trim();
            if (s.endsWith(":") && !s.contains("=") && !s.contains(" ")) {
                isLabel = true; return;
            }
            if (s.startsWith("goto ")) {
                isGoto = true; return;
            }
            if (s.startsWith("if ")) {
                isIf = true; return;
            }
            // Expect: dest = RHS
            int eq = s.indexOf('=');
            if (eq == -1) return;
            dest = s.substring(0, eq).trim();
            String rhs = s.substring(eq + 1).trim();
            String[] parts = rhs.split("\\s+");
            if (parts.length == 1) {
                // copy: dest = var_or_const
                op1 = parts[0]; op = null; op2 = null;
            } else if (parts.length == 3) {
                op1 = parts[0]; op = parts[1]; op2 = parts[2];
            } else if (parts.length == 2 && parts[0].equals("-")) {
                // unary minus: dest = - var
                op1 = parts[0]; op = "unary"; op2 = parts[1];
            }
        }

        String rebuild() {
            if (isLabel || isGoto || isIf || dest == null) return raw;
            if (op == null)       return dest + " = " + op1;
            if (op.equals("unary")) return dest + " = -" + op2;
            return dest + " = " + op1 + " " + op + " " + op2;
        }
    }

    // ─── Check if a string is a numeric constant ──────────────────────────────
    static boolean isConstant(String s) {
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    static double toDouble(String s) { return Double.parseDouble(s); }

    static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    // ─── PASS: Constant Propagation + Constant Folding + Copy Propagation ────
    static List<TAC> passConstAndCopy(List<TAC> code) {
        Map<String, String> env = new LinkedHashMap<>(); // var → value or var

        for (TAC t : code) {
            if (t.isLabel || t.isGoto || t.isIf || t.dest == null) continue;

            // Propagate op1
            if (t.op1 != null && !isConstant(t.op1) && env.containsKey(t.op1))
                t.op1 = env.get(t.op1);

            // Propagate op2
            if (t.op2 != null && !isConstant(t.op2) && env.containsKey(t.op2))
                t.op2 = env.get(t.op2);

            // Constant Folding: both operands are constants
            if (t.op != null && !t.op.equals("unary") && isConstant(t.op1) && isConstant(t.op2)) {
                double a = toDouble(t.op1), b = toDouble(t.op2);
                double result;
                switch (t.op) {
                    case "+": result = a + b; break;
                    case "-": result = a - b; break;
                    case "*": result = a * b; break;
                    case "/": result = (b == 0) ? 0 : a / b; break;
                    default:  result = 0;
                }
                String folded = fmt(result);
                env.put(t.dest, folded);
                t.op1 = folded; t.op = null; t.op2 = null;
                t.raw = t.rebuild();
                continue;
            }

            // Unary minus constant folding
            if (t.op != null && t.op.equals("unary") && isConstant(t.op2)) {
                String folded = fmt(-toDouble(t.op2));
                env.put(t.dest, folded);
                t.op1 = folded; t.op = null; t.op2 = null;
                t.raw = t.rebuild();
                continue;
            }

            // Copy propagation: dest = single value
            if (t.op == null && t.op1 != null) {
                env.put(t.dest, t.op1);
            } else {
                // dest is now complex — remove stale mapping
                env.remove(t.dest);
            }

            t.raw = t.rebuild();
        }
        return code;
    }

    // ─── PASS: Algebraic Simplification ──────────────────────────────────────
    static List<TAC> passAlgebraic(List<TAC> code) {
        for (TAC t : code) {
            if (t.dest == null || t.op == null || t.op.equals("unary")) continue;
            String a = t.op1, b = t.op2, o = t.op;

            // x * 1 = x,  1 * x = x
            if (o.equals("*") && b.equals("1"))        { t.op = null; t.op1 = a; t.op2 = null; }
            else if (o.equals("*") && a.equals("1"))   { t.op = null; t.op1 = b; t.op2 = null; }
            // x * 0 = 0,  0 * x = 0
            else if (o.equals("*") && (b.equals("0") || a.equals("0"))) {
                t.op = null; t.op1 = "0"; t.op2 = null;
            }
            // x + 0 = x,  0 + x = x
            else if (o.equals("+") && b.equals("0"))   { t.op = null; t.op1 = a; t.op2 = null; }
            else if (o.equals("+") && a.equals("0"))   { t.op = null; t.op1 = b; t.op2 = null; }
            // x - 0 = x
            else if (o.equals("-") && b.equals("0"))   { t.op = null; t.op1 = a; t.op2 = null; }
            // x / 1 = x
            else if (o.equals("/") && b.equals("1"))   { t.op = null; t.op1 = a; t.op2 = null; }

            t.raw = t.rebuild();
        }
        return code;
    }

    // ─── PASS: Dead Code Elimination ─────────────────────────────────────────
    static List<TAC> passDeadCode(List<TAC> code) {
        // Collect all variables that appear on the RHS or in if/goto
        Set<String> used = new HashSet<>();
        for (TAC t : code) {
            if (t.isIf || t.isGoto) {
                // Mark all tokens in the line as used
                Arrays.stream(t.raw.split("\\s+")).forEach(used::add);
                continue;
            }
            if (t.dest == null) continue;
            if (t.op1 != null && !isConstant(t.op1)) used.add(t.op1);
            if (t.op2 != null && !isConstant(t.op2)) used.add(t.op2);
        }

        // Mark dead: assigned but never used afterwards
        // Simple approach: temps (t1, t2, ...) not in used set
        for (TAC t : code) {
            if (t.dest != null && t.dest.matches("t\\d+") && !used.contains(t.dest)) {
                t.dead = true;
            }
        }

        List<TAC> result = new ArrayList<>();
        for (TAC t : code) if (!t.dead) result.add(t);
        return result;
    }

    // ─── Full Optimization Pipeline ───────────────────────────────────────────
    static List<TAC> optimize(List<String> lines) {
        List<TAC> code = new ArrayList<>();
        for (String l : lines) if (!l.trim().isEmpty()) code.add(new TAC(l));

        System.out.println("\n>> Step 1: Constant Propagation + Folding + Copy Propagation");
        code = passConstAndCopy(code);

        System.out.println(">> Step 2: Algebraic Simplification");
        code = passAlgebraic(code);

        System.out.println(">> Step 3: Dead Code Elimination");
        code = passDeadCode(code);

        return code;
    }

    // ─── Print comparison ─────────────────────────────────────────────────────
    static void printComparison(List<String> original, List<TAC> optimized) {
        System.out.println("\n+------------------------------------------------------+");
        System.out.println("|       ORIGINAL TAC           |    OPTIMIZED TAC       |");
        System.out.println("+------------------------------------------------------+");
        int maxOrig = original.stream().mapToInt(String::length).max().orElse(20);
        maxOrig = Math.max(maxOrig, 20);
        int n = Math.max(original.size(), optimized.size());
        for (int i = 0; i < n; i++) {
            String orig = i < original.size()  ? original.get(i).trim() : "";
            String opt  = i < optimized.size() ? optimized.get(i).raw   : "";
            System.out.printf("|  %-28s  |  %-20s  |%n", orig, opt);
        }
        System.out.println("+------------------------------------------------------+");
        System.out.printf("  Original instructions: %d  ->  Optimized: %d%n",
            original.size(), optimized.size());
    }

    // ─── Run demo or interactive ──────────────────────────────────────────────
    static void runOptimization(List<String> tac) {
        System.out.println("\n>> Original TAC:");
        tac.forEach(l -> System.out.println("   " + l));

        List<TAC> result = optimize(tac);
        printComparison(tac, result);
    }

    // ─── Main ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" Assignment 6: Code Optimization Techniques");
        System.out.println(" Applied on Three Address Code (TAC)");
        System.out.println("==============================================");

        // ── Sample 1: Basic constant folding and propagation ──
        System.out.println("\n===== SAMPLE 1: Constant Folding & Propagation =====");
        runOptimization(Arrays.asList(
            "a = 5",
            "b = 3",
            "t1 = a + b",
            "t2 = t1 * 2",
            "c = t2",
            "t3 = c + 0",
            "result = t3"
        ));

        // ── Sample 2: Dead code + algebraic ──
        System.out.println("\n===== SAMPLE 2: Dead Code + Algebraic Simplification =====");
        runOptimization(Arrays.asList(
            "x = 4",
            "y = 1",
            "t1 = x * y",
            "t2 = t1 + 0",
            "t3 = x * 0",
            "t4 = t2 + t3",
            "result = t4"
        ));

        // ── Sample 3: Copy propagation ──
        System.out.println("\n===== SAMPLE 3: Copy Propagation =====");
        runOptimization(Arrays.asList(
            "a = 10",
            "x = a",
            "t1 = x + 5",
            "y = x",
            "t2 = y * 2",
            "result = t1 + t2"
        ));

        // ── Interactive Mode ──
        System.out.println("\n==============================================");
        System.out.println(" INTERACTIVE MODE");
        System.out.println(" Enter TAC lines (one per line).");
        System.out.println(" Type 'end' to run optimization.");
        System.out.println(" Type 'exit' to quit.");
        System.out.println("==============================================");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nEnter TAC (type 'end' when done, 'exit' to quit):");
            List<String> userTAC = new ArrayList<>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("exit")) { sc.close(); return; }
                if (line.equalsIgnoreCase("end"))  break;
                if (!line.isEmpty()) userTAC.add(line);
            }
            if (!userTAC.isEmpty()) runOptimization(userTAC);
        }
    }
}
