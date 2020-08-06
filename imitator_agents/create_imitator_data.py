import sys
import os
from pathlib import Path
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
    '--num_games', '--n', type=int, default=25000,
    help='The number of games to run per agent-agent combo.')
parser.add_argument(
    '--agent', '--a', type=str, default='iggi',
    help='Imitator agent for which to create data. Must have model'
    'present in ./saved_models/<agent>.save/best.h5')
parser.add_argument(
    '--batch_num', '--b', type=int, required=True,
    help='The subdirectory where the games will be saved.')
args = parser.parse_args()

'''
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
'''
        

if __name__ == '__main__':
    modelpath = './saved_models/' + args.agent + '.save/best.h5'
    raw_data = DataCreator(args.num_games, modelpath).create_data()

    agentpath = '/tmp/replay_data/' + args.agent + '/'
    
    if not os.path.exists(agentpath):
        Path(agentpath).mkdir(parents=True, exist_ok=True)
    
    Path(agentpath + str(args.batch_num)).mkdir()
    savepath = agentpath + str(args.batch_num) + '/' + args.agent + '.pkl'
    pickle.dump(raw_data, open(savepath, 'wb'))
