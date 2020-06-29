package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Chuck away the card in my hand that has the lowest value, prioritizing the card in my hand in the lowest slot.
 *
 * Created by piers on 04/05/17.
 *
 * CliveJ @ BoardGameGeek.com
 */
public class DiscardUnidentifiedCard extends AbstractDiscardRule {

    @Override
    public Action execute(int playerID, GameState state) {
        Hand hand = state.getHand(playerID);

        for(int slot = 0; slot < hand.getSize(); slot++){
            if (!hand.hasCard(slot)) {
                continue;
            }

            if(hand.getKnownColour(slot) == null && hand.getKnownValue(slot) == null){
                return new DiscardCard(slot);
            }
        }
        return null;
    }
}
