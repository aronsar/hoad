# Imitator Models

A directory with everything related to constructing, training, and testing imitator models. It is expected that data for the original agents has already been saved to `../replay_data` with the file-tree structure used by  `../original_agents/batched_create_data.sh` (namely, each agent must have sub-folders containing smaller batches of data).

## Setup and Training Workflow

```
cd hoad/imitator_agents
virtualenv venv3 -p python3
source venv3/bin/activate
pip install tensorflow h5py_cache cffi keras # tested with tensorflow 2.2

python train.py --datadir ../replay_data/iggi_data_2_500000 \
			    --savedir saved_models \
			    --num_workers 4 \
			    --queue_size 5 \ 
			    --epochs 5
```

Run `python train.py --h` for details on the command line options. 

## Training Workflow using CSIF:
Note: these instructions are meant for the CSIF computer lab at UC Davis, but may prove valuable for adapting this repository to parallel training across multiple VMs/computers at other institutions as well. Additionally there are some good notes under "Possible common errors."
### Important things regarding CSIF

1. Every user has a disk quota of 2 gB.
2. Only files under `~/` counts towards this quota.
3. Everything under `~/` is shared across all CSIF machines.
4. There's no limit on how much a user can store under`/tmp`.
5. **`/tmp` is reset daily.**
5. **`/tmp` is NOT shared across all CSIF machines.**
7. PCs 1-20 do not have GPUs.
8. Status of all machines can be checked [here](http://iceman.cs.ucdavis.edu/nagios3/cgi-bin/status.cgi?hostgroup=all).

### Setup
For demonstration purposes, let's assume that our current working directory is inside this repository at `~/hoad/imitator_agents/scripts`, the data we will be training on is `iggi_data_2_500000.tar.gz`. The CUDA's installation package and the training data are stored on Google Cloud at https://console.cloud.google.com/storage/browser/ganabi (Thanks to Aron's guide on how to setup CUDA on CSIF).

1. Clone this repository with`git clone https://github.com/aronsar/hoad.git`.
2. This step is optional, but it is strongly recommended for users to setup keyless login on CSIF. Otherwise, users will have to type passwords multiple times during the following steps.
3. Clean up your disk on CSIF. A user only has a quota of 2GB, but TF 2.0 will take ~1.3GB. The remaining space will be used to save checkpoints during training.
4. Once it's cleaned, install the required packages for training the models with `bash csif_install_pkgs.sh {username} {pc#}` where `username` is users' KERBROS login ID & `pc#`
 is an integer indicating the PC to use. Reminder: PC 1-20 do not have GPUs, so use others if possible, and whether a machine has GPU can be checked with `nvidia-smi`.

### Training for the first time

1. Upload the data and CUDA toolkit to CSIF under `/tmp` with `bash csif_setup.sh {username} {pc#} {name-of-data-file.tar.gz}`.
2. Now training can be started with `bash csif_train.sh {username} {pc#} {directory name of data}`. In our case,  {directory name of data} is `iggi_data_2_500000`. Checkpoints are saved under `~/saved_models/{agent_name}.save`

### Resuming Training
1. `/tmp` is reset daily. Thus, `csif_setup.sh` has to be run if `/tmp` has been reset. Once making sure all the key files created by `csif_setup.sh` exist under `/tmp`, proceed to the next step.
2. Training is automatically resumed if `hoad/imitator_agents/saved_models/{agent_name}.save` is found and not corrupted (see `train.py` for details about corrupted model). Thus, simply run `bash csif_train.sh {username} {pc#} {directory name of data}` to continue training.

### Sample code

```
# install packages. This only needs to be run once for each user.
$ bash csif_install_pkgs.sh myID 48
...
# upload dataset to /tmp/. This needs to be run daily after /tmp/ is cleaned.
$ bash csif_setup.sh myID 48 iggi_data_2_500000
...
# train (or resume training) the model with the uploaded data
$ bash csif_train.sh myID 48 iggi_data_2_500000

```

### Possible Common Errors

#### ValueError related to corrupted model

This is due to unexpected alterations to the `.save` directory. The most common reason is that training was stopped before the end of the first epoch. In this case, simply removing the corresponding `.save` directory resolves the problem.

#### GPU memory error from tensorflow/CUDA

This is likely due to an interruption to the training process that terminates only the python process but not the subprocesses that are still using the GPU. In this case, first log into the corresponding CSIF machine and type `fuser -v /dev/nvidia*`. You should be able to see the currently running processes that are using the GPU. Simply terminate them with `kill -9 {pid}`. If you cannot kill the process, it means it's being used by another user, and you should consider using another machine.

#### RAM allocation error

There's an issue related to Keras's `fit_generator` where using multiprocessing can lead to unexpected memory leak. This error shouldn't happen on CSIF with current version of the code, but it does occur on other machines. If this somehow still happens rarely on CSIF, simply resume training by following the steps above. If this happens frequently on CSIF, turn off multiprocessing for `fit_generator()`.

#### Quota error

You have stored more than 2 gB under `~/`. Checkpoints are saved after every single epoch. For 50 epochs, the checkpoints should take ~270 mB. To avoid having checkpoint files filling up the disk on CSIF, consider manually moving some checkpoints from early epochs under `*.save/ckpts/` to your local machine. However, DO NOT REMOVE/MODIFY THE LATEST CHECKPOINT FILE or a ValueError will be raised.
