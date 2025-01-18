import os

# key: name of parameter
# param 0: current value
# param 1: lower bound (inclusive)
# param 2: upper bound (inclusive)
hyperparameters = {'lateGameCountThres': (203, 0, 600), 'paintToMoneyEarly': (23, 20, 80), 'paintToMoneyLate': (81, 30, 90), 'timeRuin': (20, 5, 25), 'timeTravel': (4, 2, 12), 'noSpawnThresEarly': (1318, 1000, 1500), 'noSpawnThresLate': (2549, 1250, 7000), 'lateGameChipThres': (8319, 3000, 10000)}

f = open("Hyperparameter.java", "w")
# Get the name of the current directory
current_directory = os.path.basename(os.getcwd())
f.write(f"package {current_directory};\n")
f.write("public final class Hyperparameter {\n")
for parameter in hyperparameters:
    curr, l, r = hyperparameters[parameter]
    f.write(f"    // current value: {curr}    range: [{l}, {r}]\n")
    f.write(f"    public static final int {parameter} = {curr};\n\n")
f.write("    private Hyperparameter() {}\n")
f.write("}\n")
f.close()
