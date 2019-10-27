# -*- coding: utf-8 -*-
# pseudo-code to implement two stage transfer learning
import sys
import os
#FIXME: add ganabi path to sys
#FIXME: add data path to sys

import gin, os
import random, pickle
import numpy as np
import math
import weka.core.jvm as jvm
#from utils import parse_args

jvm.start()
from weka.classifiers import Classifier
from weka.core.converters import Loader
from weka.classifiers import Evaluation
from weka.core.dataset import Instances
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
        target_agent_dir = "quux_blindbot_data_2_500000"
        target_agent_name = "_".join(target_agent_dir.split("_")[:-3])
        print("Getting target data for ", target_agent_name)
        data = self.__get_25k_data(os.path.join(self.datapath,target_agent_dir))
        
        self.target[target_agent_name] = self.write_data_to_arff(data[:10], target_agent_name, "target")
   
    def get_source_data(self):
        target_agent = list(self.target.keys())[0]
        source_agents_dir = []
        for agent_dir in self.all_agents_datadir:
            agent_name = "_".join(agent_dir.split("_")[:-3])
            if agent_name != target_agent:
                print("Getting source data for ", agent_name)
                data = self.__get_25k_data(os.path.join(self.datapath, agent_dir))
                self.source[agent_name] = self.write_data_to_arff(data[:100], agent_name, "source")

    def __get_all_agents(self):
        self.all_agents_datadir = [name for name in os.listdir(self.datapath)]

    def __get_25k_data(self, datadir):
        all_dir = [name for name in os.listdir(datadir)]
        first_dir = os.path.join(datadir, all_dir[0])
        file_name = os.listdir(first_dir)[0]
        path_to_file = os.path.join(first_dir, file_name)
        return pickle.load(open(path_to_file, "rb"), encoding='latin1')

    def write_data_to_arff(self, games, agent_name, datapath):
        filename = os.path.join(datapath, agent_name + ".arff")
        header = "@RELATION ObsActs\n"
        
        for i in range(658):
            header += "@ATTRIBUTE obs%d NUMERIC\n" % i
        header += "@ATTRIBUTE class {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19}\n"
        header += "@DATA\n" 

        with open(filename,"w") as arff_file:
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
            targetpath="",
            sourcepath="",
            boosting_iter = 5,
            fold = 10,
            max_source_dataset = 1,
            model = ""):
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
        self.model = model
        self.targetpath = targetpath
        self.sourcepath = sourcepath
        self.source = []
        self.target = ""
        self.boosting_iter = boosting_iter
        self.fold = fold
        self.max_source_dataset = max_source_dataset
    
    def load_data_from_arff(self):
        print("Load data from target and source folder")
        if self.targetpath=="" or self.sourcepath=="":
            assert "No path specify, please create data first"

        loader = Loader(classname="weka.core.converters.ArffLoader")
        
        #target dataset
        print("Get instance of target data set")
        self.target = loader.load_file(self.targetpath + os.listdir(self.targetpath)[0])
        self.target.class_is_last()

        #source dataset
        print("Get instance of source data set")
        for filename in os.listdir(self.sourcepath):
            source = loader.load_file(self.sourcepath + filename)
            source.class_is_last()
            self.source.append(source)

    def calculate_weights(self, t, source):
        #for i from 1 to m do
        # w_i = (len(T) / (len(T) + len(S))) *  (1 − i /(m − 1))
        n = source.num_instances
        m = self.target.num_instances

        fracSourceWeight = (m/(n+m)) * (1 - (t/(self.boosting_iter)))
        fracTargetWeight = 1 - fracSourceWeight

        #calculate for each instance
        #totalWeight = n / fracSourceWeight
        #targetWeight = fracTargetWeight * totalWeight / m
        #sourceWeight = fracSourceWeight * totalWeight / n
        
        print("taret", fracTargetWeight)
        print("source", fracSourceWeight)
        return fracTargetWeight, fracSourceWeight


    def getNumInstances(self, data_array):
        '''Return the number of instances from all games of the agents inside data_array'''
        numInstances = 0
        for agent in data_array:
            numInstances += agent.num_instances()
            
        return numInstances

    def calcError(self, newModel, test_data_of_kfold):
        '''Return the error from the model with test data from k fold cross validation'''
        error = 0.0
        evl = Evaluation(test_data_of_kfold)
        evl.test_model(newModel, test_data_of_kfold)

        print("The percent incorrect is: ", 100 - evl.percent_correct)

        return 100 - evl.percent_correct


    def train_internal(self):
        '''
        for all Si in S do:
            wi <- CalculateOptimalWeight(T,∅,Si,m,k)
        Sort S in decreasing order of wi’s
        '''
        best_weights_arr = []
        F = Instances.template_instances(self.source[0])
        for source in self.source:
            bestWeight, bestError = self.process_source(source, F)
            best_weights_arr.append(bestWeight)

        #sort the data based on the weights
        self.source = [source for _, source in sorted(zip(best_weights_arr, self.source), reverse=True)]

        '''
        for i from 1 to b do
            w <- CalculateOptimalWeight(T, F, Si, m, k)
            F ← F ∪ S iw
        '''
        #F = Instances() 
        for i in range(self.max_source_dataset):
            weight = self.process_source(self.source[i], F)
            for inst in self.source[i]:
                inst.weight = weight
                if not F:
                    F = Instances(inst);
                else:
                    F.add_instance(inst)
        
    def process_source(self, source, F):
        '''
        for i from 1 to m do
               cal wi based on formula
        Calculate erri from k-fold cross validation on T using F
        and Swi as additional training data return wj such that j = argmax(erri)
        '''
        print("the type of source is", type(source))
        bestError = math.inf
        bestWeight = 0.0
        for i in range(1, self.boosting_iter+1):
            print ("Process with boosting iteration:", i)
            target_w, source_w = self.calculate_weights(i, source)
            error = self.evaluateWeighting(i, source, F, target_w, source_w)
            print("The error of this boosing iteration is:", error)
            if error < bestError:
                bestError = error
                bestWeight = source_w
            print()

        return bestWeight, bestError

    def evaluateWeighting(self, t, source, F, target_w, source_w):
        '''Calculate erri from k-fold cross validation on T using F'''
        classifier = Classifier(classname="weka.classifiers.trees.REPTree")
        trainDataSet = Instances.copy_instances(source)

        for inst in trainDataSet:
            inst.weight = source_w
        if F.num_instances != 0:
            trainDataSet = Instances.append_instances(trainDataSet,F)

        target = self.target
        for inst in target:
            inst.weight = target_w

        error = 0.0
        fix_set = Instances.copy_instances(trainDataSet)
        for i in range(self.fold):
            train = target.train_cv(self.fold, i)
            test = target.test_cv(self.fold, i)
            
            #append train target set to source set
            fix_set = Instances.append_instances(fix_set,train)
            
            #train classifier
            classifier.build_classifier(fix_set)

            #calculate error
            error += self.calcError(classifier, test)

        return error


    # train decision tree with data of prev games using scikitlearn lib
#    def train(self):

                                                                
                                                                
def main():
    #loading data
    print("**************************************************")
    print("*                 LOADING DATA                   *")
    print("**************************************************")

    #data_loader = DataLoader("/data1/shared/agent_data/")
    #data_loader.load_target_source_data()
    
    print("**************************************************")
    print("*                 TRAINING                       *")
    print("**************************************************")
    model = Classifier(classname="weka.classifiers.trees.REPTree")
    classifier = TwoStageTransfer(targetpath = "target/",
            sourcepath="source/",
            boosting_iter=10,
            fold=10,
            max_source_dataset=15,
            model = model)
    classifier.load_data_from_arff()
    classifier.train_internal()

if __name__== '__main__':
    main()
