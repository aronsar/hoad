package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.TimedHand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Discard the oldest (first drawn) card in my hand.
 *
 * Created by webpigeon on 18/10/16.
 */
public class DiscardOldestFirst extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {
        //we can't fire if you didn't use a timed hand
        Hand hand = state.getHand(playerID);
        if (!(hand instanceof TimedHand)) {
            return null;
        }

        TimedHand timedHand = (TimedHand) state.getHand(playerID);
        int slotID = timedHand.getOldestSlot();

        return new DiscardCard(slotID);
    }

}
