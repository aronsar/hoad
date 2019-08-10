package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Find the highest card in my hand I know about and discard it.
 *
 * If I don't know the value of any of my cards, do nothing.
 *
 * Created by webpigeon on 09/05/17.
 */
public class DiscardHighest extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {
        Integer highestSlot = null;
        int highestKnownValue = -1;

        Hand hand = state.getHand(playerID);
        for (int slot=0; slot<hand.getSize(); slot++) {
            Integer knownValue = hand.getKnownValue(slot);

            if (knownValue == null) {
                continue;
            }

            if (knownValue > highestKnownValue) {
                highestKnownValue = knownValue;
                highestSlot = slot;
            }

        }

        if (highestSlot == null) {
            return null;
        }

        return new DiscardCard(highestSlot);
    }

}
