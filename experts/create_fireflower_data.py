# Assumes datapath argument is a directory and not a file
# Breaks if it is a file
# TODO: add datapath as file functionality

# add repo root to path variable for easy importing of modules
import os
import sys
ganabi_path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
hanabi_env_path = os.path.join(ganabi_path, "hanabi_env")
sys.path.insert(0, ganabi_path)
sys.path.insert(0, hanabi_env_path)

from utils import dir_utils, parse_args
from collections import defaultdict
import pickle
import gin
import importlib
import argparse
import pandas
import pathlib
import subprocess
import rl_env


# Check if data path given exists, if not create it
# Then, set working directory to given data path
def set_data_path():
    
    default_data_path = os.path.join((os.path.dirname(os.path.abspath(__file__))), "fireflower")

    # If we are using the default data path and it doesn't exist, create it
    # If we are using the argument data path and it doesn't exist, create it
    # This way every reference to the data path will be valid
    # This should be a redundant check as the data creator in fire flower
    # should already have created a data path
    if not args.datapath and not (os.path.isdir(default_data_path)):
        create_data_path(default_data_path)
    elif args.datapath and not (os.path.exists(args.datapath)):
        create_data_path(args.datapath)

    # Set data_path var to directory given by call
    # This means we set to default if nothing is specified
    # and the specified directory if it exists
    if args.datapath:
        data_path = args.datapath
    else:
        data_path = default_data_path

    # Set working directory to data_path
    os.chdir(data_path)
    

# Create data path directory
def create_data_path(data_path):
    os.makedirs(data_path)


# Read in raw data and return it
def read_data(file_name):
    # Read in data in csv format using Pandas lib
    raw_data = pandas.read_csv(file_name + ".csv")
    return raw_data


# Print pickle data to console to test format and file issues
def print_pickle_data():
    file_name = "data.p"
    data = pickle.load(open(file_name, 'rb'))
    print(data)


# Parse CLI arguments
# Functionality defined in /ganabi/utils/parse_args.py
# All arguments are optional, with the defaults as:
# agent_name = fireflower
# num_players = 2
# num_games = 10
# datapath = /ganabi/experts/data/<agent_name>_<num_players>_<num_games>.p
def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument('--agent_name', default = 'fireflower')
    parser.add_argument('--num_players', type = int, default = 2)
    parser.add_argument('--num_games', type = int, default = 10)
    parser.add_argument('--datapath')
    args = parser.parse_args()
    return args


# Dump raw csv data into pickle format in datapath
def dump_pickle_data(fireflower_data, file_name):

    # Dump data to appropriate directory
    pickle.dump(fireflower_data, open(file_name + ".p", "wb"))


def get_config(data, num_players):
    config = {'colors': 5, 'ranks': 5, 'players': num_players, 'hand_size': 5,
            'max_information_tokens': 8, 'max_life_tokens': 3,
            'observation_type': 1, 'seed': 1234, 'random_start_player': False}
    return config

def create_csv_from_scala(numGames):
        subprocess.run("export JAVA_HOME=/data1/shared/fireflowerenv/jre1.8.0_221", shell=True)
        subprocess.run("export PATH=$JAVA_HOME/bin:$PATH", shell=True)
        
        args = ["/data1/shared/fireflowerenv/sbt/bin/sbt", "run " + str(numGames) + " " +  os.getcwd()]
        process = subprocess.Popen(args, universal_newlines=True)
        process.communicate() # solves issue where Popen hangs
        
# retrieve deck from data file in format given
def get_decks(data):
    decks = []

    index = 0
    for step in data["Game Step"]:
        if step == 1:
            decks.append(data["Initial Deck"][index])
        index += 1

    return decks


# convert to hanabi env format
def convert_decks(decks):
    converted_decks = []

    for deck in decks:
        converted_deck = [None for i in range(int(len(deck) / 2))]

        index = 0
        for card in converted_deck:
            card = deck[index] + deck[index + 1]
            converted_deck[int(index / 2)] = card
            index += 2

        converted_decks.append(converted_deck)

    return converted_decks


def retrieve_decks_deepmind_format(data):
    decks = get_decks(data)
    decks = convert_decks(decks)
    return decks


#FIXME: make return one hot data from raw data
def one_hot_vectorized_action(data, num_moves, game_num, game_step):
    '''
    Returns:
        one_hot_action_vector: one hot action vector
        action: action in the form recognizable by the Hanabi environment
                (idk something like {'discard': 5})
    '''
    action_index = 0
    for index in range(len(data['Game Number'])):
        if int(data['Game Number'][index]) == game_num and int(data['Game Step'][index]) == game_step:
            action_index = index
            break

    move_type = data['Move Type'][action_index]
    card_info = data['Card Color/Rank/Position'][action_index]
    
    print('game num', game_num)
    print('game_step', game_step)
    print('movetype', move_type)


    one_hot_action_vector = [0]*num_moves


    action = {}
    action['action_type'] = move_type

    if move_type == 'PLAY':
        # converting to hanabi idx because scala hand is in flipped format
        idx = int(card_info, 10) - 1
        if idx == 0:
            idx = 4
        elif idx == 1:
            idx = 3
        elif idx == 3:
            idx = 1
        elif idx == 0:
            idx = 4
        action['card_index'] = idx
    elif move_type == 'DISCARD':
        # converting to hanabi idx because scala hand is in flipped format
        idx = int(card_info, 10) - 1
        if idx == 0:
            idx = 4
        elif idx == 1:
            idx = 3
        elif idx == 3:
            idx = 1
        elif idx == 0:
            idx = 4
        action['card_index'] = idx
    elif move_type == 'REVEAL_COLOR':
        action['color'] = str(card_info)
        action['target_offset'] = 1
    elif move_type == 'REVEAL_RANK':
        action['rank'] = int(card_info)
        action['target_offset'] = 1

    else:
        print("Invalid move")
        raise Exception("Invalid move given in one_hot_vectorized_action func; wrong format data given")

    print(action)
#    action_idx = obs['legal_moves_as_int'][obs['legal_moves'].index(action)]
#    one_hot_action_vector[action_idx] = 1

    return one_hot_action_vector, action


def convert_data(data, args, num_players):
    converted_data = []
    config = get_config(data, num_players)
    decks = retrieve_decks_deepmind_format(data)

    env = rl_env.HanabiEnv(config)

    for game_num in range(args.num_games):
        deck = decks[game_num]
        converted_data.append([[],[]])
        obs = env.reset(deck)
        game_done = False
        game_step = 0
            
        while not game_done:
            for agent_id in range(num_players):
                game_step += 1

                one_hot_action_vector, action = one_hot_vectorized_action(
                        data,
                        env.num_moves(),
                        game_num + 1, 
                        game_step)

                converted_data[game_num][0].append(obs['player_observations'][agent_id]['vectorized'])
                converted_data[game_num][1].append(one_hot_action_vector)

                obs, _, game_done, _ = env.step(action)

                if game_done:
                    break

    return converted_data


def get_file_name(num_players, num_games):
    return "fireflower_" + str(num_players) + "p_" + str(num_games)


def read_convert_data(num_players, args):
    print(os.getcwd())
    file_name = get_file_name(num_players, args.num_games)
    data = read_data(file_name)
    data = convert_data(data, args, num_players)
    dump_pickle_data(data, file_name)


def convert_all_data_files(args):
    for num_players in range(2,6,1):
        read_convert_data(num_players,args)
    


def create_data(args):
    # changing working directory to jar to run
    set_data_path()
    create_csv_from_scala(args.num_games)

def main(args):
    # FIXME: compile fireflower agent into jar
    # create_data(args)
    create_data(args)
    convert_all_data_files(args)


if __name__ == '__main__':
    args = parse()
    main(args)
