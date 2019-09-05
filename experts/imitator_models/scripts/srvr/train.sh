export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:/tmp/cuda/lib64
python3 /tmp/ganabi/experts/imitator_models/train.py --p /tmp/$1 --m ~/saved_models --epochs 50
