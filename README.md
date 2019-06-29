# ganabi

Because DeepMind wrote their Rainbow agents in Py 2.7 and tf 1.x, the data creation script, which interfaces with that code, uses Py 2.7 and tf 1.x. However, once the data is produced, we only use Py 3.6 and tf 2.0 for building and training our models.

### Getting Started:
```
fork/clone repo into your home folder
cd ~/ganabi/hanabi-env
cmake .
make
cd ..
source /data1/shared/venvg2/bin/activate # use venvg for python 3 
mkdir data # FIXME: should get created if it doesn't exist
python create_data.py
```
