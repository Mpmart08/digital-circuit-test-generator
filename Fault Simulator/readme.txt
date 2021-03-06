1. Java must be installed on your machine to run the simulator
- If you have Windows, follow these steps: https://docs.oracle.com/javase/8/docs/technotes/guides/install/windows_jdk_install.html
- If you have Mac, follow these steps: https://docs.oracle.com/javase/8/docs/technotes/guides/install/mac_jdk.html

2. Open a terminal or command prompt and navigate to the directory that has the simulator source files

3. Run the following command to compile the program:
	javac *.java

4. Run the following command to run the program with the s27 circuit and test vector 1110101:
	java FaultSimulator input_files/s27.txt 1110101

5. The detected faults should have printed to the console

6. In general, the usage of the program is:
	java FaultSimulator <path>/<netlist_file> <test_vector>
   or:
        java FaultSimulator <path>/<netlist_file> <test_vector> <path>/<faultlist_file>

7. To dump the output to a file rather than the console:
	java FaultSimulator input_files/s27.txt 1110101 > output_files/s27_1110101.txt

8. To dump the output to a file in general:
	java FaultSimulator <path>/<netlist_file> <input_vector> > <path>/<output_file>

9. To run the program for part b:
	java PartB input_files/s27.txt

10. To run the program for part b in general:
	java PartB <path>/<netlist_file>