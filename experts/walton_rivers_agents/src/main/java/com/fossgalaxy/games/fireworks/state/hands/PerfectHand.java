package com.fossgalaxy.games.fireworks.state.hands;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;

import java.util.Arrays;

/**
 * This hand ignores all information, except for cards which have been given to it via the game engine.
 */
public class PerfectHand implements SimpleHand {
    private final Card[] cards;

    public PerfectHand(PerfectHand hand) {
        this.cards = Arrays.copyOf(hand.cards, hand.getSize());
    }

    public PerfectHand(int size) {
        this.cards = new Card[size];
    }

    public boolean isPossible(int slot, Integer value) {
        return cards[slot] == null || value.equals(cards[slot].value);
    }

    @Override
    public int getSize() {
        return cards.length;
    }

    @Override
    public CardColour[] getColours(int slot) {
        if (cards[slot] == null) {
            return CardColour.values();
        } else {
            return new CardColour[]{cards[slot].colour};
        }
    }

    @Override
    public Integer[] getValues(int slot) {
        if (cards[slot] == null) {
            return new Integer[]{1, 2, 3, 4, 5};
        } else {
            return new Integer[]{cards[slot].value};
        }
    }

    public boolean isPossible(int slot, CardColour colour) {
        return cards[slot] == null || colour.equals(cards[slot].colour);
    }

}
