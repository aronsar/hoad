package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell the next player something about their ones if they don't know already.
 */
public class TellAboutOnes extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        int nextPlayer = (playerID + 1) % state.getPlayerCount();
        Hand hand = state.getHand(nextPlayer);

        for (int slot = 0; slot < state.getHandSize(); slot++) {

            Card card = hand.getCard(slot);
            if (card == null || card.value != 1) {
                continue;
            }

            Action toDo = tellMissingPrioritiseValue(hand, nextPlayer, slot);
            if (toDo != null) {
                return toDo;
            }
        }

        return null;
    }

}
