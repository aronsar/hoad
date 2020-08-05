import sys
from os.path import dirname as opd
from os.path import realpath as opr
sys.path.insert(0, opd(opd(opr(__file__)))) # place /path/to/hoad/ in sys.path
import csv
import pickle
import numpy as np
from imitator_agents.data_creation_wrappers.create_agent_data import DataCreator
import argparse

parser = argparse.ArgumentParser()
parser.add_argument(
    '--num_games', '--n', type=int, default=100,
    help='The number of games to run per agent-agent combo.')
parser.add_argument(
    '--modelpath', '--m', type=str, default='./saved_models/iggi.save/best.h5',
    help='Model path of imitator agent for which to create data.')
args = parser.parse_args()


def csif_cluster():
    for pc in range(21, 41):
        ssh -i ~/.ssh/id_rsa aronsar@pc${pc}.cs.ucdavis.edu
        git clone github.com/aronsar/hoad
        cd hoad/imitator_agents
        virtualenv venv -p python3
        source venv/bin/activate
        pip install stuff
        deactivate

        num = pc-21
        for i in range(4):
            screen
            source venv/bin/activate
            python create_imitator_data.py --n 25000 --s /tmp/agent/${num}
            ctrl-a-d

        

if __name__ == '__main__':
    raw_data = DataCreator(args.num_games, args.modelpath).create_data()
    savepath = './tst.pkl'
    pickle.dump(raw_data, open(savepath, 'wb'))
