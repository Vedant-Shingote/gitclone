import java.util.*;

/**
 * Assignment 1: Two-Pass Assembler
 * Generates: Symbol Table, Literal Table, Pool Table, Intermediate Code
 *
 * HOW TO COMPILE AND RUN:
 * ========================
 * Step 1: Open terminal / command prompt in the folder containing this file.
 * Step 2: Compile:
 *           javac Assign1TwoPassAssembler.java
 * Step 3: Run:
 *           java Assign1TwoPassAssembler
 * Step 4: The program uses a hardcoded sample assembly program (see SOURCE array).
 *         Output will be printed directly to the console.
 * ========================
 */
public class Assign1TwoPassAssembler {

    // -------- Mnemonic Opcode Table (MOT) --------
    static final Map<String, int[]> MOT = new LinkedHashMap<>();
    // int[0] = class (0=AD,1=DL,2=IS), int[1] = opcode
    static {
        MOT.put("START",  new int[]{0, 1});
        MOT.put("END",    new int[]{0, 2});
        MOT.put("ORIGIN", new int[]{0, 3});
        MOT.put("LTORG",  new int[]{0, 4});
        MOT.put("EQU",    new int[]{0, 5});
        MOT.put("DC",     new int[]{1, 1});
        MOT.put("DS",     new int[]{1, 2});
        MOT.put("LOAD",   new int[]{2, 1});
        MOT.put("ADD",    new int[]{2, 2});
        MOT.put("MULT",   new int[]{2, 3});
        MOT.put("STORE",  new int[]{2, 4});
        MOT.put("SUB",    new int[]{2, 5});
    }

    static final String[] CLASS_NAMES = {"AD", "DL", "IS"};

    // -------- Data Structures --------
    static List<String[]>  SYMTAB  = new ArrayList<>(); // {symbol, address}
    static List<String[]>  LITTAB  = new ArrayList<>(); // {literal, address}
    static List<Integer>   POOLTAB = new ArrayList<>(List.of(0));
    static List<String>    IC      = new ArrayList<>();
    static List<String>    ERRORS  = new ArrayList<>();

    // -------- Sample Assembly Source Program --------
    static final String[] SOURCE = {
        "START 100",
        "A      DC 01",
        "LOAD   A",
        "LOAD   C",
        "ORIGIN 500",
        "ADD    ='5'",
        "ORIGIN A+4",
        "MULT   ='10'",
        "ADD    L",
        "ORIGIN 900",
        "LTORG",
        "L      ADD ='5'",
        "ORIGIN A+10",
        "ADD    B",
        "B      DS 4",
        "C      EQU B",
        "A1     DS 7",
        "END"
    };

    // -------- Helper: find symbol index --------
    static int symIdx(String s) {
        for (int i = 0; i < SYMTAB.size(); i++)
            if (SYMTAB.get(i)[0].equals(s)) return i;
        return -1;
    }

    static void addSym(String s, int addr) {
        int i = symIdx(s);
        if (i == -1) SYMTAB.add(new String[]{s, addr == -1 ? "-1" : String.valueOf(addr)});
        else if (SYMTAB.get(i)[1].equals("-1") && addr != -1)
            SYMTAB.get(i)[1] = String.valueOf(addr);
    }

    static int symAddr(String s) {
        int i = symIdx(s);
        if (i == -1) return -1;
        return Integer.parseInt(SYMTAB.get(i)[1]);
    }

    // -------- Helper: add literal --------
    static int addLit(String l) {
        for (int i = 0; i < LITTAB.size(); i++)
            if (LITTAB.get(i)[0].equals(l) && LITTAB.get(i)[1].equals("-1")) return i;
        LITTAB.add(new String[]{l, "-1"});
        return LITTAB.size() - 1;
    }

    // -------- Helper: evaluate expression like A+4, B-1, 500 --------
    static int evalExpr(String e) {
        if (e.matches("\\d+")) return Integer.parseInt(e);
        if (e.contains("+")) {
            String[] p = e.split("\\+");
            int base = symAddr(p[0].trim());
            return base == -1 ? -1 : base + Integer.parseInt(p[1].trim());
        }
        if (e.contains("-")) {
            String[] p = e.split("-");
            int base = symAddr(p[0].trim());
            return base == -1 ? -1 : base - Integer.parseInt(p[1].trim());
        }
        return symAddr(e.trim());
    }

    // -------- Assign literals from current pool at LTORG/END --------
    static int assignLiterals(int LC) {
        int start = POOLTAB.get(POOLTAB.size() - 1);
        for (int i = start; i < LITTAB.size(); i++) {
            if (LITTAB.get(i)[1].equals("-1")) {
                LITTAB.get(i)[1] = String.valueOf(LC);
                String val = LITTAB.get(i)[0].replaceAll("='(.*)'", "$1");
                IC.add(String.format("%03d  (DL,01)  (C,%s)", LC, val));
                LC++;
            }
        }
        POOLTAB.add(LITTAB.size());
        return LC;
    }

    // -------- PASS 1 --------
    static void pass1() {
        int LC = 0;
        IC.add("INTERMEDIATE CODE (PASS 1)");
        IC.add("-".repeat(40));

        for (int ln = 0; ln < SOURCE.length; ln++) {
            String raw = SOURCE[ln];
            String line = raw.split(";")[0].trim();
            if (line.isEmpty()) continue;

            String[] tok = line.split("\\s+");
            String label = "-", mnem, op = "-";

            if (tok.length == 1) {
                mnem = tok[0];
            } else if (tok.length == 2) {
                // Could be "LABEL MNEM" or "MNEM OP"
                if (MOT.containsKey(tok[0].toUpperCase())) {
                    mnem = tok[0]; op = tok[1];
                } else {
                    label = tok[0]; mnem = tok[1];
                }
            } else {
                label = tok[0]; mnem = tok[1]; op = tok[2];
            }
            mnem = mnem.toUpperCase();

            // Process label
            if (!label.equals("-")) {
                if (symIdx(label) == -1) addSym(label, LC);
                else if (!SYMTAB.get(symIdx(label))[1].equals("-1"))
                    ERRORS.add("Line " + (ln+1) + ": Duplicate symbol " + label);
                else SYMTAB.get(symIdx(label))[1] = String.valueOf(LC);
            }

            switch (mnem) {
                case "START":
                    LC = Integer.parseInt(op);
                    IC.add(String.format("     (AD,01)  (C,%d)", LC));
                    break;
                case "DC":
                    IC.add(String.format("%03d  (DL,01)  (C,%s)", LC, op));
                    LC++;
                    break;
                case "DS":
                    IC.add(String.format("%03d  (DL,02)  (C,%s)", LC, op));
                    LC += Integer.parseInt(op);
                    break;
                case "ORIGIN":
                    IC.add(String.format("%03d  (AD,03)  (S,%s)", LC, op));
                    int nv = evalExpr(op);
                    if (nv == -1) ERRORS.add("Line " + (ln+1) + ": Undefined symbol in ORIGIN");
                    else LC = nv;
                    break;
                case "EQU":
                    IC.add(String.format("%03d  (AD,05)  (S,%s)  (S,%s)", LC, label, op));
                    int ev = evalExpr(op);
                    if (ev == -1) ERRORS.add("Line " + (ln+1) + ": Undefined symbol in EQU");
                    else addSym(label, ev);
                    break;
                case "LTORG":
                    IC.add(String.format("%03d  (AD,04)", LC));
                    LC = assignLiterals(LC);
                    break;
                case "END":
                    LC = assignLiterals(LC);
                    IC.add(String.format("%03d  (AD,02)", LC));
                    return;
                default:
                    if (!MOT.containsKey(mnem)) {
                        ERRORS.add("Line " + (ln+1) + ": Invalid mnemonic " + mnem);
                    } else {
                        int[] entry = MOT.get(mnem);
                        String cls = CLASS_NAMES[entry[0]];
                        int opc = entry[1];
                        if (op.startsWith("='")) {
                            int li = addLit(op);
                            IC.add(String.format("%03d  (%s,%02d)  (L,%d)", LC, cls, opc, li + 1));
                        } else {
                            addSym(op, -1);
                            IC.add(String.format("%03d  (%s,%02d)  (S,%s)", LC, cls, opc, op));
                        }
                        LC++;
                    }
            }
        }
    }

    // -------- PASS 2: resolve symbols in IC (optional display) --------
    static void pass2() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("PASS 2: RESOLVED MACHINE CODE (partial)");
        System.out.println("=".repeat(50));
        System.out.printf("%-6s %-8s %-8s%n", "Addr", "OpCode", "Operand");
        System.out.println("-".repeat(30));

        for (String ic : IC) {
            if (ic.startsWith("INT") || ic.startsWith("-")) continue;
            String addr = ic.length() >= 3 ? ic.substring(0, 3).trim() : "";
            if (addr.isEmpty() || !addr.matches("\\d+")) continue;

            // Manual symbol resolution: replace (S,SYMBOL) with (S,ADDRESS)
            String line = ic;
            int sIdx = line.indexOf("(S,");
            if (sIdx != -1) {
                int end = line.indexOf(")", sIdx);
                String sym = line.substring(sIdx + 3, end);
                int sa = symAddr(sym);
                if (sa != -1)
                    line = line.substring(0, sIdx) + "(S," + sa + ")" + line.substring(end + 1);
            }
            System.out.println(line);
        }
    }

    // -------- Print Tables --------
    static void printAll() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("MNEMONIC OPCODE TABLE (MOT)");
        System.out.println("=".repeat(50));
        System.out.printf("%-10s %-6s %-6s%n", "Mnemonic", "Class", "Opcode");
        System.out.println("-".repeat(25));
        for (Map.Entry<String, int[]> e : MOT.entrySet())
            System.out.printf("%-10s %-6s %02d%n", e.getKey(),
                CLASS_NAMES[e.getValue()[0]], e.getValue()[1]);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("INTERMEDIATE CODE (PASS 1)");
        System.out.println("=".repeat(50));
        for (String s : IC) System.out.println(s);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("SYMBOL TABLE");
        System.out.println("=".repeat(50));
        System.out.printf("%-5s %-12s %-10s%n", "Idx", "Symbol", "Address");
        System.out.println("-".repeat(25));
        for (int i = 0; i < SYMTAB.size(); i++)
            System.out.printf("%-5d %-12s %-10s%n", i + 1,
                SYMTAB.get(i)[0], SYMTAB.get(i)[1]);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("LITERAL TABLE");
        System.out.println("=".repeat(50));
        System.out.printf("%-5s %-12s %-10s%n", "Idx", "Literal", "Address");
        System.out.println("-".repeat(25));
        for (int i = 0; i < LITTAB.size(); i++)
            System.out.printf("%-5d %-12s %-10s%n", i + 1,
                LITTAB.get(i)[0], LITTAB.get(i)[1]);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("POOL TABLE");
        System.out.println("=".repeat(50));
        System.out.printf("%-6s %-12s%n", "Pool#", "LitTab Index");
        System.out.println("-".repeat(20));
        for (int i = 0; i < POOLTAB.size(); i++)
            System.out.printf("%-6d %-12d%n", i + 1, POOLTAB.get(i));

        System.out.println("\n" + "=".repeat(50));
        System.out.println("ERRORS");
        System.out.println("=".repeat(50));
        if (ERRORS.isEmpty()) System.out.println("No errors found.");
        else ERRORS.forEach(System.out::println);
    }

    public static void main(String[] args) {
        System.out.println("===== TWO-PASS ASSEMBLER =====");
        System.out.println("Assembly Source Program:");
        System.out.println("-".repeat(40));
        for (String s : SOURCE) System.out.println(s);
        System.out.println("-".repeat(40));

        pass1();
        printAll();
        pass2();
    }
}
