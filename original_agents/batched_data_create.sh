repo_name=$1
agent_name=$2
batch_size=25000


mkdir ../replay_data/$repo_name
for i in {0..19}
do
    let total_games=20*$batch_size
	savedir=../replay_data/${repo_name}_data_2_${total_games}/$i
	mkdir $savedir
	echo "Creating ${i}th batch of $agent_name data."
	python create_${repo_name}_data.py --num_games $batch_size --savedir $savedir --agent_name $agent_name
done
