package com.fossgalaxy.games.fireworks.ai.rule.finesse;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.ai.rule.PlaySafeCard;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by webpigeon on 11/05/17.
 */
public class TellFinesse extends AbstractTellRule {

    @Override
    public boolean canFire(int playerID, GameState state) {
        if(state.getPlayerCount() == 2) return false;
        return super.canFire(playerID, state);
    }

    @Override
    public Action execute(int playerID, GameState state) {
        // if this is a 2 player game, we can't do this move.
        if (state.getPlayerCount() == 2) {
            return null;
        }

        int nextPlayer = selectPlayer(playerID, state);
        int newestSlot = PlayFinesse.getNewestCard(state, nextPlayer);
        if (newestSlot == -1) {
            return null;
        }

        Card newestCard = state.getHand(nextPlayer).getCard(newestSlot);
        if (newestCard == null || !PlaySafeCard.isPlayable(newestCard, state)) {
            return null;
        }


        //finnesse time
        int finessePlayer = selectPlayer(nextPlayer, state);
        Hand finesseHand = state.getHand(finessePlayer);
        Map<CardColour, Integer> cardColours = new EnumMap<>(CardColour.class);
        Map<Integer, Integer> cardValues = new HashMap<>();

        for (int slot=0; slot<finesseHand.getSize(); slot++) {
            Card finesseCard = finesseHand.getCard(slot);
            int currColour = cardColours.getOrDefault(finesseCard.colour, 0);
            cardColours.put(finesseCard.colour, currColour+1);

            int currValue = cardValues.getOrDefault(finesseCard.value, 0);
            cardValues.put(finesseCard.value, currValue+1);
        }


        for (int slot=0; slot<finesseHand.getSize(); slot++) {
            Card finesseCard = finesseHand.getCard(slot);
            if (finesseCard.value.equals(newestCard.value+1) && finesseCard.colour.equals(newestCard.colour)) {

                //step 1 - prioritise unknown information
                if (cardColours.get(finesseCard.colour) == 1 && cardValues.get(finesseCard.value) == 1) {
                    if (finesseHand.getKnownValue(slot) != null) {
                        return new TellColour(finessePlayer, finesseCard.colour);
                    }

                    if (finesseHand.getKnownColour(slot) != null) {
                        return new TellValue(finessePlayer, finesseCard.value);
                    }
                }

                //step 2 - if the value is unique go for that
                if (cardValues.get(finesseCard.value) == 1) {
                    return new TellValue(finessePlayer, finesseCard.value);
                }

                //step 3 - the the colour is unique (but not value) then go for that.
                if (cardColours.get(finesseCard.colour) == 1) {
                    return new TellColour(finessePlayer, finesseCard.colour);
                }


            }
        }

        return null;
    }


}
