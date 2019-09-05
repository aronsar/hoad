cd /tmp
for f in *.tar.gz; do tar -zxvf "$f"; done
tar -zxvf cudnn-10.0-linux-x64-v7.6.2.24.tgz
export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:/tmp/cuda/lib64
