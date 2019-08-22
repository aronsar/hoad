package com.fossgalaxy.games.fireworks.state;

import java.io.Serializable;

/**
 * Created by webpigeon on 18/10/16.
 */
public interface Hand extends Serializable {
    /**
     * Initialise all information about the slots.
     * <p>
     * This should reset any known information for any slot in the hand.
     */
    void init();

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
    Card getCard(int slot);

    /**
     * Get the known colour of this slot, from the perspective of the player who's hand it is.
     * <p>
     * For this version of the hand class, this is worked out based on explicitly observed tell actions.
     * It will not take into account negative information.
     *
     * @param slot the slot to query
     * @return the colour of the slot, from the owner's perspective.
     */
    CardColour getKnownColour(int slot);

    /**
     * Get the known value of this slot, from the perspective of the player who's hand it is.
     * <p>
     * For this version of the hand class, this is worked out based on explicitly observed tell actions.
     * It will not take into account negative information.
     *
     * @param slot the slot to query
     * @return the value of the slot, from the owner's perspective.
     */
    Integer getKnownValue(int slot);

    /**
     * The number of cards which make up this hand.
     * <p>
     * This can very with game size, and so this should be used whenever you need to iterate over the number of slots
     * in this player's hand.
     *
     * @return the number of slots in this player's hand.
     */
    int getSize();

    /**
     * Set a card in a given slot, removing all known information about it.
     * <p>
     * This should be called when a {@link com.fossgalaxy.games.fireworks.state.events.CardDrawn} event occurs in order
     * to update this hand with the correct information.
     * <p>
     * It will also reset all known information from the perspective of the owner. If the card is non-null it will also
     * setHasCard correctly.
     *
     * @param slot the slot to update
     * @param card the card now residing in this slot
     */
    void setCard(int slot, Card card);

    /**
     * Bind a card to a given slot, keeping all known information about it.
     * <p>
     * This has the same basic role as {@link #setCard} but will not remove any information from the owners perspective.
     * This is mostly useful when using this class as part of a simulation based agent to set the value of the card.
     *
     * @param slot the slot to update
     * @param card the card to assign to that slot
     */
    void bindCard(int slot, Card card);

    /**
     * Sets the known colour of a slot from the perspective of the owner.
     * <p>
     * This method is used to inform the hand about the card from the owner's perspective (tell actions).
     *
     * @param colour the colour to assign to the slots
     * @param slots  the slots to assign the colour to.
     */
    void setKnownColour(CardColour colour, Integer[] slots);

    /**
     * Sets the known value of a slot from the perspective of the owner.
     * <p>
     * This method is used to inform the hand about the card from the owner's perspective (tell actions).
     *
     * @param value the value to assign to the slots
     * @param slots the slots to assign the value to.
     */
    void setKnownValue(Integer value, Integer[] slots);

    /**
     * Check if this card is possible based on complete infomation.
     *
     * @param slot the slot to check
     * @param card the card we are considering
     * @return true if this card could possibly be in this slot, else return false
     */
    boolean isCompletePossible(int slot, Card card);

    /**
     * check if the card is possible, based on the owner's prespective
     *
     * @param slot the slot to check
     * @param card the card we are considering
     * @return true if this card could possibly fit in this slow, false otherwise.
     */
    boolean isPossible(int slot, Card card);

    /**
     * get possible values for a given slot.
     *
     * @param slot the slot to check
     * @return all possible value for this slot
     */
    int[] getPossibleValues(int slot);

    /**
     * get possible colours for a given slot.
     *
     * @param slot the slot to check
     * @return all possible colour for this slot
     */
    CardColour[] getPossibleColours(int slot);

    /**
     * Has a card of that colour that it knows exactly about
     *
     * @param colour The colour to check
     * @return Wether there is a card of that colour in the hand
     */
    boolean hasColour(CardColour colour);

    /**
     * Has a card of that value that it knows exactly about
     *
     * @param value The value to check
     * @return Wether there is a card of that value in the hand
     */
    boolean hasValue(Integer value);

    /**
     * Does the hand contain a card in this slot?
     * @param slot The slot to check
     * @return Whether there is a card in the slot
     */
    boolean hasCard(int slot);

    /**
     * Sets whether the hand has a card in this slot.
     *
     * This is useful for the own player's hand (card we don't know about but need to know we have).
     *
     * @param slot The slot to set
     * @param value The presence or not of a card
     */
    void setHasCard(int slot, boolean value);
}
