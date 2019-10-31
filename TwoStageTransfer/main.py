
from DataLoader import DataLoader
from two_stage_transfer import TwoStageTransfer
from weka.classifiers import Classifier
import argparse
import weka.core.jvm as jvm

def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument(
            '--target_agent',
            type=str,
            default = 'quux_blindbot',
            help = "Name of the target agent. Choose one of these options: flawed, iggi, legal_random, outer, piers, quux_blindbot, quux_cheatbot, quux_holmesbot, quux_infobot, quux_newcheatbot, quux_simplebot, quux_valuebot, rainbow, van_den_bergh, WTFWT, fireflower"
            )
    parser.add_argument(
            '--targetpath',
            type=str,
            default = 'target/',
            help = 'the path to save target agent data in arff format'
            )
    parser.add_argument(
            '--sourcepath',
            type=str,
            default = 'source/',
            help = 'the path to save source agents data in arff format'
            )
    parser.add_argument(
            '--evalpath',
            type=str,
            default = 'eval/',
            help = 'the path to save target data used for testing the model'
            )
    parser.add_argument(
            '--savepath',
            type=str,
            default = 'final/',
            help = 'the path to save the data after train internal. With this file, the final can be trained immediately'
            )
    parser.add_argument(
            '--boosting_iter',
            type=int,
            default = 10,
            help = 'the number of boosting iteration'
            )
    parser.add_argument(
            '--max_source',
            type=int,
            default = 15,
            help = 'the max number of source dataset used'
            )
    parser.add_argument(
            '--fold',
            type=int,
            default = 10,
            help = 'the number of k in k-fold validation'
            )
    parser.add_argument(
            '--Datapath',
            type=str,
            default = '/data1/shared/agent_data/',
            help = 'folder where the agent data is saved'
            )
    parser.add_argument(
            '--num_games_target',
            type=int,
            default = 1,
            help = 'number of games from target agent used for training'
            )
    parser.add_argument(
            '--num_games_source',
            type=int,
            default = 10,
            help = 'number of games from each source agent used for training'
            )
    parser.add_argument(
            '--num_games_eval',
            type=int,
            default = 1,
            help = 'number of games from target agent used for evaluation'
            )
    args = parser.parse_args()
    return args

def main():
    jvm.start()
    args = parse()
    #loading data
    print("**************************************************")
    print("*                 LOADING DATA                   *")
    print("**************************************************")

    data_loader = DataLoader(datapath = args.Datapath, 
            target_name = args.target_agent,
            sourcepath = args.sourcepath,
            targetpath = args.targetpath,
            evalpath = args.evalpath)
    data_loader.load_target_source_data()
    
    print("**************************************************")
    print("*                 TRAINING                       *")
    print("**************************************************")
    model = Classifier(classname="weka.classifiers.trees.REPTree")
    classifier = TwoStageTransfer(targetpath = args.targetpath,
            sourcepath = args.sourcepath,
            evalpath = args.evalpath,
            savepath = args.savepath,
            target_name = args.target_agent,
            num_target = args.num_games_target,
            num_source = args.num_games_source,
            num_eval = args.num_games_eval,
            boosting_iter=args.boosting_iter,
            fold=args.fold,
            max_source_dataset=args.max_source,
            model = model)
    classifier.load_data_from_arff()
    classifier.train()

    print("**************************************************")
    print("*                EVALUATING                      *")
    print("**************************************************")
    print("Evaluate for ", args.target_agent)
    classifier.evaluate_model()
    
    jvm.stop()
if __name__== '__main__':
    main()
