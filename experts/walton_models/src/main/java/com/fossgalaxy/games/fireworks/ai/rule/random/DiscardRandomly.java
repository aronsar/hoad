package com.fossgalaxy.games.fireworks.ai.rule.random;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractDiscardRule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiscardRandomly extends AbstractDiscardRule {
    private Random random;

    public DiscardRandomly() {
        this.random = new Random();
    }

    @Override
    public Action execute(int playerID, GameState state) {

        Hand hand = state.getHand(playerID);
        List<Integer> possibleCards = new ArrayList<>();
        for (int slot=0; slot<hand.getSize(); slot++) {
            if (hand.hasCard(slot)) {
                possibleCards.add(slot);
            }
        }

        if (possibleCards.isEmpty()) {
            return null;
        }

        return new DiscardCard(possibleCards.get(random.nextInt(possibleCards.size())));
    }

}
