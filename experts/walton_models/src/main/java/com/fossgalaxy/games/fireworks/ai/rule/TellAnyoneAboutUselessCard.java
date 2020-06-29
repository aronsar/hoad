package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell a player about a card in their hand we know is not needed.
 *
 * Created by piers on 12/12/16.
 */
public class TellAnyoneAboutUselessCard extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();
            Hand hand = state.getHand(nextPlayer);
            if (nextPlayer == playerID) {
                continue;
            }

            for (int slot = 0; slot < state.getHandSize(); slot++) {
                if (!hand.hasCard(slot)) {
                    continue;
                }
                if (HandUtils.isSafeToDiscard(state, nextPlayer, slot)) {
                    return tellMissingPrioritiseValue(hand, nextPlayer, slot);
                }
            }
        }
        return null;
    }
}
