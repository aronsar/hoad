for agent_path in ../../../adhoc_agents/maml/result/*
do
    latest_run=`ls ${agent_path} | tail -1`
    agent=`basename ${agent_path}`
    rm $agent
    ln -sf $agent_path/$latest_run ${agent}
done
