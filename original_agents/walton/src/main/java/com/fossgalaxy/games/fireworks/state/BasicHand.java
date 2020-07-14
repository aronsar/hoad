package com.fossgalaxy.games.fireworks.state;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An implementation of a hand.
 * <p>
 * This only keeps track of only explicitly told information (performs no deduction)
 */
public class BasicHand implements Iterable<Card>, Hand {
    private final int size;
    private final CardColour[] colours;
    private final Integer[] values;
    private final Card[] cards;
    private final boolean[] hasCards;

    /**
     * Create a deep copy of a given hand.
     *
     * @param hand the hand to copy
     */
    public BasicHand(BasicHand hand) {
        this.size = hand.size;
        this.colours = Arrays.copyOf(hand.colours, size);
        this.values = Arrays.copyOf(hand.values, size);
        this.cards = Arrays.copyOf(hand.cards, size);
        this.hasCards = Arrays.copyOf(hand.hasCards, size);
    }

    /**
     * Create a new blank hand.
     * <p>
     * When initialised, all card slots will be blank (have no card present).
     *
     * @param size the number of cards in this hand
     */
    public BasicHand(int size) {
        this.size = size;
        this.colours = new CardColour[size];
        this.values = new Integer[size];
        this.cards = new Card[size];
        this.hasCards = new boolean[size];
    }

    /**
     * Initialise all information about the slots.
     * <p>
     * This should reset any known infomation for any slot in the hand.
     */
    @Override
    public void init() {
        for (int i = 0; i < size; i++) {
            clear(i);
        }
    }

    /**
     * Reset all information about a slot.
     * <p>
     * This resets all known information about the slot to be blank (we know nothing about the slot).
     */
    void clear(int slot) {
        cards[slot] = null;
        values[slot] = null;
        colours[slot] = null;
    }

    /**
     * Get the card really present in the slot (or null if unknown).
     * <p>
     * This is the card which is present in the slot based on information provided by the game itself. This method
     * should always return null for your own hand (agents should not call this method on their own hands).
     * <p>
     * Agents can use this for accurate information about other player's hands.
     *
     * @param slot the slot to query
     * @return the card present in that slot, or null if the card is unknown.
     */
    @Override
    public Card getCard(int slot) {
        return cards[slot];
    }

    /**
     * Get the known colour of this slot, from the perspective of the player who's hand it is.
     * <p>
     * For this version of the hand class, this is worked out based on explicitly observed tell actions.
     * It will not take into account negative information.
     *
     * @param slot the slot to query
     * @return the colour of the slot, from the owner's perspective.
     */
    @Override
    public CardColour getKnownColour(int slot) {
        return colours[slot];
    }

    /**
     * Get the known value of this slot, from the perspective of the player who's hand it is.
     * <p>
     * For this version of the hand class, this is worked out based on explicitly observed tell actions.
     * It will not take into account negative information.
     *
     * @param slot the slot to query
     * @return the value of the slot, from the owner's perspective.
     */
    @Override
    public Integer getKnownValue(int slot) {
        return values[slot];
    }

    /**
     * The number of cards which make up this hand.
     * <p>
     * This can very with game size, and so this should be used whenever you need to iterate over the number of slots
     * in this player's hand.
     *
     * @return the number of slots in this player's hand.
     */
    @Override
    public int getSize() {
        return size;
    }

    /**
     * Create an iterator of the cards, based on the real cards present.
     * <p>
     * This uses the same values as {@link #getCard(int)}.
     *
     * @return a card iterator based on the getCard function
     */
    @Override
    public Iterator<Card> iterator() {
        return Arrays.asList(cards).iterator();
    }

    /**
     * Set a card in a given slot, removing all known information about it.
     * <p>
     * This should be called when a {@link com.fossgalaxy.games.fireworks.state.events.CardDrawn} event occurs in order
     * to update this hand with the correct information.
     * <p>
     * It will also reset all known information from the perspective of the owner.
     *
     * @param slot the slot to update
     * @param card the card now residing in this slot
     */
    @Override
    public void setCard(int slot, Card card) {
        clear(slot);
        cards[slot] = card;
        hasCards[slot] = (card != null);
    }

    /**
     * Bind a card to a given slot, keeping all known information about it.
     * <p>
     * This has the same basic role as {@link #setCard} but will not remove any information from the owners perspective.
     * This is mostly useful when using this class as part of a simulation based agent to set the value of the card.
     *
     * @param slot the slot to update
     * @param card the card to assign to that slot
     */
    @Override
    public void bindCard(int slot, Card card) {
        cards[slot] = card;
    }

    /**
     * Sets the known colour of a slot from the perspective of the owner.
     * <p>
     * This method is used to inform the hand about the card from the owner's perspective (tell actions).
     *
     * @param colour the colour to assign to the slots
     * @param slots  the slots to assign the colour to.
     */
    @Override
    public void setKnownColour(CardColour colour, Integer[] slots) {
        for (Integer slot : slots) {
            assert colours[slot] == null || colours[slot].equals(colour) : "told about contradictory colours: " + colours[slot] + " " + colour;
            colours[slot] = colour;
        }
    }

    /**
     * Sets the known value of a slot from the perspective of the owner.
     * <p>
     * This method is used to inform the hand about the card from the owner's perspective (tell actions).
     *
     * @param value the value to assign to the slots
     * @param slots the slots to assign the value to.
     */
    @Override
    public void setKnownValue(Integer value, Integer[] slots) {
        for (Integer slot : slots) {
            assert values[slot] == null || values[slot].equals(value) : "told about contradictory values for " + slot + ": " + values[slot] + " " + value;
            values[slot] = value;
        }
    }

    /**
     * Pretty print this hand.
     *
     * @return a string representation of the hand.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < size; i++) {
            buf.append(colours[i]);
            buf.append(" ");
            buf.append(values[i]);

            if (i < size - 1) {
                buf.append(", ");
            }
        }

        return "I think I have: " + buf.toString();
    }

    /**
     * Check if this card is possible based on complete infomation.
     *
     * @param slot the slot to check
     * @param card the card we are considering
     * @return true if this card could possibly be in this slot, else return false
     */
    @Override
    public boolean isCompletePossible(int slot, Card card) {

        if (cards[slot] != null) {
            return cards[slot].equals(card);
        }

        return isPossible(slot, card);
    }

    /**
     * check if the card is possible, based on the owner's prespective
     *
     * @param slot the slot to check
     * @param card the card we are considering
     * @return true if this card could possibly fit in this slow, false otherwise.
     */
    @Override
    public boolean isPossible(int slot, Card card) {
        boolean possibleColour = colours[slot] == null || colours[slot].equals(card.colour);
        boolean possibleValue = values[slot] == null || values[slot].equals(card.value);
        return hasCard(slot) && possibleColour && possibleValue;
    }

    /**
     * get possible values for a given slot.
     *
     * @param slot the slot to check
     * @return all possible value for this slot
     */
    @Override
    public int[] getPossibleValues(int slot) {
        Integer value = getKnownValue(slot);
        if (value == null) {
            return new int[]{1, 2, 3, 4, 5};
        }
        return new int[]{value};
    }

    /**
     * get possible colours for a given slot.
     *
     * @param slot the slot to check
     * @return all possible colour for this slot
     */
    @Override
    public CardColour[] getPossibleColours(int slot) {
        CardColour colour = getKnownColour(slot);
        if (colour == null) {
            return CardColour.values();
        }
        return new CardColour[]{colour};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicHand cards1 = (BasicHand) o;

        if (size != cards1.size) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(colours, cards1.colours)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(values, cards1.values)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(cards, cards1.cards);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + size;
        result = 31 * result + Arrays.hashCode(colours);
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(cards);
        return result;
    }

    @Override
    public boolean hasColour(CardColour colour) {
        for (Card c : cards) {
            if (c != null && colour.equals(c.colour)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasValue(Integer value) {
        for (Card c : cards) {
            if (c != null && value.equals(c.value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCard(int slot) {
        return slot < size && hasCards[slot];
    }

    @Override
    public void setHasCard(int slot, boolean value) {
        if(slot < size){
            hasCards[slot] = value;
        }
    }
}
