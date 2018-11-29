import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Collections;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class TestGenerator {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Usage: java TestGenerator <circuit_file.txt> <fault_line> <stuck_at_value>");
            System.exit(0);
        }

        File netListFile = new File(args[0]);
        if (!netListFile.exists()) {
            System.out.println("Error: Could not find the net list file");
            System.exit(0);
        }

        int faultLine = Integer.parseInt(args[1]) - 1;
        Logic stuckAtValue = Integer.parseInt(args[2]) == 0 ? Logic.ZERO : Logic.ONE;

        try {

            TestGenerator generator = new TestGenerator(netListFile, faultLine, stuckAtValue);
            generator.run();
            generator.printTestVector();

        } catch (Exception ex) {

            System.out.println("Error: Unexpected input format");
            System.exit(0);
        }
    }

    private enum Logic {
        ZERO,
        ONE,
        X,
        D,
        DBAR
    }

    private class Gate {
        private String function;
        private int[] inputs;
        private int output;
    }

    private class NetValuePair {
        private int net;
        private Logic value;
    }

    private List<Gate> gates = new LinkedList<>();
    private List<Gate> dFrontier = new LinkedList<>();
    private Logic[] nets;
    private int[] inputs;
    private int[] outputs;
    private boolean success;
    private int faultLine;
    private Logic stuckAtValue;

    public TestGenerator(File netListFile, int faultLine, Logic stuckAtValue) throws Exception {

        parseNetListFile(netListFile);
        Arrays.fill(nets, Logic.X);
        this.faultLine = faultLine;
        this.stuckAtValue = stuckAtValue;
        success = false;
    }

    public void printTestVector() {

        if (success) {
            for (int input : inputs) {
                char c;
                switch (nets[input]) {
                    case DBAR:
                    case ZERO:  c = '0';    break;
                    case D:
                    case ONE:   c = '1';    break;
                    default:    c = 'X';    break;
                }
                System.out.print(c);
            }
            System.out.println();
        } else {
            System.out.println("Fault Undetectable");
        }
    }

    public void run() {
        success = podem();
    }

    private void printNets() {
        System.out.println();
        for (int i = 0; i < nets.length; i++) {
            System.out.println((i + 1) + ": " + nets[i]);
        }
        System.out.println();
    }

    private boolean podem() {
        for (int output : outputs) {
            if ((nets[output] == Logic.D || nets[output] == Logic.DBAR) && pathToInput(output) && circuitConsistent()) {
                return true;
            }
        }
        if (!xPathCheck(faultLine)) {
            //System.out.println("xPathCheck failed");
            return false;
        }
        NetValuePair objective = objective();
        // System.out.println((objective.net + 1) + ", " + objective.value);
        NetValuePair assignment = backtrace(objective);
        // System.out.println((assignment.net + 1) + ", " + assignment.value);
        Logic[] backup = Arrays.copyOf(nets, nets.length);
        imply(assignment);
        // printNets();
        if (podem()) {
            return true;
        }
        nets = backup;
        assignment.value = assignment.value == Logic.ZERO ? Logic.ONE : Logic.ZERO;
        // System.out.println((assignment.net + 1) + ", " + assignment.value);
        imply(assignment);
        // printNets();
        if (podem()) {
            return true;
        }
        nets = backup;
        assignment.value = Logic.X;
        imply(assignment);
        return false;
    }

    private boolean xPathCheck(int net) {
        boolean isOutput = IntStream.of(outputs).anyMatch(x -> x == net);
        boolean isOnXPath = nets[net] == Logic.X || nets[net] == Logic.D || nets[net] == Logic.DBAR;
        if (isOnXPath) {
            if (isOutput) {
                return true;
            } else {
                for (Gate gate : gates) {
                    if (IntStream.of(gate.inputs).anyMatch(x -> x == net)) {
                        if (xPathCheck(gate.output)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private NetValuePair objective() {
        NetValuePair objective = new NetValuePair();
        if (dFrontier.isEmpty()) {
            nets[faultLine] = stuckAtValue == Logic.ZERO ? Logic.D : Logic.DBAR;
            objective.net = faultLine;
            objective.value = stuckAtValue == Logic.ZERO ? Logic.ONE : Logic.ZERO;
            return objective;
        }
        Gate gate = dFrontier.get(0);
        int net = IntStream.of(gate.inputs).filter(x -> nets[x] == Logic.X).findFirst().getAsInt();
        Logic value = null;
        switch (gate.function) {
            case "AND":
            case "NAND":
                value = Logic.ONE;
                break;
            case "OR":
            case "NOR":
                value = Logic.ZERO;
                break;
            default:
                value = Logic.X;
                break;
        }
        objective.net = net;
        objective.value = value;
        return objective;
    }

    private NetValuePair backtrace(NetValuePair objective) {
        NetValuePair assignment = new NetValuePair();
        if (IntStream.of(inputs).anyMatch(x -> x == objective.net)) {
            assignment.net = objective.net;
            assignment.value = objective.value;
            return assignment;
        }
        Gate gate = gates.stream().filter(x -> x.output == objective.net).findFirst().get();
        boolean value = objective.value == Logic.ONE || objective.value == Logic.D;
        boolean parity = gate.function.equals("INV") || gate.function.equals("NAND") || gate.function.equals("NOR");
        for (int input : gate.inputs) {
            if (nets[input] == Logic.X) {
                NetValuePair next = new NetValuePair();
                next.net = input;
                next.value = value ^ parity ? Logic.ONE : Logic.ZERO;
                assignment = backtrace(next);
                if (assignment != null) {
                    return assignment;
                }
            }
        }
        return null;
    }

    private void imply(NetValuePair assignment) {
        if (assignment.net == faultLine) {
            nets[assignment.net] = assignment.value == Logic.ZERO ? Logic.DBAR : Logic.D;
        } else {
            nets[assignment.net] = assignment.value;
        }
        List<Gate> list = new LinkedList<>(gates);
        while (!list.isEmpty()) {
            ListIterator<Gate> iterator = list.listIterator();
            int length = list.size();
            while (iterator.hasNext()) {
                Gate gate = iterator.next();
                if (isGateReady(gate)) {
                    evaluateGate(gate);
                    iterator.remove();
                }
            }
            if (length == list.size()) {
                break;
            }
        }
        dFrontier.clear();
        for (Gate gate : gates) {
            if (nets[gate.output] == Logic.X) {
                for (int input : gate.inputs) {
                    if (nets[input] == Logic.D || nets[input] == Logic.DBAR) {
                        dFrontier.add(gate);
                        break;
                    }
                }
            }
        }
    }

    private boolean pathToInput(int net) {
        boolean isInput = IntStream.of(inputs).anyMatch(x -> x == net);
        boolean isOnPath = nets[net] != Logic.X;
        if (isOnPath) {
            if (isInput) {
                return true;
            } else {
                Gate gate = gates.stream().filter(x -> x.output == net).findFirst().get();
                for (int input : gate.inputs) {
                    if (pathToInput(input)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean circuitConsistent() {
        for (Gate gate : gates) {
            Logic value = Logic.X;
            int[] in = gate.inputs;
            if (isGateReady(gate)) {
                switch (gate.function) {
                    case "INV":
                        switch (nets[in[0]]) {
                            case ZERO:  value = Logic.ONE;    break;
                            case ONE:   value = Logic.ZERO;   break;
                            case D:     value = Logic.DBAR;   break;
                            case DBAR:  value = Logic.D;      break;
                            default:    break;
                        }
                        break;
                    case "BUF":
                        value = nets[in[0]];
                        break;
                    case "AND":
                        value = Logic.ONE;
                        for (int i : in) {
                            if (nets[i] == Logic.D) {
                                value = Logic.D;
                            } else if (nets[i] == Logic.DBAR) {
                                value = Logic.DBAR;
                            } else if (nets[i] == Logic.X || nets[i] == Logic.ZERO) {
                                value = Logic.ZERO;
                                break;
                            }
                        }
                        break;
                    case "OR":
                        value = Logic.ZERO;
                        for (int i : in) {
                            if (nets[i] == Logic.D) {
                                value = Logic.D;
                            } else if (nets[i] == Logic.DBAR) {
                                value = Logic.DBAR;
                            } else if (nets[i] == Logic.X || nets[i] == Logic.ONE) {
                                value = Logic.ONE;
                                break;
                            }
                        }
                        break;
                    case "NAND":
                        value = Logic.ZERO;
                        for (int i : in) {
                            if (nets[i] == Logic.D) {
                                value = Logic.DBAR;
                            } else if (nets[i] == Logic.DBAR) {
                                value = Logic.D;
                            } else if (nets[i] == Logic.X || nets[i] == Logic.ZERO) {
                                value = Logic.ONE;
                                break;
                            }
                        }
                        break;
                    case "NOR":
                        value = Logic.ONE;
                        for (int i : in) {
                            if (nets[i] == Logic.D) {
                                value = Logic.DBAR;
                            } else if (nets[i] == Logic.DBAR) {
                                value = Logic.D;
                            } else if (nets[i] == Logic.X || nets[i] == Logic.ONE) {
                                value = Logic.ZERO;
                                break;
                            }
                        }
                        break;
                    default:
                        System.out.println("Error: Unsupported logic gate");
                        System.exit(0);
                        break;
                }
                if (gate.output == faultLine) {
                    if (value == Logic.ZERO && stuckAtValue == Logic.ONE) {
                        value = Logic.DBAR;
                    } else if (value == Logic.ONE && stuckAtValue == Logic.ZERO) {
                        value = Logic.D;
                    }
                }
            }
            if (value != nets[gate.output]) {
                return false;
            }
        }
        return true;
    }

    private int getNumberOfInputs() {

        return inputs.length;
    }

    private void parseNetListFile(File netListFile) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(netListFile));
        String line = reader.readLine();
        int numberOfNets = 0;

        while (line != null) {
            int net = parseLine(line);
            if (net > numberOfNets) {
                numberOfNets = net;
            }
            line = reader.readLine();
        }

        nets = new Logic[numberOfNets];
    }

    private int parseLine(String line) {

        int net = 0;
        String[] tokens = line.split("\\s+");
        if (tokens.length > 2) {
            String function = parseFunction(tokens);
            int[] in = parseInputs(tokens);
            int out = parseOutput(tokens);
            initializeGate(tokens, function, in, out);
            net = getNumberOfNets(in, out);
        }
        return net;
    }

    private String parseFunction(String[] tokens) {

        return tokens[0];
    }

    private int[] parseInputs(String[] tokens) {

        int[] in = new int[tokens.length - 2];
        for (int i = 1; i < tokens.length - 1; i++) {
            int net = Integer.parseInt(tokens[i]);
            in[i - 1] = net - 1;
        }
        return in;
    }

    private int parseOutput(String[] tokens) {

        return Integer.parseInt(tokens[tokens.length - 1]) - 1;
    }

    private void initializeGate(String[] tokens, String function, int[] in, int out) {

        switch (function) {
            case "INPUT":
                inputs = new int[tokens.length - 2];
                for (int i = 1; i < tokens.length - 1; i++) {
                    inputs[i - 1] = Integer.parseInt(tokens[i]) - 1;
                }
                break;
            case "OUTPUT":
                outputs = new int[tokens.length - 2];
                for (int i = 1; i < tokens.length - 1; i++) {
                    outputs[i - 1] = Integer.parseInt(tokens[i]) - 1;
                }
                break;
            default:
                Gate gate = new Gate();
                gate.function = function;
                gate.inputs = in;
                gate.output = out;
                gates.add(gate);
                break;
        }
    }

    private int getNumberOfNets(int[] in, int out) {

        int numberOfNets = out;
        for (int i : in) {
            if (i > numberOfNets) {
                numberOfNets = i;
            }
        }
        return numberOfNets + 1;
    }

    private boolean isGateReady(Gate gate) {
        boolean anyUnknown = false;
        Logic control = null;
        switch (gate.function) {
            case "AND":
            case "NAND":
                control = Logic.ZERO;
                break;
            case "OR":
            case "NOR":
                control = Logic.ONE;
                break;
            default:
                break;
        }
        for (int input : gate.inputs) {
            if (nets[input] == control) {
                return true;
            } else if (nets[input] == Logic.X) {
                anyUnknown = true;
            }
        }
        return !anyUnknown;
    }

    private void evaluateGate(Gate gate) {
        Logic value = null;
        int[] in = gate.inputs;
        switch (gate.function) {
            case "INV":
                switch (nets[in[0]]) {
                    case ZERO:  value = Logic.ONE;    break;
                    case ONE:   value = Logic.ZERO;   break;
                    case D:     value = Logic.DBAR;   break;
                    case DBAR:  value = Logic.D;      break;
                    default:    break;
                }
                break;
            case "BUF":
                value = nets[in[0]];
                break;
            case "AND":
                value = Logic.ONE;
                for (int i : in) {
                    if (nets[i] == Logic.D) {
                        value = Logic.D;
                    } else if (nets[i] == Logic.DBAR) {
                        value = Logic.DBAR;
                    } else if (nets[i] == Logic.X || nets[i] == Logic.ZERO) {
                        value = Logic.ZERO;
                        break;
                    }
                }
                break;
            case "OR":
                value = Logic.ZERO;
                for (int i : in) {
                    if (nets[i] == Logic.D) {
                        value = Logic.D;
                    } else if (nets[i] == Logic.DBAR) {
                        value = Logic.DBAR;
                    } else if (nets[i] == Logic.X || nets[i] == Logic.ONE) {
                        value = Logic.ONE;
                        break;
                    }
                }
                break;
            case "NAND":
                value = Logic.ZERO;
                for (int i : in) {
                    if (nets[i] == Logic.D) {
                        value = Logic.DBAR;
                    } else if (nets[i] == Logic.DBAR) {
                        value = Logic.D;
                    } else if (nets[i] == Logic.X || nets[i] == Logic.ZERO) {
                        value = Logic.ONE;
                        break;
                    }
                }
                break;
            case "NOR":
                value = Logic.ONE;
                for (int i : in) {
                    if (nets[i] == Logic.D) {
                        value = Logic.DBAR;
                    } else if (nets[i] == Logic.DBAR) {
                        value = Logic.D;
                    } else if (nets[i] == Logic.X || nets[i] == Logic.ONE) {
                        value = Logic.ZERO;
                        break;
                    }
                }
                break;
            default:
                System.out.println("Error: Unsupported logic gate");
                System.exit(0);
                break;
        }
        if (gate.output == faultLine) {
            if (value == Logic.ZERO && stuckAtValue == Logic.ONE) {
                value = Logic.DBAR;
            } else if (value == Logic.ONE && stuckAtValue == Logic.ZERO) {
                value = Logic.D;
            }
        }
        nets[gate.output] = value;
    }
}