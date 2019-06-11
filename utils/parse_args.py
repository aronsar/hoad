import argparse

def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mode', 
                        default='full_model',
                        help='which part of ablation study or baseline to run')

    parser.add_argument('--modedir',
                        default='./modes')

    parser.add_argument('--configpath',
                        help='gin config file path')

    parser.add_argument('--datadir',
                        default='./data/')
    
    parser.add_argument('--expertdir',
                        default='./experts')

    parser.add_argument('--ckptdir')

    parser.add_argument('--resultdir')

    parser.add_argument('--outdir',
                        default='./output/')

    parser.add_argument('-newrun',
                        action='store_true',
                        help="If specified, creates a directory inside the output "
                             "directory (specified with --outdir), with a "       
                             "checkpoint and results directory inside it, plus a "
                             "copy of the gin config files. The run ID is the "   
                             "next available number.")

    args = parser.parse_args()
    return args
