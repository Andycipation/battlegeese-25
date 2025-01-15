import os

# key: name of parameter
# param 0: current value
# param 1: lower bound (inclusive)
# param 2: upper bound (inclusive)
hyperparameters = {
    "lateGameCountThres" : (600, 0, 2000),
    "paintToMoneyEarly" : (50, 0, 100),
    "paintToMoneyLate" : (75, 0, 100),
    "timeRuin" : (15, 0, 25),
    "timeTravel" : (5, 0, 25),
    "noSpawnThresEarly" : (1250, 1000, 1500),
    "noSpawnThresLate" : (5000, 1000, 10000),
    "lateGameChipThres" : (5000, 2000, 20000),
}

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
