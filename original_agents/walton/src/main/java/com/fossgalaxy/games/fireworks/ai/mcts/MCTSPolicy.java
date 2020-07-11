package com.fossgalaxy.games.fireworks.ai.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A version of the MCTS agent that replaces the random rollout with policy based roll-outs.
 *
 * This variant uses standard MCTS for all agent's moves in the tree, and then uses the policy for roll-outs.
 */
public class MCTSPolicy extends MCTS {
    private final Logger LOG = LoggerFactory.getLogger(MCTSPolicy.class);
    private final Agent rolloutPolicy;

    public MCTSPolicy(Agent rolloutPolicy) {
        this.rolloutPolicy = rolloutPolicy;
    }

    @AgentConstructor("mctsPolicy")
    public MCTSPolicy(int roundLength, int rolloutDepth, int treeDepthMul, Agent rollout) {
        super(roundLength, rolloutDepth, treeDepthMul);
        this.rolloutPolicy = rollout;
    }

    @AgentBuilderStatic("mctsPolicyND")
    public static MCTSPolicy buildPolicyND(Agent rolloutPolicy) {
        return new MCTSPolicy(MCTS.DEFAULT_ITERATIONS, MCTS.NO_LIMIT, MCTS.NO_LIMIT, rolloutPolicy);
    }

    /**
     * Rather than perform a random move, query a policy for one.
     *
     * Consult the policy provided when creating the agent for all agent's moves.
     *
     * @param state the current game state
     * @param playerID the current player ID
     * @return the move that the policy has selected
     */
    @Override
    protected Action selectActionForRollout(GameState state, int playerID) {
        try {
            return rolloutPolicy.doMove(playerID, state);
        } catch (IllegalArgumentException ex) {
            LOG.error("warning, agent failed to make move: {}", ex);
            return super.selectActionForRollout(state, playerID);
        }
    }

    @Override
    public String toString() {
        return String.format("policyMCTS(%s)", rolloutPolicy);
    }
}
