repo_name=$1
agent_name=$2

mkdir ../replay_data/$repo_name
for i in {0..19}
do
	savedir=../replay_data/$repo_name/$i
	mkdir $savedir
	echo "Creating ${i}th batch of $agent_name data."
	python create_${repo_name}_data.py --num_games 25000 --savedir $savedir --agent_name $agent_name
done
