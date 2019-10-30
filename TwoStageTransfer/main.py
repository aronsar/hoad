
from DataLoader import DataLoader
from two_stage_transfer import TwoStageTransfer
from weka.classifiers import Classifier
import argparse

def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument(
            '--target_agent',
            default = 'quux_blindbot',
            help = "Name of the target agent. Choose one of these options: flawed, iggi, legal_random, outer, piers, quux_blindbot, quux_cheatbot, quux_holmesbot, quux_infobot, quux_newcheatbot, quux_simplebot, quux_valuebot, rainbow, van_den_bergh, WTFWT, fireflower"
            )
    parser.add_argument(
            '--targetpath',
            default = 'target/',
            help = 'the path to save target agent data in arff format'
            )
    parser.add_argument(
            '--sourcepath',
            default = 'source/',
            help = 'the path to save source agents data in arff format'
            )
    parser.add_argument(
            '--evalpath',
            default = 'eval/',
            help = 'the path to save target data used for testing the model'
            )
    parser.add_argument(
            '--savepath',
            default = 'final/',
            help = 'the path to save the data after train internal. With this file, the final can be trained immediately'
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

    data_loader = DataLoader(args.Datapath, args.target_agent)
    data_loader.load_target_source_data()
    
    print("**************************************************")
    print("*                 TRAINING                       *")
    print("**************************************************")
    model = Classifier(classname="weka.classifiers.trees.REPTree")
    classifier = TwoStageTransfer(targetpath = args.targetpath,
            sourcepath = args.sourcepath,
            evalpath = args.evalpath,
            savepath = args.savepath,
            target_name = data_loader.target_agent,
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
