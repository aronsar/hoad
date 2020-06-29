package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell a player about a 5 that they don't know about.
 *
 * Created by piers on 05/05/17.
 */
public class TellFives extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        for(int i = 0; i < state.getPlayerCount(); i++){
            int lookingAt = (playerID + i) % state.getPlayerCount();
            if(lookingAt == playerID) continue;

            // Does this player have any un-identified 5
            Hand hand = state.getHand(lookingAt);
            int indexOfFive = HandUtils.hasUnidentifiedCard(hand, 5);
            if(indexOfFive != -1){
                return new TellValue(lookingAt, 5);
            }
        }
        return null;
    }
}
