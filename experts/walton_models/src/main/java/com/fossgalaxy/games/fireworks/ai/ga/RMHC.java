package com.fossgalaxy.games.fireworks.ai.ga;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Created by webpigeon on 24/02/17.
 */
public class RMHC implements Agent {

    @Override
    public Action doMove(int agentID, GameState state) {
        long finishTime = System.currentTimeMillis() + 1000;


        int evals = 0;

        Individual currBest = new Individual(30).randomise();
        double bestScore = currBest.multiEval(state, agentID);

        while (System.currentTimeMillis() < finishTime) {
            Individual newGuy = currBest.copy().mutate();
            double newGuyScore = newGuy.multiEval(state, agentID);

            if (newGuyScore > bestScore) {
                currBest = newGuy;
                bestScore = newGuyScore;
            }

            evals++;
        }

        return currBest.getAction(0, agentID, state);
    }

}
