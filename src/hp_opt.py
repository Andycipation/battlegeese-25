import sys
from random import *
import importlib.util
global params 

def get_squids(num_squids):
    cur_squids = []
    for _ in range(num_squids):
        this_squid = dict()
        for param in params:
            curr, l, r = params[param]
            this_squid[param] = (randint(l, r), l, r)
        cur_squids.append(this_squid)
    return cur_squids

def play_game(squid_a, squid_b):
    pass

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




squids = get_squids(num_squids)

# for _ in range(num_games):
#     wins = [0]*num_squids
#     for i in range(num_squids):
#         for j in range(i + 1, num_squids):
#             res = play_game(squids[i], squids[j])
#             if res == 1: wins[i] += 1
#             else: wins[j] += 1
    
#     ranked_squids = []
#     for i in range(num_squids):
#         ranked_squids.append((wins[i], squids[i]))
    
#     wins.sort(reverse=True)

#     squids = squids[:int(num_squids/2)]
#     squids.extend(get_squids(num_squids-int(num_squids/2)))

# print(squids[0])
