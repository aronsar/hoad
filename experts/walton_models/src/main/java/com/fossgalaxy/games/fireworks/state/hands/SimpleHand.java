package com.fossgalaxy.games.fireworks.state.hands;

import com.fossgalaxy.games.fireworks.state.CardColour;

/**
 * Created by webpigeon on 18/10/16.
 */
public interface SimpleHand {

    int getSize();

    CardColour[] getColours(int slot);

    Integer[] getValues(int slot);

    boolean isPossible(int slot, CardColour colour);

    boolean isPossible(int slot, Integer value);

    default boolean isPossible(int slot, CardColour cardColour, Integer value) {
        return isPossible(slot, cardColour) && isPossible(slot, value);
    }

}
