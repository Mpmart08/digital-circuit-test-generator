import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class Simulator {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: java Simulator <file.txt> <input_vector>");
            System.exit(0);
        }

        File netListFile = new File(args[0]);
        if (!netListFile.exists()) {
            System.out.println("Error: Could not find the file");
            System.exit(0);
        }

        String inputVector = args[1];

        try {

            Simulator simulator = new Simulator(netListFile);
            simulator.initialize(inputVector);
            simulator.run();
            simulator.printOutputs();

        } catch (Exception ex) {

            System.out.println("Error: Unexpected input format");
            System.exit(0);
        }
    }

    private class Gate {

        private String function;
        private int[] inputs;
        private int output;

        public Gate(String function, int[] inputs, int output) {
            this.function = function;
            this.inputs = inputs;
            this.output = output;
        }

        public String getFunction() {
            return function;
        }

        public int[] getInputs() {
            return inputs;
        }

        public int getOutput() {
            return output;
        }
    }

    private List<Gate> gates = new LinkedList<>();
    private int[] nets;
    private int[] inputs;
    private int[] outputs;

    public Simulator(File netListFile) throws Exception {

        this(netListFile, "");
    }

    public Simulator(File netListFile, String inputVector) throws Exception {

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

        nets = new int[numberOfNets];
        initialize(inputVector);
    }

    public void initialize(String inputVector) {

        Arrays.fill(nets, -1);
        for (int i = 0; i < inputVector.length(); i++) {
            nets[inputs[i]] = Character.getNumericValue(inputVector.charAt(i));
        }
    }

    public void run() {

        while (!gates.isEmpty()) {
            ListIterator<Gate> iterator = gates.listIterator();
            while (iterator.hasNext()) {
                Gate gate = iterator.next();
                if (isGateReady(gate)) {
                    evaluateFunction(gate);
                    iterator.remove();
                }
            }
        }
    }

    public void printOutputs() {

        for (int i = 0; i < outputs.length; i++) {
            System.out.print(nets[outputs[i]]);
        }
        System.out.println();
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
                gates.add(new Gate(function, in, out));
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
        for (int input : gate.getInputs()) {
            if (nets[input] == -1) {
                return false;
            }
        }
        return true;
    }

    private void evaluateFunction(Gate gate) {
        int value = 0;
        int[] in = gate.getInputs();
        switch (gate.getFunction()) {
            case "INV":
                value = nets[in[0]] == 0 ? 1 : 0;
                break;
            case "BUF":
                value = nets[in[0]];
                break;
            case "AND":
                value = 1;
                for (int i : in) {
                    value = value & nets[i];
                }
                break;
            case "OR":
                value = 0;
                for (int i : in) {
                    value = value | nets[i];
                }
                break;
            case "NAND":
                value = 1;
                for (int i : in) {
                    value = value & nets[i];
                }
                value = value == 0 ? 1 : 0;
                break;
            case "NOR":
                value = 0;
                for (int i : in) {
                    value = value | nets[i];
                }
                value = value == 0 ? 1 : 0;
                break;
            case "XOR":
                value = 0;
                for (int i : in) {
                    value = value ^ nets[i];
                }
                break;
            case "XNOR":
                value = 0;
                for (int i : in) {
                    value = value ^ nets[i];
                }
                value = value == 0 ? 1 : 0;
                break;
            default:
                System.out.println("Error: Unsupported logic gate");
                System.exit(0);
                break;
        }
        nets[gate.getOutput()] = value;
    }
}