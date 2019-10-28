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
            evalpath="",
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
        self.evalpath = evalpath
        self.source = []
        self.target = ""
        self.eval = ""
        self.boosting_iter = boosting_iter
        self.fold = fold
        self.max_source_dataset = max_source_dataset
    
    def load_data_from_arff(self):
        print("Load data from target and source folder")
        if self.targetpath=="" or self.sourcepath=="":
            assert "No path specify, please create data first"

        loader = Loader(classname="weka.core.converters.ArffLoader")
        
        #target dataset
        print("Get instances of target data set")
        self.target = loader.load_file(self.targetpath + os.listdir(self.targetpath)[0])
        self.target.class_is_last()

        #source dataset
        print("Get instances of source data set")
        for filename in os.listdir(self.sourcepath):
            source = loader.load_file(self.sourcepath + filename)
            source.class_is_last()
            self.source.append(source)

        #eval dataset
        print("Get instances of eval dataset")
        self.eval = loader.load_file(self.evalpath + os.listdir(self.evalpath)[0])
        self.eval.class_is_last()

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
        #create an empty F with source as template
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
            F = Instances.append_instances(F, self.source[i])
        
        F.class_is_last()
        return F
        
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

        for inst in source:
            inst.weight = source_w

        trainDataSet = Instances.append_instances(source,F)
        
        target = self.target
        for inst in target:
            inst.weight = target_w

        error = 0.0
        for i in range(self.fold):
            train = target.train_cv(self.fold, i)
            test = target.test_cv(self.fold, i)
            
            #append train target set to source set
            fix_set = Instances.append_instances(trainDataSet,train)
            fix_set.class_is_last()

            #train classifier
            classifier.build_classifier(fix_set)

            #calculate error
            test.class_is_last()
            error += self.calcError(classifier, test)

        return error

    def train(self):
        F = self.train_internal()
        final_train_set = Instances.append(F, self.target)
        final_train_set.class_is_last()

        self.model.build_classifier(final_train_set)

    def evaluate_model(self):
        evl = Evaluation(self.eval)
        evl.test_model(self.model, self.eval)

        print("Accuracy score: ", evl.percent_correct)
