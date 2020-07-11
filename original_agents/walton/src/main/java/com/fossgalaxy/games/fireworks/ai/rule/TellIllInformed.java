package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell a player that does't know about a useful card that a card can be played.
 *
 * Created by webpigeon on 09/05/17.
 */
public class TellIllInformed extends AbstractTellRule {
    @Override
    public Action execute(int playerID, GameState state) {


        for (int i = 0; i < state.getPlayerCount(); i++) {
            int lookingAt = (playerID + i) % state.getPlayerCount();
            if (lookingAt == playerID) {
                continue;
            }

            Action action = getInformingAction(state, lookingAt);
            if (action != null) {
                return action;
            }
        }

        return null;

    }

    public Action getInformingAction(GameState state, int playerID) {
        Hand hand = state.getHand(playerID);

        for (int slot=0; slot<hand.getSize(); slot++) {
            if (!hand.hasCard(slot)) {
                continue;
            }

            Card card = hand.getCard(slot);
            if (!TryToUnBlock.isUsableCard(state, card.colour, card.value)) {
                continue;
            }

            CardColour knownColour = hand.getKnownColour(slot);
            Integer knownValue = hand.getKnownValue(slot);
            if (knownColour != null && knownValue != null) {
                continue;
            }

            return tellMissingPrioritiseValue(hand, playerID, slot);
        }

        return null;
    }

}
