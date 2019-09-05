cd /tmp
for f in *.tar.gz; do tar -zxvf "$f"; done
export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:/tmp/cuda/lib64
