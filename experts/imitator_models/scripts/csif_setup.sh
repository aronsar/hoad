# Use this script when /tmp has been reset on the CSIF machine

# bash .sh [username] [pc] [name-of-data-file.tar.gz]
ssh $1@pc$2.cs.ucdavis.edu "cd /tmp/"\
"&& git clone https://github.com/3tz/ganabi.git"\
"&& bash ganabi/experts/imitator_models/scripts/srvr/setup.sh $3"
