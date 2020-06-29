from shutil import copy2
import subprocess
import os


def save_gin_config(args):
    copy2(args.config_path, args.run_dir)


def save_git_hash(args):
    git_hash_file_path = os.path.join(args.run_dir, "git_hash.txt")
    git_hash_file = open(git_hash_file_path, "w")
    git_command = ["git", "rev-parse", "HEAD"]
    process = subprocess.Popen(git_command, stdout=git_hash_file)
    process.communicate()


def evaluate_model(data, model, args):
    save_gin_config(args)
    save_git_hash(args)
