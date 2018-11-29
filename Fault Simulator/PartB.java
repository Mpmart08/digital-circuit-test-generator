import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Iterator;
import java.io.File;

public class PartB {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java PartB <circuit_file.txt>");
            System.exit(0);
        }

        File netListFile = new File(args[0]);
        if (!netListFile.exists()) {
            System.out.println("Error: Could not find the net list file");
            System.exit(0);
        }

        try {

            FaultSimulator simulator = new FaultSimulator(netListFile);
            List<FaultSimulator.Fault> faults = new LinkedList<>(simulator.getFaultList());
            double total = faults.size();
            double coverage = 0.0;
            Random rand = new Random();
            int bound = 1 << (simulator.getNumberOfInputs());
            Iterator<Integer> iterator = rand.ints(0, bound).distinct().iterator();
            while(iterator.hasNext()) {
                int i = iterator.next();
                String inputVector = Integer.toBinaryString(i);
                StringBuilder builder = new StringBuilder(inputVector);
                while (builder.length() < simulator.getNumberOfInputs()) {
                    builder.insert(0, '0');
                }
                simulator.initialize(builder.toString());
                simulator.run();
                List<FaultSimulator.Fault> detected = simulator.getDetectedFaults();
                for (FaultSimulator.Fault fault : detected) {
                    if (faults.contains(fault)) {
                        faults.remove(fault);
                    }
                }
                coverage = 1.0 - (faults.size() / total);
                System.out.println(builder.toString() + ",\t" + coverage);
                if (coverage > 0.995) {
                    break;
                }
            }

        } catch (Exception ex) {

            System.out.println("Error: Unexpected input format");
            System.exit(0);
        }
    }
}