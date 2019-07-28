package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

/**
 * Discards a card that is safe - It is safe to discard if it cannot be played
 * <p>
 * Created by webpigeon on 18/10/16.
 */
public class DiscardSafeCard extends AbstractDiscardRule {
    @Override
    public Action execute(int playerID, GameState state) {
        for (int slot = 0; slot < state.getHandSize(); slot++) {
            if (state.getHand(playerID).hasCard(slot) && HandUtils.knowsItIsSafeToDiscard(state, playerID, slot)) {
                return new DiscardCard(slot);
            }
        }

        return null;
    }
}
