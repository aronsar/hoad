package com.fossgalaxy.games.fireworks.ai.rule.simple;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractDiscardRule;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Discard a card we know is 100% safe based on provided information.
 */
public class DiscardIfCertain extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {
        Hand myHand = state.getHand(playerID);
        for (int slot = 0; slot < state.getHandSize(); slot++) {
            if (!myHand.hasCard(slot)) {
                continue;
            }

            CardColour c = myHand.getKnownColour(slot);
            Integer value = myHand.getKnownValue(slot);

            if (c != null) {
                int currValue = state.getTableValue(c);
                if (value != null && currValue >= value) {
                    return new DiscardCard(slot);
                }
            }
        }

        return null;
    }

}
