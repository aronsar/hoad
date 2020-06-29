package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell the next player about an unknown card.
 */
public class TellUnknown extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        int nextAgent = selectPlayer(playerID, state);
        Hand otherHand = state.getHand(nextAgent);

        for (int slot = 0; slot < state.getHandSize(); slot++) {
            Card card = otherHand.getCard(slot);
            if (card == null) {
                continue;
            }

            Action expectedAction = tellMissingPrioritiseColour(otherHand, nextAgent, slot);
            if (expectedAction != null) {
                return expectedAction;
            }
        }

        return null;
    }

}
