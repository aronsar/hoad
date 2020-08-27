
for agent_path in ../../../adhoc_agents/maml/result/*
do
    echo $agent_path
    latest_run=`ls -rt ${agent_path} | tail -1`
    echo $latest_run
    weights_path="${agent_path}/${latest_run}/weights"
    last_weight=`ls -rt ${weights_path} | tail -1`
    agent=`basename ${agent_path}`
    mkdir $agent
    ln -sf ../$weights_path/$last_weight ${agent}/${agent}.h5
done
