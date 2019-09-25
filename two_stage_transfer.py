# -*- coding: utf-8 -*-
# pseudo-code to implement two stage transfer learning
import sys
import os
#FIXME: add ganabi path to sys
#FIXME: add data path to sys

import gin
import numpy as np
from utils import parse_args
from create_load_data import create_load_data
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import KFold
from sklearn.metrics import mean_squared_error

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
            target = [],
            source = [],
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
        self.boosting_iter = boosting_iter,
        self.fold = fold
        self.max_souce_dataset = max_source_dataset

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
            #define a model
            model = DecisionTreeClassifier()

            #calculate the weight
            weight = (len(target) / (len(target) + len(source))) * (1 - (i / (boosting_iter - 1)))
            weights.append (weight)

            #preparing training and testing data
            source = w_source + source
            print(source)

        if err > max_err:
            max_err = err
            max_err_ind = boosting_iter-1
        return weights[max_err_ind]
    
    def first_stage(self):
        #weights = []
        weight_sourcedata_dict = {}
        for agent in self.source:
            #phi is an empty set
            weight = self.calculate_optimal_weight(self.target,
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
    args = parse_args.parse()
    args = parse_args.resolve_configpath(args)

    #loading data
    data_loader = create_load_data(args)
    '''
    DATA FORMAT:
    - In this example, I created 10 games. The result will be a list of 10 games    - For each game list, there will be 2 elements:
    + Observations (a list): observations encoded in integers from 0-9
    + Actions (a list): one hot encoded vector 
    '''
    #print(data.train_data)
    #`10 games from 1 agent for target datasset, i.e: prior knowledge
    target = data_loader.raw_data["fireflower"]
    # thousands of games from all other agents for source data set, except the target data set agent,  ex: 1000 games from Quux, 1000 games from Walton,...
    source = data_loader.raw_data
    
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
