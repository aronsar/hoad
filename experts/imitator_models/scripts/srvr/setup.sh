cd /tmp
wget https://storage.googleapis.com/ganabi/cudnn-10.0-linux-x64-v7.6.2.24.tgz
wget https://storage.googleapis.com/ganabi/$1.tar.gz
tar -zxvf cudnn-10.0-linux-x64-v7.6.2.24.tgz
tar -zxvf $1.tar.gz
