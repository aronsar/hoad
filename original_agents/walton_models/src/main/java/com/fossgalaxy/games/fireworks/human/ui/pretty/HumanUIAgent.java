package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by webpigeon on 20/04/17.
 */
public class HumanUIAgent implements Agent {
    private final BlockingQueue<Action> actionList;

    public HumanUIAgent() {
        this.actionList = new ArrayBlockingQueue<>(5);
    }

    public void setMove(Action action) {
        actionList.clear();
        actionList.add(action);
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        Action action = null;

        while (action == null) {
            try {
                action = actionList.take();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        return action;
    }
}
