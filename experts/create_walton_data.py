# this script creates data using python 2 and rainbow agents

# here we add the repo's root directory to the path variable; everything
# is imported relative to that to avoid problems
from subprocess import call

import argparse
import os
import pandas as pd
import numpy as np

import pickle


# import sys
# sys.path.insert(0, os.path.dirname(
#     os.path.dirname(os.path.realpath(__file__))))

def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument('--agent_name',
                        default='iggi')

    parser.add_argument('--num_players',
                        type=int)

    parser.add_argument('--num_games',
                        type=int)

    parser.add_argument('--datapath')

    parser.add_argument('--rainbowdir')  # FIXME

    args = parser.parse_args()
    return args

# TODO: For future work, we can have the target agent and other agents to be different types
def create_csv_from_java(jar_filename, csv_filename, agent_name, player_count, game_count, seed):
    call("java -jar %s %s %s %s %s %s 1>%s" % (jar_filename, agent_name, agent_name, player_count, game_count, seed, csv_filename), shell=True)

def create_pkl_data(csv_data):
    '''
    Convert csv data  to pkl
    '''
    raw_data = []
    
    # TODO: Fix csv
    game_num = csv_data.iloc[0, 0] + 1 # need to add 1
    obs_size = csv_data.iloc[0, 1]
    act_size = csv_data.iloc[0, 2]

    for i in range(0, game_num):
        ids = csv_data.iloc[:, -1] == i
        game_data = csv_data[ids]
        obs = np.array(game_data.iloc[:, 3:3+obs_size]).tolist()
        act = np.array(game_data.iloc[:, (-1 * act_size):]).tolist()
        raw_data.append([obs, act])
    
    return raw_data

def create_data_filenames(args):
    '''
    '''
    
    # Config csv & pkl file path
    agent_data_filename = args.agent_name + "_" + str(args.num_players) + "_" + str(args.num_games)
    datapath = os.path.dirname(args.datapath)
    csv_filename = os.path.join(datapath, agent_data_filename + ".csv")
    pkl_filename = os.path.join(datapath, agent_data_filename + ".pkl")
    
    # Config jar file path
    jar_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "walton_rivers_agents")
    jar_filename = os.path.join(jar_path, "walton.jar")

    return csv_filename, pkl_filename, jar_filename


def main(args):
    # Sort Params
    seed = 1 
    csv_filename, pkl_filename, jar_filename = create_data_filenames(args) 

    # Create csv on Disk by using Java code
    create_csv_from_java(jar_filename, csv_filename, args.agent_name, args.num_players, args.num_games, seed)

    # Read csv
    csv_data = pd.read_csv(csv_filename)

    # Convert csv to pkl
    pkl_data = create_pkl_data(csv_data)

    # Save pkl on Disk
    pickle.dump(pkl_data, open(pkl_filename, "wb"))

    # Remove csv on Disk
    remove_csv = True
    if (remove_csv):
        os.remove(csv_filename)
    


if __name__ == '__main__':
    print("Create walton data")
    args = parse()
    main(args)