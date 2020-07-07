package com.fossgalaxy.games.fireworks.ai.mcs;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSPredictor;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Flat MC search player
 * <p>
 * Created by piers on 20/12/16.
 */
public class MonteCarloSearch extends MCTSPredictor {

    /**
     * Constructs a new MC Search player with the given policy to use
     *
     * @param policy The policy to use instead of Random rollouts
     */
    @AgentConstructor("flatmc")
    public MonteCarloSearch(Agent policy) {
        super(new Agent[]{policy, policy, policy, policy, policy});
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        return doSuperMove(agentID, state);
    }

    @Override
    protected Action selectActionForRollout(GameState state, int agentID) {
        return agents[agentID].doMove(agentID, state.getCopy());
    }

    @Override
    protected int calculateTreeDepthLimit(GameState state) {
        return 1;
    }

    @Override
    public String toString() {
        return String.format("MCTS(%s)", agents[0].toString());
    }
}