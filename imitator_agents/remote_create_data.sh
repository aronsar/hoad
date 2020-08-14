pcnum=$1
agent=$2
batch_num=$3
num_games=$4

ssh -i ~/.ssh/id_rsa -T aronsar@pc${pcnum}.cs.ucdavis.edu << EOF
    cd hoad/imitator_agents;
    screen -dmS ${agent}${batch_num};
    screen -S ${agent}${batch_num} -X stuff 'source venv/bin/activate\n';
    screen -S ${agent}${batch_num} -X stuff 'python create_imitator_data.py --agent ${agent} --num_games ${num_games} --batch_num ${batch_num}\n'
EOF
