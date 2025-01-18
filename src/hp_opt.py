import sys
import os
from random import *
import importlib.util
import shutil
import subprocess
import time
from pprint import pprint


global params 
cnt = 0
def get_squids(num_squids):
    global cnt
    cur_squids = []
    for _ in range(num_squids):
        this_squid = dict()
        for param in params:
            curr, l, r = params[param]
            this_squid[param] = (randint(l, r), l, r)
        cur_squids.append([this_squid, cnt])
        cnt += 1
    return cur_squids


def change_params(dest, squid):
    file_path = f"./{dest}/hyperparameter.py"
    with open(file_path, "r") as file:
        lines = file.readlines()

    start, end = None, None
    for i, line in enumerate(lines):
        if "{" in line and start is None:
            start = i
        if "}" in line and start is not None:
            end = i
            break

    if start is not None and end is not None:
        replacement = f"hyperparameters = {squid}\n"
        lines = lines[:start] + [replacement] + lines[end+1:]

    with open(file_path, "w") as file:
        file.writelines(lines)

def play_game(squid_a, squid_b):
    # Step 1: Get the parent directory
    current_directory = os.getcwd()
    parent_directory = os.path.dirname(current_directory)

    # step 2: change the hyperparameter.py file lmao
    change_params(dest_a, squid_a)
    change_params(dest_b, squid_b)

    # step 2: run hyperparameter.py in both directories
    os.chdir(f"./{dest_a}")
    command = "python3 hyperparameter.py"
    subprocess.run(command, shell=True, capture_output=True, text=True)
    
    os.chdir("..")

    os.chdir(f"./{dest_b}")
    command = "python3 hyperparameter.py"
    subprocess.run(command, shell=True, capture_output=True, text=True)

    # Step 2: Change to the parent directory
    os.chdir(parent_directory)
    # print(f"Changed directory to: {os.getcwd()}")
    time.sleep(0.1)
    # Step 3: Run a command in the parent directory
    a = 0
    b = 0
    for map in ["DefaultSmall", "DefaultMedium", "DefaultLarge", "DefaultHuge"]:
        command = f"./gradlew run -Pmaps={map} -PteamA={dest_a} -PteamB={dest_b} -PoutputVerbose=false"  # Replace with your command
        result = str(subprocess.run(command, shell=True, capture_output=True, text=True))
        idx = result.find(") wins")-1
        winner = result[idx]
        if winner == "A": a += 1
        else: b += 1
    os.chdir(current_directory)
    if a > b:
        return 1
    elif a < b:
        return 0
    else:
        return -1



def rename_package(dir):
    file_path = f"./{dir}/RobotPlayer.java"  # Replace with your file path
    new_first_line = f"package {dir}; import {dir}.Hyperparameter;"  # New line to replace the first line

    # Step 1: Read the file contents
    with open(file_path, "r") as file:
        lines = file.readlines()

    # Step 2: Replace the first line
    if lines:  # Check if the file has any content
        lines[0] = new_first_line
    else:
        lines.append(new_first_line)  # If the file is empty, add the new line

    # Step 3: Write the updated contents back to the file
    with open(file_path, "w") as file:
        file.writelines(lines)

    # print(f"The first line of '{file_path}' has been updated.")

n = len(sys.argv)
if n == 2 and sys.argv[1] == "-help":
    print(f"python3 hp_opt <source directory> <number of squids> <number of games>")
    sys.exit(1)
elif n != 4:
    print("Invalid use! Try \"-help\" to see format")
    sys.exit(1)

bot = sys.argv[1]
num_squids = int(sys.argv[2])
num_games = int(sys.argv[3])
file_path = f"./{bot}/hyperparameter.py"

module_name = "custom_module"  # You can choose any valid name for the module
spec = importlib.util.spec_from_file_location(module_name, file_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

params = module.hyperparameters

# delete bad
shutil.rmtree(f"./{bot}/__pycache__")
os.remove("Hyperparameter.java")

source = bot
dest_a = f"{bot}_hp_a"
dest_b = f"{bot}_hp_b"

# make two new bots
try:
    shutil.rmtree(dest_a)
    shutil.rmtree(dest_b)
except:
    pass

shutil.copytree(source, dest_a)
shutil.copytree(source, dest_b)


# make sure their packages work
rename_package(dest_a)
rename_package(dest_b)


squids = get_squids(num_squids)

# play_game(squids[0], squids[1])

for _ in range(num_games):
    start = time.time()
    print(f"Playing round {_}: ")
    wins = [0]*num_squids
    for i in range(num_squids):
        for j in range(i + 1, num_squids):
            print(f"playing {squids[i][1]} against {squids[j][1]}")
            res = play_game(squids[i][0], squids[j][0])
            if res == 1: 
                # print("A wins!!")
                print(f"Squid: {squids[i][1]} beats {squids[j][1]}")
                wins[i] += 1
            elif res == 0: 
                # print("B wins!!")
                print(f"Squid: {squids[j][1]} beats {squids[i][1]}")
                wins[j] += 1
            else:
                print(f"Squid: {squids[i][1]} ties {squids[j][1]}")

    end = time.time()
    elapsed = end-start
    print(f"Finished this round, took {elapsed:.2f} seconds")
    ranked_squids = []
    for i in range(num_squids):
        ranked_squids.append((wins[i], squids[i]))

    ranked_squids.sort(reverse=True, key=lambda x: x[0])

    squids = [i[1] for i in ranked_squids[:int(num_squids/2)]]
    squids.extend(get_squids(num_squids-int(num_squids/2)))
    print("_______________")

print("Result: ")
pprint(squids[0][0])
play_game(squids[0][0], squids[0][0])
# print(ranked_squids[1])
