import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from utils import dir_utils, parse_args
from collections import defaultdict
import pickle
import gin
import importlib
import argparse
import pandas


# TODO: include datapath argument for csv file
def convert_to_pickle():
    path = os.path.dirname(__file__)
    os.chdir(path)
    raw_data = pandas.read_csv('data.csv')
    return raw_data


# for testing
def print_pickle_data():
    data = pickle.load(open('data.p', 'rb'))
    print(data)


def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument('--agent_name', default = 'fireflower')
    parser.add_argument('--num_players', type = int)
    parser.add_argument('--num_games', type = int)
    parser.add_argument('--datapath')
    args = parser.parse_args()
    return args


def main(args):
    fireflower_data = convert_to_pickle()
    pickle.dump(fireflower_data, open('data.p', "wb"))
    print_pickle_data()


if __name__ == '__main__':
    args = parse()
    main(args)
