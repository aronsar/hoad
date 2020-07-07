package com.fossgalaxy.games.fireworks.ai.iggi;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class LegalRandom extends AbstractRule {
    private Random random;

    public LegalRandom() {
        this.random = new Random();
    }

    @Override
    public Action execute(int playerID, GameState state) {

        Collection<Action> actions = Utils.generateActions(playerID, state);
        List<Action> listAction = new ArrayList<>(actions);

        return listAction.get(random.nextInt(listAction.size()));
    }

}
