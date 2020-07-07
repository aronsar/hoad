package com.fossgalaxy.games.fireworks.ai.osawa.rules;

import com.fossgalaxy.games.fireworks.ai.rule.DiscardSafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.DiscardUselessCard;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Osawa has strange notions of discard and play - this tries to enumerate them.
 * <p>
 * If a card has enough information but is not playable after the turn, this card is defined as a discard-able card.
 * For example, if there is a red fireworks set with the number 4 on top, R1, R2, R3 are discard-able cards. If there
 * is a yellow fireworks set with the number 5 on top, any yellow card is discard-able without needing information
 * about the number.
 */
public class OsawaDiscard implements Rule {

    //we're on track to score max, this rules are good
    private Rule discardSafe;

    //we've already messed up - some (high value) cards are useless.
    private Rule discardUseless;

    public OsawaDiscard() {
        this.discardSafe = new DiscardSafeCard();
        this.discardUseless = new DiscardUselessCard();
    }

    @Override
    public Action execute(int playerID, GameState state) {
        if (discardSafe.canFire(playerID, state)) {
            Action selected = discardSafe.execute(playerID, state);
            if (selected != null) {
                return selected;
            }
        }

        if (discardUseless.canFire(playerID, state)) {
            Action selected = discardUseless.execute(playerID, state);
            if (selected != null) {
                return selected;
            }
        }

        return null;
    }
}
