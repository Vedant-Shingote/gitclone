import java.util.*;

/**
 * Assignment 2: Two-Pass Macro Processor
 * Handles: Simple Macro, Nested Macro, Macro with Parameters
 * Data Structures: MNT, MDT, ALA (Argument List Array), Intermediate Code
 *
 * HOW TO COMPILE AND RUN:
 * ========================
 * Step 1: Open terminal in the folder containing this file.
 * Step 2: Compile:
 *           javac Assign2MacroProcessor.java
 * Step 3: Run:
 *           java Assign2MacroProcessor
 * Step 4: The program uses a hardcoded source program (see SOURCE array).
 *         Output is printed to the console.
 * ========================
 */
public class Assign2MacroProcessor {

    // ─── Data Structures ────────────────────────────────────────────────────

    /** MNT entry: macro name → (MDT start index, formal params) */
    static class MNTEntry {
        String name;
        int mdtStart;         // 1-based index into MDT
        List<String> params;

        MNTEntry(String name, int mdtStart, List<String> params) {
            this.name = name;
            this.mdtStart = mdtStart;
            this.params = params;
        }
    }

    static List<MNTEntry>      MNT    = new ArrayList<>();
    static List<String>        MDT    = new ArrayList<>(); // stores body lines (param-replaced)
    static Map<String,MNTEntry> MNT_MAP = new LinkedHashMap<>();

    // ─── Sample Source Program ───────────────────────────────────────────────
    static final String[] SOURCE = {
        "LOAD A",
        "STORE B",
        "",
        "MACRO ABC",
        "LOAD p",
        "SUB q",
        "MEND",
        "",
        "MACRO ADD1 ARG",
        "LOAD X",
        "STORE ARG",
        "MEND",
        "",
        "MACRO ADD5 A1, A2, A3",
        "STORE A2",
        "ADD1 5",
        "ADD1 10",
        "LOAD A1",
        "LOAD A3",
        "MEND",
        "",
        "ABC",
        "ADD5 D1, D2, D3",
        "END"
    };

    // ─── Helper: parse "MACRO NAME param1, param2" header ───────────────────
    static String[] parseMacroHeader(String line) {
        // returns [name, "param1,param2,..."] or [name, ""]
        String[] parts = line.trim().split("\\s+", 3);
        String name = parts[1];
        String params = parts.length == 3 ? parts[2] : "";
        return new String[]{name, params};
    }

    static List<String> splitParams(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return list;
        for (String p : raw.split(",")) list.add(p.trim());
        return list;
    }

    /** Replace formal params with positional markers #1, #2, ... */
    static String replaceParams(String bodyLine, List<String> formals) {
        String[] tokens = bodyLine.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            for (int j = 0; j < formals.size(); j++) {
                if (tokens[i].equals(formals.get(j))) {
                    tokens[i] = "#" + (j + 1);
                }
            }
        }
        return String.join(" ", tokens);
    }

    /** Substitute actual args for positional markers */
    static String substitute(String mdtLine, List<String> actuals) {
        String result = mdtLine;
        for (int i = 0; i < actuals.size(); i++)
            result = result.replace("#" + (i + 1), actuals.get(i));
        return result;
    }

    static String[] parseCall(String line) {
        // returns [name, "arg1,arg2,..."]
        String[] parts = line.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }

    // ─── PASS 1 ─────────────────────────────────────────────────────────────
    static List<String> pass1() {
        List<String> intermediate = new ArrayList<>();
        int i = 0;

        while (i < SOURCE.length) {
            String line = SOURCE[i].trim();

            if (line.startsWith("MACRO")) {
                // Parse header
                String[] header = parseMacroHeader(line);
                String macroName = header[0];
                List<String> formals = splitParams(header[1]);

                int mdtStart = MDT.size() + 1; // 1-based
                MNTEntry entry = new MNTEntry(macroName, mdtStart, formals);
                MNT.add(entry);
                MNT_MAP.put(macroName, entry);
                i++;

                // Read body until MEND
                while (i < SOURCE.length && !SOURCE[i].trim().equals("MEND")) {
                    String bodyLine = SOURCE[i].trim();
                    if (!bodyLine.isEmpty()) {
                        String[] call = parseCall(bodyLine);
                        String callName = call[0];

                        // EARLY EXPANSION: if nested macro call detected
                        if (MNT_MAP.containsKey(callName)) {
                            MNTEntry nested = MNT_MAP.get(callName);
                            List<String> callArgs = splitParams(call[1]);
                            int idx = nested.mdtStart - 1;
                            while (idx < MDT.size() && !MDT.get(idx).equals("MEND")) {
                                String expanded = substitute(MDT.get(idx), callArgs);
                                MDT.add(expanded);
                                idx++;
                            }
                        } else {
                            MDT.add(replaceParams(bodyLine, formals));
                        }
                    }
                    i++;
                }
                MDT.add("MEND");
                i++; // skip MEND

            } else {
                if (!line.isEmpty()) intermediate.add(line);
                i++;
            }
        }
        return intermediate;
    }

    // ─── PASS 2 ─────────────────────────────────────────────────────────────
    static List<String> pass2(List<String> intermediate) {
        List<String> output = new ArrayList<>();
        for (String line : intermediate) {
            if (line.equals("END")) { output.add("END"); break; }
            String[] call = parseCall(line);
            String name = call[0];
            if (MNT_MAP.containsKey(name)) {
                List<String> actuals = splitParams(call[1]);
                MNTEntry entry = MNT_MAP.get(name);
                int idx = entry.mdtStart - 1;
                while (idx < MDT.size() && !MDT.get(idx).equals("MEND")) {
                    output.add(substitute(MDT.get(idx), actuals));
                    idx++;
                }
            } else {
                output.add(line);
            }
        }
        return output;
    }

    // ─── Print Helpers ───────────────────────────────────────────────────────
    static void printBlock(String title, List<String> lines) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60));
        lines.forEach(System.out::println);
    }

    static void printMNT() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MNT - Macro Name Table");
        System.out.println("=".repeat(60));
        System.out.printf("%-5s %-14s %-12s %-20s%n", "Idx", "MacroName", "MDT_Start", "Formal Params");
        System.out.println("-".repeat(55));
        for (int i = 0; i < MNT.size(); i++) {
            MNTEntry e = MNT.get(i);
            System.out.printf("%-5d %-14s %-12d %-20s%n",
                i + 1, e.name, e.mdtStart,
                e.params.isEmpty() ? "-" : String.join(", ", e.params));
        }
    }

    static void printMDT() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MDT - Macro Definition Table (with Early Expansion)");
        System.out.println("=".repeat(60));
        for (int i = 0; i < MDT.size(); i++)
            System.out.printf("%-4d %s%n", i + 1, MDT.get(i));
    }

    static void printALA() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ALA - Argument List Array (Formal -> Positional)");
        System.out.println("=".repeat(60));
        for (MNTEntry e : MNT) {
            if (e.params.isEmpty()) {
                System.out.println("Macro: " + e.name + "  -> No parameters");
            } else {
                System.out.println("Macro: " + e.name);
                for (int j = 0; j < e.params.size(); j++)
                    System.out.printf("  %-8s -> #%d%n", e.params.get(j), j + 1);
            }
        }
    }

    // ─── Main ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("===== TWO-PASS MACRO PROCESSOR =====");

        List<String> src = Arrays.asList(SOURCE);
        printBlock("ORIGINAL SOURCE PROGRAM", src);

        List<String> intermediate = pass1();
        printBlock("PASS-1 OUTPUT: INTERMEDIATE CODE", intermediate);
        printMNT();
        printMDT();
        printALA();

        List<String> finalCode = pass2(intermediate);
        printBlock("PASS-2 OUTPUT: FINAL EXPANDED CODE", finalCode);
    }
}
