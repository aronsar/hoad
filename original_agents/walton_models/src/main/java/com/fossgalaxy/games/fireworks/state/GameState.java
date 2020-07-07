package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public interface GameState extends Serializable {

    /**
     * Initialise the game, using a random deck ordering.
     */
    void init();

    /**
     * Initialise the game, using seed as the order to shuffle the deck into.
     *
     * @param seed the shuffling order
     */
    void init(Long seed);

    /**
     * Add a copy of the card mentioned to the discard pile.
     *
     * @param card the card to add to the discard list
     */
    void addToDiscard(Card card);


    /**
     * Draw the top card from the deck
     *
     * @return the card that was on the top of the deck.
     */
    Card drawFromDeck();

    /**
     * The card contained within a players hand at a given slot.
     *
     * This will return null for the players hand in a partially observable state as we cannot see our own cards.
     *
     * @param player the player to query
     * @param slot the slot to query
     * @return the card in the slot, or null if no card is present.
     */
    Card getCardAt(int player, int slot);

    /**
     * Get the cards remaining in the deck.
     *
     * For partially observable states, the deck includes all cards we haven't seen yet (ie, this will include cards
     * in our hand). When determinising, these should be removed from the deck and placed into the correct place
     * in the player's hand.
     *
     * @return the deck object
     */
    Deck getDeck();

    /**
     * Clone the game state.
     *
     * @return a deep copy of the game state
     */
    GameState getCopy();

    /**
     * Deal a hand to a player.
     *
     * @param playerID the player to deal to
     */
    void deal(int playerID);

    /**
     * Get all discarded cards.
     *
     * @return this is the cards that the players have discarded.
     */
    Collection<Card> getDiscards();

    /**
     * Get the hand of a player.
     *
     * @param player the player to query
     * @return the cards the player is holding in their hand.
     */
    Hand getHand(int player);

    /**
     * Get the number of cards each player has in their hand.
     *
     * At the end of the game, some of the slots may be null, but there should always be this many slots in a player's
     * hand.
     *
     * @return The number of cards each player has in their hand.
     */
    int getHandSize();

    /**
     * Get the current number of information tokens.
     *
     * @return the number of information tokens the players have left.
     */
    int getInfomation();

    default int getInformation(){
        return getInfomation();
    }

    /**
     * Get the number of lives remaining.
     *
     * @return the number of lives remaining, 0 indicates the game is over
     */
    int getLives();

    /**
     * Update lives remaining.
     *
     * When this is zero, the game ends.
     *
     * @param newValue the number of lives remaining.
     */
    void setLives(int newValue);

    /**
     * Get the number of players in this game.
     *
     * @return the number of players in the game
     */
    int getPlayerCount();

    /**
     * Return the current score for this state.
     *
     * This is the sum of getTableValue for each suit.
     *
     * @return the current score.
     */
    int getScore();

    /**
     * Return the number of moves left at the end of the game.
     *
     * @return the remaining moves.
     */
    int getMovesLeft();

    /**
     * Get the starting (maximum) number of information tokens possible
     *
     * @return the maximum number of information tokens allowed in the game.
     */
    int getStartingInfomation();

    /**
     * Get the starting (maximum) number of lives possible
     *
     * @return the maximum number of lives possible
     */
    int getStartingLives();

    /**
     * Get the current top card for a given deck on the table.
     *
     * The sum of these values is the player's score.
     *
     * @param colour the colour the check
     * @return the current table value, 0 if no cards in this suit have been played successfully.
     */
    int getTableValue(CardColour colour);

    /**
     * True if the game is over (the state is terminal)
     *
     * @return true if game over, false otherwise.
     */
    boolean isGameOver();

    /**
     * Set the card in a given player's hand to this card.
     *
     * This method will clear the information about the card known by the player, ie. it behaves as if they
     * just drew the card. If you are determinising, don't use this method.
     *
     * @param player the player to update
     * @param slot the position to update
     * @param newCard the new card.
     */
    void setCardAt(int player, int slot, Card newCard);

    /**
     * Set the remaining information tokens.
     *
     * @param newValue the new value of the infomation tokens.
     */
    void setInformation(int newValue);

    // update the state
    @Deprecated
    void setKnownValue(int player, int slot, Integer value, CardColour colour);

    /**
     * Set the value of a suit on the table.
     *
     * This method doesn't check that the card is legal and will not update the deck/card drawn.
     *
     * @param c the colour to update
     * @param nextValue the next value
     */
    void setTableValue(CardColour c, int nextValue);

    @Deprecated
    void tick();

    /**
     * Update state turn information.
     *
     * This is called when an action is applied.
     * If you are applying events manually, then call this when applying them.
     */
    void actionTick();

    /**
     * Return the game event history.
     *
     * The new history method should be used instead.
     *
     * @return the previous history of the game.
     */
    @Deprecated
    LinkedList<GameEvent> getHistory();

    int getTurnNumber();

    /**
     * Add an action to the game history.
     *
     * This has no effect on the game state, other than updating the log.
     *
     * @param playerID the player who performed the action
     * @param action the action that was performed
     * @param eventList the resulting events from out perspective.
     */
    void addAction(int playerID, Action action, List<GameEvent> eventList);
    List<HistoryEntry> getActionHistory(); 
}
