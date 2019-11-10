import os
import random, pickle
import numpy as np

'''
Load data from datapath and write to arff files. Save those arff files in arff_data_path
'''
class DataLoader(object):
    def __init__(self,
            datapath,
            arff_data_path="",
            target_name="", 
            num_games=1000):
        self.datapath = datapath
        self.all_agents_datadir = []
        self.target = {}
        self.source = {}
        self.arff_data_path = arff_data_path
        self.num_games = num_games
        self.target_name = target_name

    def load_target_source_data(self):
        self.__get_all_agents()
        if len(self.all_agents_datadir)==0:
            assert("No agent available")

        self.get_target_data()
        self.get_source_data()

    def get_target_data(self):
        target_agent_dir = self.target_name + "_data_2_500000"
        print("Getting target data for ", self.target_name)
        data = self.__get_25k_data(os.path.join(self.datapath,target_agent_dir))
        filename = self.target_name + ".arff"
        self.target[self.target_name] = self.write_data_to_arff(data[:self.num_games], filename, self.arff_data_path)
        filename = self.target_name + "_test.arff"

    def get_source_data(self):
        source_agents_dir = []
        for agent_dir in self.all_agents_datadir:
            agent_name = "_".join(agent_dir.split("_")[:-3])
            if agent_name != self.target_name:
                print("Getting source data for ", agent_name)
                data = self.__get_25k_data(os.path.join(self.datapath, agent_dir))
                filename = agent_name + ".arff"
                self.source[agent_name] = self.write_data_to_arff(data[:self.num_games], filename, self.arff_data_path)

    def __get_all_agents(self):
        self.all_agents_datadir = [name for name in os.listdir(self.datapath)]

    def __get_25k_data(self, datadir):
        all_dir = [name for name in os.listdir(datadir)]
        first_dir = os.path.join(datadir, all_dir[0])
        file_name = os.listdir(first_dir)[0]
        path_to_file = os.path.join(first_dir, file_name)
        return pickle.load(open(path_to_file, "rb"), encoding='latin1')

    def write_data_to_arff(self, games, filename, datapath):
        if not os.path.exists(datapath):
            os.mkdir(datapath)

        f = os.path.join(datapath, filename)
        header = create_header()

        with open(f,"w") as arff_file:
            arff_file.write(header)
            for game in games:
                obs = game[0]
                acts = game[1]
                for step in range(len(obs)):
                    ob = self.int_to_bool(obs[step])
                    act = self.bool_to_int(acts[step])
                    string = ",".join([str(num) for num in ob])
                    arff_file.write(string + ",%d\n" % act)
    

    def int_to_bool(self,num):
        boolvec = np.array([])
        temp = np.array(list('{0:b}'.format(num)), dtype=int)
        boolvec=np.pad(temp, ((658-len(temp)),0), 'constant', constant_values=(0,0 ))
        return boolvec

    def bool_to_int(self, onehot):
        return np.argmax(onehot)


def create_header():
    header = "@RELATION ObsActs\n"

    for i in range(658):
        header += "@ATTRIBUTE obs%d {0,1}\n" % i
    header += "@ATTRIBUTE class {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19}\n"
    header += "@DATA\n"

    return header
