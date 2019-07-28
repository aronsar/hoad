package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.TimedHand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Discard the oldest (first drawn) card in my hand, but only if I know nothing about.
 *
 * Created by webpigeon on 18/10/16.
 */
public class DiscardOldestNoInfoFirst extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {
        //we can't fire if you didn't use a timed hand
        Hand hand = state.getHand(playerID);
        if (!(hand instanceof TimedHand)) {
            return null;
        }

        TimedHand timedHand = (TimedHand) state.getHand(playerID);

        int oldestNoInfo = Integer.MAX_VALUE;

        for (int slot=0; slot<timedHand.getSize(); slot++) {
            if (!hand.hasCard(slot)) {
                continue;
            }

            int cardAge = timedHand.getAge(slot);
            if ( cardAge > oldestNoInfo ) {
                continue;
            }

            CardColour colour = hand.getKnownColour(slot);
            Integer value = hand.getKnownValue(slot);
            if (colour == null && value == null) {
                oldestNoInfo = slot;
            }
        }

        if (oldestNoInfo == Integer.MAX_VALUE) {
            return null;
        }

        return new DiscardCard(oldestNoInfo);
    }

}
