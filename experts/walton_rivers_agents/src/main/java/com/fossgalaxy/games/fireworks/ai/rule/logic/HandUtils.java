package com.fossgalaxy.games.fireworks.ai.rule.logic;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by piers on 01/12/16.
 */
public class HandUtils {

    private final static int[] NUM_CARDS = {0, 3, 2, 2, 2, 1};

    private HandUtils() {

    }

    /**
     * Calculates the maximum possible score obtainable for the given colour
     *
     * @param state  The game state
     * @param colour The colour to look at
     * @return The maximum possible score
     */
    public static int getHighestScorePossible(GameState state, CardColour colour) {
        int nextValue = state.getTableValue(colour) + 1;

        Collection<Card> discards = state.getDiscards();
        for (int i = nextValue; i <= 5; i++) {
            int occurrences = Collections.frequency(discards, new Card(i, colour));

            //if all of this value have already been discarded, the previous value is the highest
            if (occurrences == NUM_CARDS[i]) {
                return i - 1;
            }

        }

        //if we didn't find any doomed combinations, we're probably still good
        return 5;
    }

    /**
     * Returns true if the card is safe to discard because it is higher than what is currently possible for its colour
     * <p>
     * I.e if all the Blue 2's have been discarded and none played, then any Blue 3, 4 or 5 is safe to discard
     *
     * @param state The game state
     * @param colour The colour of the card to consider
     * @param value The value of the card to consider
     * @return Can we safely discard it for this reason?
     */
    public static boolean isSafeToDiscardHigherThanPossible(GameState state, CardColour colour, Integer value) {
        return (value != null  && colour != null && getHighestScorePossible(state, colour) < value);
    }


    /**
     * Returns true if a given card is safe to discard. Works on what the player has, not what they know
     *
     * @param state  The game state
     * @param player The player
     * @param slot   The slot
     * @return Whether it is discardable
     */
    public static boolean isSafeToDiscard(GameState state, int player, int slot) {
        Hand hand = state.getHand(player);
        Card card = hand.getCard(slot);
        return card != null && isSafeToDiscard(state, card.colour, card.value);
    }

    public static boolean isSafeToDiscard(GameState state, CardColour c, Integer value) {
        return (
                isSafeBecauseFiveAlreadyPlayed(state, c)
                        || isSafeBecauseValueLowerThanPlayed(state, c, value)
                        || isSafeBecauseValueLowerThanMinOnTable(state, value)
                        || isSafeToDiscardHigherThanPossible(state, c, value)
        );
    }

    public static boolean isSafeBecauseFiveAlreadyPlayed(GameState state, CardColour c) {
        return c != null && state.getTableValue(c) == 5;
    }

    public static boolean isSafeBecauseValueLowerThanPlayed(GameState state, CardColour c, Integer value) {
        return (c != null && value != null) && (state.getTableValue(c) >= value);
    }

    public static boolean isSafeBecauseValueLowerThanMinOnTable(GameState state, Integer value) {
        return (value != null && value <= getMinTableValue(state));
    }

    /**
     * Will return the index of a card that matches the rule
     *
     * @param hand the hand we are checking against
     * @param value The value we are looking for
     * @return index of the card or -1 if there isn't one
     */
    public static int hasUnidentifiedCard(Hand hand, int value){
        for(int slot = 0; slot < hand.getSize(); slot++){
            if(hand.hasCard(slot)) {
                if (hand.getKnownValue(slot) == null && hand.getCard(slot).value == value) {
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Returns true if a given card is safe to discard. Works on what the player knows, not what they have
     *
     * @param state  The game state
     * @param player The player
     * @param slot   The slot
     * @return Whether it is discardable
     */
    public static boolean knowsItIsSafeToDiscard(GameState state, int player, int slot) {
        Hand myHand = state.getHand(player);
        return isSafeToDiscard(state, myHand.getKnownColour(slot), myHand.getKnownValue(slot));
    }

    public static Integer[] getTableScores(GameState state) {
        return Arrays.stream(CardColour.values())
                .map(state::getTableValue)
                .toArray(Integer[]::new);
    }

    public static int getMinTableValue(GameState state) {
        return Arrays.stream(CardColour.values())
                .map(state::getTableValue)
                .mapToInt(Integer::intValue)
                .min().getAsInt();
    }
}
