# Assumes datapath argument is a directory and not a file
# Breaks if it is a file
# TODO: add datapath as file functionality

# add repo root to path variable for easy importing of modules
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.realpath(__file__))))

from utils import dir_utils, parse_args
from collections import defaultdict
import pickle
import gin
import importlib
import argparse
import pandas
import pathlib
import subprocess


# Check if data path given exists, if not create it
# Then, set working directory to given data path
def set_data_path():

    # Set default data path local var for easy reference
    default_data_path = (os.getcwd() + "/data")

    # If we are using the default data path and it doesn't exist, create it
    # If we are using the argument data path and it doesn't exist, create it
    # This way every reference to the data path will be valid
    # This should be a redundant check as the data creator in fire flower
    # should already have created a data path
    if not args.datapath and not (os.path.isdir(os.getcwd() + "/data")):
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
def read_data():
    # Set file name to provided
    file_name = args.agent_name + "_" + str(args.num_players) + "_" + str(args.num_games) + ".csv"

    # Read in data in csv format using Pandas lib
    raw_data = pandas.read_csv(file_name)
    return raw_data


# Print pickle data to console to test format and file issues
def print_pickle_data():
    file_name = args.agent_name + "_" + str(args.num_players) + "_" + str(args.num_games) + ".p"
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
def dump_pickle_data(fireflower_data, args):

    # Set dump filename
    file_name = args.agent_name + "_" + str(args.num_players) + "_" + str(args.num_games) + ".p"

    # Dump data to appropriate directory
    pickle.dump(fireflower_data, open(file_name, "wb"))


def convert_data():
    set_data_path()
    data = read_data()
    dump_pickle_data(data, args)
    print_pickle_data()


def jar_wrapper(*jar_args):
    process = subprocess.Popen(['java', '-jar']+list(jar_args),
                               stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    ret = []
    while process.poll() is None:
        line = process.stdout.readline()
        if line != '' and line.endswith('\n'):
            ret.append(line[:-1])
    stdout, stderr = process.communicate()
    ret += stdout.split('\n')
    if stderr != '':
        ret += stderr.split('\n')
    ret.remove('')
    return ret


def create_data(args):
    # changing working directory to jar to run
    jar_path = os.getcwd() + '/fireflower'
    os.chdir(jar_path)

    '''
    simple untested method to call jar (don't know if it can handle args):
    
    subprocess.call(['java', 'jar', 'fireflower.jar'])
    '''

    '''
    more complex method to call jar, can handle args and will print out errors and ret (which is null)
    '''

    jar_args = ['fireflower.jar', '--agent_name' + args.agent_name, '--num_players' + args.num_players,
                '--num_games' + args.num_games,'--datapath' + args.datapath]

    result = jar_wrapper(*jar_args)

    print(result)


def main(args):
    # FIXME: compile fireflower agent into jar
    # create_data(args)
    convert_data()


if __name__ == '__main__':
    args = parse()
    main(args)
