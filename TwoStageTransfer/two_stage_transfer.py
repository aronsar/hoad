# -*- coding: utf-8 -*-
# pseudo-code to implement two stage transfer learning
import sys
import os
import random, pickle
import numpy as np
import math
import operator

from weka.classifiers import Classifier
from weka.core.converters import Loader
from weka.classifiers import Evaluation
from weka.core.dataset import Instances
import weka.core.converters as converters
from DataLoader import create_header
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
            savepath="",
            rawpath="raw/",
            target_name="",
            num_target=10,
            num_source=100,
            num_eval=1000,
            boosting_iter = 5,
            fold = 2,
            max_source_dataset = 1,
            model = ""):
        self.model = model
        self.targetpath = targetpath
        self.sourcepath = sourcepath
        self.evalpath = evalpath
        self.savepath = savepath
        self.rawpath = rawpath
        self.target_name = target_name
        self.num_games_target = num_target
        self.num_games_source = num_source
        self.num_games_eval = num_eval
        self.source = []
        self.target = ""
        self.eval = ""
        self.boosting_iter = boosting_iter
        self.fold = fold
        self.max_source_dataset = max_source_dataset

    def load_data_from_raw(self):
        print("loading data from raw")
        loader = Loader(classname="weka.core.converters.ArffLoader")
        #target
        print("Loading target data") 
        all_target = loader.load_file(self.rawpath + self.target_name + ".arff")
        all_target.class_is_last()
        self.target, self.eval = all_target.train_test_split(1)
        print("target size:", self.target.num_instances)
        print("Eval size:", self.eval.num_instances)

        #source
        print("Loading source data")
        i=0
        allFiles = os.listdir(self.rawpath)
        random.shuffle(allFiles)
        while i < len(allFiles):
            filename = allFiles[i]
            if filename!= self.target_name+".arff":
                print("Loading", filename)
                source = loader.load_file(self.rawpath + filename)
                source.class_is_last()
                print("Size:", source.num_instances)
                self.source.append(source)
            i+=1

    def load_data_from_arff(self):
        print("Load data from target and source folder")
        if self.targetpath=="" or self.sourcepath=="" or self.evalpath=="" or self.savepath=="":
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

        return fracTargetWeight, fracSourceWeight


    def calcError(self, newModel, test_data_of_kfold):
        '''Return the error from the model with test data from k fold cross validation'''
        error = 0.0
        evl = Evaluation(test_data_of_kfold)
        evl.test_model(newModel, test_data_of_kfold)

        return 100 - evl.percent_correct


    def train_internal(self):
        best_weights_arr = []
        #create an empty F with source as template
        F = Instances.template_instances(self.source[0])
        withF = False
        print("Find weight for each source data set")
        for source in self.source:
            bestWeight, bestError = self.process_source(source, F, withF)
            best_weights_arr.append(bestWeight)
        
        #sort the data based on the weights
        self.source = [source for _, source in sorted(zip(best_weights_arr, self.source), reverse=True, key=operator.itemgetter(0))]
    
        print("Train for final stage")
        withF = True
        while len(self.source) > 0:#self.max_source_dataset):
            weight, _ = self.process_source(self.source[0], F, withF)
            for inst in self.source[0]:
                inst.weight = weight
            F = Instances.append_instances(F, self.source[0])
            F.class_is_last()
            self.source.pop(0)
        
        return F
        
    def process_source(self, source, F, withF):
        bestError = math.inf
        bestWeight = 0.0
        for i in range(1, self.boosting_iter):
            print ("Process with boosting iteration:", i)
            target_w, source_w = self.calculate_weights(i, source)
            error = self.evaluateWeighting(i, source, F, target_w, source_w, withF)
            if error < bestError:
                bestError = error
                bestWeight = source_w

        return bestWeight, bestError

    def evaluateWeighting(self, t, source, F, target_w, source_w, withF):
        '''Calculate erri from k-fold cross validation on T using F'''
        for inst in source:
            inst.weight = source_w

        target = self.target
        for inst in target:
            inst.weight = target_w

        error = 0.0
        for i in range(self.fold):
            classifier = Classifier(classname="weka.classifiers.trees.REPTree")
            train = target.train_cv(self.fold, i)
            test = target.test_cv(self.fold, i)
            
            #train classifier
            if F.num_instances !=0:
                classifier.build_classifier(F)
            classifier.build_classifier(source)
            classifier.build_classifier(train)

            #calculate error
            test.class_is_last()
            error += self.calcError(classifier, test)

        return error

    def train(self):
        if not os.path.exists(self.savepath):
            os.mkdir(self.savepath)

        filename = os.path.join(self.savepath, self.target_name+"_final.arff")
        if os.path.exists(filename):
            print("loading pretrain data for final training")
            loader = Loader(classname="weka.core.converters.ArffLoader")
            F = loader.load_file(filename)
        else:
            print("Start internal training")
            F = self.train_internal()
            self.saveFinal(filename, F)
        
        F.class_is_last()
        final_train_set = Instances.append_instances(F, self.target)
        final_train_set.class_is_last()
        
        print("Building final model")
        self.model.build_classifier(final_train_set)

    def saveFinal(self, filename, F):
        print("Saving final F to train ",self.target_name)
        header = create_header()
        
        with open(filename, "w") as save_final:
            save_final.write(header)
            converters.save_any_file(F, filename)

        print("F saved")

    def evaluate_model(self):
        evl = Evaluation(self.eval)
        evl.test_model(self.model, self.eval)
        
        print(evl.summary())
        print("Accuracy score: ", evl.percent_correct)

    def predict(self, instance):
        return self.model.classify_instance(instance)
