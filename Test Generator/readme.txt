1. Java must be installed on your machine to run the simulator
- If you have Windows, follow these steps: https://docs.oracle.com/javase/8/docs/technotes/guides/install/windows_jdk_install.html
- If you have Mac, follow these steps: https://docs.oracle.com/javase/8/docs/technotes/guides/install/mac_jdk.html

2. Open a terminal or command prompt and navigate to the directory that has the simulator source files

3. Run the following command to compile the program:
	javac *.java

4. Run the following command to run the program with the s27 circuit and fault 12 s-a-0:
	java TestGenerator input_files/s27.txt 12 0

5. If the fault is detectable, a test vector will be printed to the screen.

6. In general, the usage of the program is:
	java TestGenerator <path>/<netlist_file> <net> <stuck-at-value>

7. To dump the output to a file rather than the console:
	java TestGenerator input_files/s27.txt 12 0 > output_files/s27_12_0.txt

8. To dump the output to a file in general:
	java TestGenerator <path>/<netlist_file> <net> <stuck-at-value> > <path>/<output_file>