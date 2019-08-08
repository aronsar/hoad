import os
import argparse


def parse():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--mode',
        default='full_gan',
        help='which part of ablation study or baseline to run')

    parser.add_argument(
        '--config_path',
        help='gin config file path'
    )

    parser.add_argument(
        '--data_dir',
        default='./data/')

    parser.add_argument(
        '--evaldatapath',
        help='specify only if evaluating, and data is in unexpected path')

    parser.add_argument(
        '--expert_dir',
        default='./experts')

    parser.add_argument(
        '--agents_to_use',
        nargs='+',
        default=['rainbow'],
        help='Space deliniated list of agents to use. Options are:\n' \
             '    rainbow\n' \
             '    walton-rivers (not yet)\n' \
             '    etc...')

    parser.add_argument('--run_dir')
    parser.add_argument('--checkpoints_dir')
    parser.add_argument('--results_dir')

    parser.add_argument(
        '--output_dir',
        default='./output/')

    parser.add_argument(
        '--new_run',
        action='store_true',
        help="If specified, creates a directory inside the output "
             "directory (specified with --outdir), with a "
             "checkpoint and results directory inside it, plus a "
             "copy of the gin config files. The run ID is the "
             "next available number.")

    args = parser.parse_args()
    args = resolve_run_directory(args)

    return args


def resolve_run_directory(args):
    if args.new_run:
        if not os.path.exists(args.output_dir):
            os.makedirs(args.output_dir)
        new_run_id = get_new_run_id(args.output_dir)
        os.mkdir(os.path.join(args.output_dir, 'run%03d' % new_run_id))
        args.run_dir = os.path.join(args.output_dir, 'run%03d' % new_run_id)
        args.checkpoints_dir = os.path.join(args.output_dir, 'run%03d' % new_run_id, 'checkpoints/')
        args.results_dir = os.path.join(args.output_dir, 'run%03d' % new_run_id, 'results/')
        os.mkdir(args.checkpoints_dir)
        os.mkdir(args.results_dir)
        # TODO: copy over gin config file
        # TODO: save git commit hash in result directory as well
    elif args.checkpoint_dir is None or args.results_dir is None:
        raise ValueError("Please either specify the -newrun flag "
                         "or provide paths for both --ckptdir and --resultdir")
    return args


def resolve_datapath(
        args,
        game_type='Hanabi-Full',
        num_players=2,
        num_unique_agents=6,
        num_games=50):
    if args.datapath == None:
        data_filename = game_type + "_" + str(num_players) + "_" \
                        + str(num_unique_agents) + "_" + str(num_games) + ".pkl"
        args.datapath = os.path.join(args.datadir, data_filename)

    return args


def resolve_configpath(args):
    if args.configpath == None:
        config_filename = args.mode + ".config.gin"
        args.configpath = os.path.join(args.modedir, config_filename)

    return args


def get_new_run_id(output_dir):
    last_run_id = 0
    run_names = [fname for fname in os.listdir(output_dir) if "run" in fname]
    if len(run_names) is not 0:
        run_names.sort(key=lambda f: int(''.join(filter(str.isdigit, f))))
        last_run_id = int(''.join(filter(str.isdigit, run_names[-1])))

    return last_run_id + 1
