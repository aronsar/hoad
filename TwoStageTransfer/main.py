from DataLoader import DataLoader
from two_stage_transfer import TwoStageTransfer
from weka.classifiers import Classifier

def main():
    #loading data
    print("**************************************************")
    print("*                 LOADING DATA                   *")
    print("**************************************************")

    data_loader = DataLoader("/data1/shared/agent_data/")
    data_loader.load_target_source_data()
    
    print("**************************************************")
    print("*                 TRAINING                       *")
    print("**************************************************")
    model = Classifier(classname="weka.classifiers.trees.REPTree")
    classifier = TwoStageTransfer(targetpath = "target/",
            sourcepath="source/",
            evalpath = "eval/",
            savepath = "final/",
            target_name = data_loader.target_name,
            boosting_iter=10,
            fold=10,
            max_source_dataset=15,
            model = model)
    classifier.load_data_from_arff()
    classifier.train()

    print("**************************************************")
    print("*                EVALUATING                      *")
    print("**************************************************")
    classifier.evaluating_model()

if __name__== '__main__':
    main()
