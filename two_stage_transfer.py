# -*- coding: utf-8 -*-
# pseudo-code to implement two stage transfer learning
import sys
import os
#FIXME: add ganabi path to sys
#FIXME: add data path to sys

import gin, os
import random, pickle
import numpy as np
from utils import parse_args
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import KFold
from sklearn.metrics import mean_squared_error

'''
DATA LOADER FOR TWO STAGE TRANSFER
'''
class DataLoader(object):
    def __init__(self,
            datapath):
        self.datapath = datapath
        self.all_agents_datadir = []
        self.target = {}
        self.source = {}
    
    def load_target_source_data(self):
        self.__get_all_agents()
        if len(self.all_agents_datadir)==0:
            assert("No agent available")
        
        self.get_target_data()
        self.get_source_data()

    def get_target_data(self):
        target_agent_dir = random.choice(self.all_agents_datadir)
        target_agent_name = "_".join(target_agent_dir.split("_")[:-3])
        data = self.__get_25k_data(os.path.join(self.datapath,target_agent_dir))
        print("Getting target data for ", target_agent_name)
        #self.target[target_agent_name] = data[:10]
        self.target[target_agent_name] = self.__format_data(data[:10])
        #print(self.target[target_agent_name])
   
    def get_source_data(self):
        target_agent = list(self.target.keys())[0]
        source_agents_dir = []
        for agent_dir in self.all_agents_datadir:
            agent_name = "_".join(agent_dir.split("_")[:-3])
            if agent_name != target_agent:
                print("Getting source data for ", agent_name)
                data = self.__get_25k_data(os.path.join(self.datapath, agent_dir))
                self.source[agent_name] = self.__format_data(data[:100])

    def __get_all_agents(self):
        self.all_agents_datadir = [name for name in os.listdir(self.datapath)]

    def __get_25k_data(self, datadir):
        all_dir = [name for name in os.listdir(datadir)]
        first_dir = os.path.join(datadir, all_dir[0])
        file_name = os.listdir(first_dir)[0]
        path_to_file = os.path.join(first_dir, file_name)
        return pickle.load(open(path_to_file, "rb"), encoding='latin1')

    def __format_data(self, games):
        #print(games)
        '''
        Input data is a list of all games of 1 agent:
            For each game:
                + A list contains 2 list:  obs and acts of the whole game
                    + In obs list: [12345, 67890,...]
                    + In acts list: [[0,0,0,1,0,0],[1,0,0,0,0],...]
        Output data format is a list with 2 lists:
            + List 1: Obs of all games combined in 1 list
            + List 2: Acts of all games combined in 1 list
        '''
        obs = games[0][0]
        acts = games[0][1]
        for i in range (1, len(games)):
            obs += games[i][0]
            acts += games[i][1]
        return [obs, acts]

'''
TwoStageTransfer (T, S, m, k, b)
    for all S_i in S: do
        wi ← CalculateOptimalWeight(T, ∅, Si, m, k)
    Sort S in decreasing order of w_i's
    F ← ∅
    for i from 1 to b do
        w ← CalculateOptimalWeight(T, F, S_i, m, k)
        F ← F ∪ S^w_i
    Train classifier c on T ∪ F
    return c
'''
class TwoStageTransfer:
    def __init__(self,
            target = {},
            source = {},
            boosting_iter = 5,
            fold = 10,
            max_source_dataset = 1):
        '''
        - target is target data set (10 adhoc games from same agent)
        - S is set of source data sets S = {S_1, S_2, ..., S_n} (games from previously observed teammates,
        which can be thousands of data points in length)
        - m is num of boosting iterations
        - k is num of folds for cross validation, k should be 10 as we have 10 games
        - b is max num of source data sets to include
        - S^w means data set S taken with weight w spread over instances
        - F is weighted source data
        '''
        self.target = target
        self.source = source
        self.boosting_iter = boosting_iter
        self.fold = fold
        self.max_souce_dataset = max_source_dataset

    def int_to_bool(self, intvec):
        boolvec = np.array([])
        for num in np.array(intvec):
            temp=np.array(list('{0:b}'.format(num)), dtype=int)
            #print (temp)
            print (len(temp))
            if(len(boolvec)==0):
                boolvec=[np.pad(temp, ((658-len(temp)),0), 'constant', constant_values=(0,0 ))]
            else:
                boolvec = np.append(boolvec,[np.pad(temp, ((658-len(temp)),0), 'constant', constant_values=(0,0 ))],axis=0)
        return boolvec


    def calculate_optimal_weight(self, target, w_source, source, boosting_iter, fold):
        '''
        CalculateOptimalWeight(T, F, S, m, k):
        for i from 1 to m do
         w_i = (len(T) / (len(T) + len(S))) *  (1 − i /(m − 1))
        Calculate erri from k-fold cross validation on T using F and S wi as additional training data
        return wj such that j = argmaxi(erri)
        '''
        weights = []
        max_err = 0
        max_err_ind = 0
        
        for i in range(1, boosting_iter+1):
            #calculate the weight
            weight = (len(target) / (len(target) + len(source))) * (1 - (i / (boosting_iter - 1)))
            weights.append (weight)

            #preparing training and testing data
            if len(w_source)!=0:
                print("w_source is not empty")
                #concatenate

            target_obs = self.int_to_bool(target[0])


            if i==1:
                print(len(target_obs))

            target_act = np.array(target[1])
            source_obs = self.int_to_bool(source[0])
            source_act = np.array(source[1])
            #kFold cross validation
            kf = KFold(n_splits = self.fold)
            err = []

            for train,test in kf.split(target_obs):
                #define a model
                model = DecisionTreeClassifier()
                obs_train = np.concatenate((source_obs,target_obs[train]))
                act_train = np.concatenate((source_act,target_act[train]))
                obs_test = target_obs[test]
                act_test = target_act[test]
                
                model.fit(obs_train,act_train, sample_weight=weight)
                act_predict = model.predict(obs_test)
                err.append(mean_squared_error(act_predict, act_test))

        if err > max_err:
            max_err = err
            max_err_ind = boosting_iter-1
        return weights[max_err_ind]
    
    def first_stage(self):
        #weights = []
        weight_sourcedata_dict = {}
        target_agent_name = list(self.target.keys())[0]
        for agent in self.source:
            #phi is an empty set
            
            weight = self.calculate_optimal_weight(self.target[target_agent_name],
                [],
                self.source[agent],
                self.boosting_iter,
                self.fold)
        sortedS = sort_data_by_weight(weight_sourcedata_dict)
    
        weighted_source = []
       
        for i in range(max_source_dataset):
            weight = calculate_optimal_weight(self.target, weighted_source, source[i], self.boosting_iter, self.fold)
            weighted_source = weighted_source.append(source[i] * weight)
        training_data = target + weighted_source
        return train_classifier(training_data)


# train decision tree with data of prev games using scikitlearn lib
def second_stage(training_data):
    clf = DecisionTreeClassifier()
    obs = [data[0] for data in training_data]
    act = [data[1] for data in training_data]

    classifier = clf.fit(obs, act)
    return classifier


# FIXME: aclculate error
def calculate_err(target, weighted_source, S):
    return err

def sort_data_by_weight(weight_sourcedata_dict):
    return [weight_sourcedata_dict[key] for key in sorted(weight_sourcedata_dict.keys(), reverse=True)]

                                                                
                                                                
def main():
    #loading data
    data_loader = DataLoader("./data/agent_data")
    data_loader.load_target_source_data()
    '''
    DATA FORMAT:
    - In this example, I created 10 games. The result will be a list of 10 games    - For each game list, there will be 2 elements:
    + Observations (a list): observations encoded in integers from 0-9
    + Actions (a list): one hot encoded vector 
    '''
    #print(data.train_data)
    #`10 games from 1 agent for target datasset, i.e: prior knowledge
    target = data_loader.target
    # thousands of games from all other agents for source data set, except the target data set agent,  ex: 1000 games from Quux, 1000 games from Walton,...
    source = data_loader.source
    
    print("============ len ", len(source), "============")
    classifier = TwoStageTransfer(target,
            source,
            boosting_iter=10,
            fold=10,
            max_source_dataset=len(source))
    
    classifier.first_stage()
    # return classifier

if __name__== '__main__':
    main()
