package com.fossgalaxy.games.fireworks.ai.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * A filter agent which will consult its policy with 1-threshold probabilty, i.e. 0.4 means execute policy 60% of the time,
 * execute a random move otherwise.
 *
 * This was created to test the agent's ability to work with noisy/inaccurate policies.
 */
public class NoisyPredictor implements Agent {
    private double threshold;
    private Agent policy;
    private Random random;

    @AgentConstructor("noisy")
    public NoisyPredictor(double threshold, Agent policy) {
        this.threshold = threshold;
        this.policy = policy;
        this.random = new Random();
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        if (random.nextDouble() > threshold) {
            return policy.doMove(agentID, state);
        } else {
            Collection<Action> actions = Utils.generateActions(agentID, state);
            List<Action> listAction = new ArrayList<>(actions);
            return listAction.get(random.nextInt(listAction.size()));
        }
    }

    @Override
    public void receiveID(int agentID, String[] names) {
        policy.receiveID(agentID, names);
    }

    @Override
    public String toString() {
        return "NoisyPredictor{" +
                "threshold=" + threshold +
                ", policy=" + policy +
                ", random=" + random +
                '}';
    }
}
