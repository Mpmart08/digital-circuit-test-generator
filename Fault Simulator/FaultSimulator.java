import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Collections;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class FaultSimulator {

    public static void main(String[] args) {

        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java FaultSimulator <circuit_file.txt> <fault_file.txt> <input_vector>\n"
                             + "       java FaultSimulator <circuit_file.txt> <input_vector>");
            System.exit(0);
        }

        File netListFile = new File(args[0]);
        if (!netListFile.exists()) {
            System.out.println("Error: Could not find the net list file");
            System.exit(0);
        }

        File faultListFile = new File(args[args.length - 2]);
        if (!faultListFile.exists()) {
            System.out.println("Error: Could not find the fault list file");
            System.exit(0);
        }

        String inputVector = args[args.length - 1];

        try {

            FaultSimulator simulator = (args.length == 2)
                ? new FaultSimulator(netListFile)
                : new FaultSimulator(netListFile, faultListFile);
            simulator.initialize(inputVector);
            simulator.run();
            simulator.printDetectedFaults();

        } catch (Exception ex) {

            System.out.println("Error: Unexpected input format");
            System.exit(0);
        }
    }

    private class Gate {
        private String function;
        private int[] inputs;
        private int output;
    }

    private class Net {
        private List<Fault> faults;
        private int value;
    }

    public class Fault {
        private int net;
        private int value;

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Fault)) return false;
            Fault that = (Fault) other;
            return (this.net == that.net) && (this.value == that.value);
        }
    }

    private List<Fault> faults = new LinkedList<>();
    private List<Gate> gates = new LinkedList<>();
    private List<Gate> removed = new LinkedList<>();
    private Net[] nets;
    private int[] inputs;
    private int[] outputs;

    public FaultSimulator(File netListFile) throws Exception {

        parseNetListFile(netListFile);
        createFaultList();
    }

    public FaultSimulator(File netListFile, File faultListFile) throws Exception {

        parseNetListFile(netListFile);
        parseFaultListFile(faultListFile);
    }

    public void initialize(String inputVector) {

        if (gates.isEmpty()) {
            List<Gate> temp = gates;
            gates = removed;
            removed = temp;
        }

        for (int i = 0; i < nets.length; i++) {
            Net net = new Net();
            net.faults = new LinkedList<>();
            for (Fault fault : faults) {
                if (fault.net == i) {
                    net.faults.add(fault);
                }
            }
            net.value = -1;
            nets[i] = net;
        }
        for (int i = 0; i < inputVector.length(); i++) {
            int value = Character.getNumericValue(inputVector.charAt(i)) & 1;
            Net net = nets[inputs[i]];
            net.value = value;
            ListIterator<Fault> iterator = net.faults.listIterator();
            while (iterator.hasNext()) {
                Fault fault = iterator.next();
                if (fault.value == value) {
                    iterator.remove();
                }
            }
        }
    }

    public void run() {

        while (!gates.isEmpty()) {
            ListIterator<Gate> iterator = gates.listIterator();
            while (iterator.hasNext()) {
                Gate gate = iterator.next();
                if (isGateReady(gate)) {
                    evaluateGate(gate);
                    removed.add(gate);
                    iterator.remove();
                }
            }
        }
    }

    public void printDetectedFaults() {

        List<Fault> detected = getDetectedFaults();
        for (Fault fault : detected) {
            System.out.println("net\t" + (fault.net + 1) + "\ts-a-" + fault.value);
        }
        System.out.println();
        System.out.println("Total faults detected: " + detected.size());
    }

    public List<Fault> getFaultList() {
        return faults;
    }

    public List<Fault> getDetectedFaults() {
        List<Fault> detected = new LinkedList<>();
        for (int i = 0; i < outputs.length; i++) {
            Net net = nets[outputs[i]];
            for (Fault fault : net.faults) {
                if (!detected.contains(fault)) {
                    detected.add(fault);
                }
            }
        }
        Collections.sort(detected, (x, y) -> Integer.compare(x.net, y.net));
        return detected;
    }

    public int getNumberOfInputs() {
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

        nets = new Net[numberOfNets];
    }

    private void createFaultList() {

        for (int i = 0; i < nets.length; i++) {
            Fault sa0 = new Fault();
            Fault sa1 = new Fault();
            sa0.net = i;
            sa0.value = 0;
            sa1.net = i;
            sa1.value = 1;
            faults.add(sa0);
            faults.add(sa1);
        }
    }

    private void parseFaultListFile(File faultListFile) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(faultListFile));
        String line = reader.readLine();
        while (line != null) {
            String[] tokens = line.split("\\s+");
            if (tokens.length == 2) {
                Fault fault = new Fault();
                fault.net = Integer.parseInt(tokens[0]) - 1;
                fault.value = Integer.parseInt(tokens[1]);
                faults.add(fault);
            }
            line = reader.readLine();
        }
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
        for (int input : gate.inputs) {
            if (nets[input].value == -1) {
                return false;
            }
        }
        return true;
    }

    private void evaluateGate(Gate gate) {
        int value = 0;
        int[] in = gate.inputs;
        List<Fault> faults = null;
        switch (gate.function) {
            case "INV":
                value = nets[in[0]].value == 0 ? 1 : 0;
                faults = faultListInv(gate);
                break;
            case "BUF":
                value = nets[in[0]].value;
                faults = faultListInv(gate);
                break;
            case "AND":
                value = 1;
                for (int i : in) {
                    value = value & nets[i].value;
                }
                faults = faultList(gate, 0);
                break;
            case "OR":
                value = 0;
                for (int i : in) {
                    value = value | nets[i].value;
                }
                faults = faultList(gate, 1);
                break;
            case "NAND":
                value = 1;
                for (int i : in) {
                    value = value & nets[i].value;
                }
                value = value == 0 ? 1 : 0;
                faults = faultList(gate, 0);
                break;
            case "NOR":
                value = 0;
                for (int i : in) {
                    value = value | nets[i].value;
                }
                value = value == 0 ? 1 : 0;
                faults = faultList(gate, 1);
                break;
            default:
                System.out.println("Error: Unsupported logic gate");
                System.exit(0);
                break;
        }
        Net net = nets[gate.output];
        net.value = value;
        ListIterator<Fault> iterator = faults.listIterator();
        while (iterator.hasNext()) {
            Fault fault = iterator.next();
            if ((fault.net == gate.output) && (fault.value == value)) {
                iterator.remove();
            }
        }
        net.faults = faults;
    }

    List<Fault> faultListInv(Gate gate) {

        List<Fault> faults = new LinkedList<>();
        faults.addAll(nets[gate.inputs[0]].faults);
        faults.addAll(nets[gate.output].faults);
        return faults;
    }

    List<Fault> faultList(Gate gate, int ctrl) {

        List<Fault> faults = new LinkedList<>();
        List<Net> controllingValues = new LinkedList<>();
        List<Net> noncontrollingValues = new LinkedList<>();
        for (int i : gate.inputs) {
            Net net = nets[i];
            if (net.value == ctrl) {
                controllingValues.add(net);
            } else {
                noncontrollingValues.add(net);
            }
        }
        if (controllingValues.size() == 0) {
            for (Net net : noncontrollingValues) {
                faults.addAll(net.faults);
            }
        } else if (controllingValues.size() == 1) {
            faults.addAll(controllingValues.get(0).faults);
            for (Net net : noncontrollingValues) {
                for (Fault fault : net.faults) {
                    if (faults.contains(fault)) {
                        faults.remove(fault);
                    }
                }
            }
        }
        faults.addAll(nets[gate.output].faults);
        return faults;
    }
}