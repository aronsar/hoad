# bash csif_setup.sh [username] [pc]
ssh $1@pc$2.cs.ucdavis.edu "pip3 install tensorflow-gpu==2.0.0-beta1 h5py_cache"
