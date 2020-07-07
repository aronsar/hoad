package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

/**
 * Created by webpigeon on 12/12/16.
 */
public class RuleUtils {

   public static void setHasCards(GameState state) {
       for (int player = 0; player < state.getPlayerCount(); player++) {
            setHasCards(state.getHand(player));
       }
   }


    public static void setHasCards(Hand hand) {
        for (int slot = 0; slot < hand.getSize(); slot++) {
            hand.setHasCard(slot, true);
        }
    }
}
