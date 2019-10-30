
from DataLoader import DataLoader
from two_stage_transfer import TwoStageTransfer
from weka.classifiers import Classifier
import argparse

def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument(
            '--targetpath',
            default = 'target/',
            help = 'the target path'
            )
    parser.add_argument(
            '--sourcepath',
            default = 'source/',
            help = 'the source path'
            )
    parser.add_argument(
            '--evalpath',
            default = 'eval/',
            help = 'the evaluation path'
            )
    parser.add_argument(
            '--savepath',
            default = 'final/',
            help = 'the save path'
            )
    parser.add_argument(
            '--boosting_iter',
            default = 10,
            help = 'the number of boosting iteration'
            )
    parser.add_argument(
            '--max_source',
            default = 15,
            help = 'the max number of source dataset used'
            )
    parser.add_argument(
            '--fold',
            default = 10,
            help = 'the number of k in k-fold validation'
            )
    parser.add_argument(
            '--target_name',
            default = 'quux_blindbot',
            help = 'name of the desired target'
            )
    parser.add_argument(
            '--Datapath',
            default = '/data1/shared/agent_data/',
            help = 'folder where the agent data is saved'
            )
    parser.add_argument(
            '--num_games_target',
            default = 10,
            help = 'number of games from target agent used for training'
            )
    parser.add_argument(
            '--num_games_source',
            default = 100,
            help = 'number of games from each source agent used for training'
            )
    args = parser.parse_args()
    return args

def main():
    args = parse()
    #loading data
    print("**************************************************")
    print("*                 LOADING DATA                   *")
    print("**************************************************")

    data_loader = DataLoader(args.Datapath)
    data_loader.load_target_source_data()
    
    print("**************************************************")
    print("*                 TRAINING                       *")
    print("**************************************************")
    model = Classifier(classname="weka.classifiers.trees.REPTree")
    classifier = TwoStageTransfer(targetpath = args.targetpath,
            sourcepath = args.sourcepath,
            evalpath = args.evalpath,
            savepath = "final/",
            target_name = data_loader.target_name,
            boosting_iter=args.boosting_iter,
            fold=args.fold,
            max_source_dataset=args.max_source,
            model = model)
    classifier.load_data_from_arff()
    classifier.train()

    print("**************************************************")
    print("*                EVALUATING                      *")
    print("**************************************************")
    classifier.evaluate_model()

if __name__== '__main__':
    main()
