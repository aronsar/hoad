package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * The basic state of the game.
 *
 * This keeps track of all the features of the game that we have learnt.
 */
public class BasicState implements GameState {
    private final Logger LOG = LoggerFactory.getLogger(BasicState.class);

    /**
     * The hand sizes for different numbers of players. A -1 in this list indicates that the requested size of game
     * is not permitted under the game rules.
     *
     * The index into the array is the number of players, ie, HAND_SIZE[2] means a 2 player game.
     */
    private static final int[] HAND_SIZE = {-1, -1, 5, 5, 4, 4};

    /**
     * The maximum (starting) information the agents are permitted to have.
     */
    private static final int MAX_INFOMATION = 8;

    /**
     * The maximum (starting) lives the agents are permitted to have.
     */
    private static final int MAX_LIVES = 3;

    /**
     * The maximum possible score if all suits are played correctly.
     */
    private static final int MAX_SCORE = CardColour.values().length * 5;

    /**
     * The number of cards permitted to be in a player's hand in this game
     */
    private final int handSize;

    /**
     * The player's hands.
     *
     * The index is the player ID, ie. hands[0] is the first player in the game.
     */
    private final TimedHand[] hands;

    /**
     * The cards remaining in the game, with their associated ordering (if known)
     */
    private final Deck deck;

    /**
     * The current state of the table
     *
     * if the value is Null, this indicates that no card is on the table (the next card should be a 1).
     */
    private final Map<CardColour, Integer> table;

    /**
     * The list (and order) of cards discarded so far in the game
     */
    private final List<Card> discard;

    /**
     * A history of the game from this agent's perspective.
     */
    private final LinkedList<HistoryEntry> historyEntries;

    /**
     * The current number of remaining information tokens
     */
    private int information;

    /**
     * The current number of remaining lives
     */
    private int lives;

    /**
     * Variable used to keep track of how many moves are permitted during the end game, once this hits zero the game
     * is over.
     *
     * This should only be used when the deck is empty.
     */
    private int movesLeft;

    /**
     * The current turn number.
     *
     * This is incremented whenever tick is called.
     */
    private int turnNumber;

    /**
     * A copy constructor for the state.
     *
     * Creating a state and passing in a state as an argument will create a deep copy of that state. If someone may
     * extend this state, it is better to use the getCopy method rather than creating a copy using this constructor.
     *
     * @param state the state to clone
     */
    protected BasicState(BasicState state) {
        this.handSize = state.handSize;
        this.deck = new Deck(state.deck);
        this.discard = new ArrayList<>(state.discard);
        this.information = state.information;
        this.lives = state.lives;
        this.movesLeft = state.movesLeft;

        this.historyEntries = new LinkedList<>();
        this.historyEntries.addAll(state.historyEntries);

        this.turnNumber = state.turnNumber;

        this.table = new EnumMap<>(state.table);

        this.hands = new TimedHand[state.hands.length];
        for (int i = 0; i < hands.length; i++) {
            hands[i] = new TimedHand(state.hands[i]);
        }
    }

    /**
     * Create a game that has player count players.
     *
     * This assumes that the number of player's provided is valid (between 2 and 5).
     *
     * @param playerCount the number of players to use for this game.
     */
    public BasicState(int playerCount) {
        this(HAND_SIZE[playerCount], playerCount);
    }

    /**
     * Create a game state passing in the hand size and number of players.
     *
     * This should probably not be used directly, it's better to use the single argument version of the constructor.
     *
     * @param handSize the number of cards each player should be dealt
     * @param playerCount the number of players that will be in the game
     */
    public BasicState(int handSize, int playerCount) {
        assert handSize != -1;
        assert playerCount >= 2;
        assert playerCount <= 5;

        this.handSize = handSize;
        this.hands = new TimedHand[playerCount];
        this.deck = new Deck();
        this.table = new EnumMap<>(CardColour.class);
        this.discard = new ArrayList<>();
        this.movesLeft = playerCount + 1;
        this.historyEntries = new LinkedList<>();

        this.turnNumber = 0;

        this.information = MAX_INFOMATION;
        this.lives = MAX_LIVES;

        for (int i = 0; i < playerCount; i++) {
            hands[i] = new TimedHand(handSize);
            hands[i].init();
        }
    }

    /**
     * Perform game setup with a random seed.
     */
    @Override
    public void init() {
        init(null);
    }

    /**
     * Setup the game for play.
     *
     * This should only be called once at the start of the game, it's job is to create the deck, shuffle it and deal
     * the starting hands to the players. This should not be called directly by client code unless they are trying to
     * create a new game from scratch.
     *
     * @param seed the seed to use for deck ordering, if null the java default will be used.
     */
    @Override
    public void init(Long seed) {
        deck.init();
        if (seed != null) {
            deck.shuffle(seed);
        } else {
            deck.shuffle();
        }
        dealHands();
    }

    /**
     * Deal cards to each player.
     *
     * This should not be called directly, instead init should deal with this for us.
     */
    public void dealHands() {
        for (int hand = 0; hand < hands.length; hand++) {
            deal(hand);
        }
    }

    /**
     * Create a copy of the game state.
     *
     * This allows an agent to get a deep copy of the game state. It is functionally equlivent to using the copy
     * constructor on a basic state.
     *
     * It is present in case someone wishes to extend basic state and needs custom logic when cloning. When this is
     * possible, it is better to use this method than the copy constructor.
     *
     * @return A deep copy of this game state
     */
    @Override
    public GameState getCopy() {
        return new BasicState(this);
    }

    /**
     * Get a version of the hand for a given player, with information about their hand removed.
     * <p>
     * The server side should <b>never</b> need to use this - as only updates are sent to each client they should not
     * see any information they are not permitted to see.
     * <p>
     * It's mostly good for simulation-based approaches.
     *
     * @param playerID the playerID of the hand you want to access
     * @return the hidden information version of the hand
     */
    public Hand getPerspective(int playerID) {
        return new ShieldedHand(hands[playerID]);
    }

    /**
     * Add a card to the list of discarded cards this game.
     *
     * This will not remove the card from the deck as it is assumed that it is being removed from a player's hand.
     * It will also not update information or perform any game logic checks.
     *
     * @param card the card to add to the discard pile.
     */
    @Override
    public void addToDiscard(Card card) {
        assert card != null;
        discard.add(card);
    }

    /**
     * Convenience method to draw the card at the top of the deck.
     *
     * @return the card at the top of the deck
     */
    @Override
    public Card drawFromDeck() {
        return deck.getTopCard();
    }

    /**
     * Convenience method for getting a card from a player's hand.
     *
     * If this is called on the player's hand it should return null as we cannot access cards in our own hand.
     *
     * @param player the player to query
     * @param slot the slot to ask for
     * @return the card in the slot, or null of no card is present.
     */
    @Override
    public Card getCardAt(int player, int slot) {
        assert player >= 0 : "playerID must be bigger than -1";
        assert player < hands.length : "player ID higher than number of players";

        return hands[player].getCard(slot);
    }

    /**
     * Get the deck used by the game state
     *
     * If this is being asked for by the client, the deck will be in an undefined order (usually the order of card
     * creation before shuffling if the deck has not been shuffled by the client).
     *
     * @return The deck used by the game state
     */
    @Override
    public Deck getDeck() {
        return deck;
    }

    /**
     * Return a read only list of discarded cards
     *
     * @return the cards that have been discarded during this game
     */
    @Override
    public Collection<Card> getDiscards() {
        return Collections.unmodifiableList(discard);
    }

    /**
     * Get the hand (set of cards in front of) the corresponding player.
     *
     * @param player the player who's hand we are asking for
     * @return the players hand
     */
    @Override
    public Hand getHand(int player) {
        assert player >= 0 : "playerID must be bigger than -1";
        assert player < hands.length : "player ID higher than number of players";

        return hands[player];
    }

    /**
     * Convenience method for asking for the number of slots contained in a player's hand.
     *
     * @return the number of slots that the player can have in their hand.
     */
    @Override
    public int getHandSize() {
        return handSize;
    }

    /**
     * Get the amount of information tokens currently present on the board.
     *
     * @return the amount of unspent information tokens in the board.
     */
    @Override
    public int getInfomation() {
        return information;
    }

    /**
     * Return the number of lives remaining
     *
     * @return the number of lives remaining
     */
    @Override
    public int getLives() {
        return lives;
    }

    /**
     * Change the number of lives remaining.
     *
     * This is used when a player makes an error to update the current lives remaining.
     *
     * @param newValue the new number of lives remaining.
     */
    @Override
    public void setLives(int newValue) {
        assert newValue <= MAX_LIVES;
        assert newValue >= 0;
        lives = newValue;
    }

    /**
     * Return the number of players currently in the game
     *
     * @return the number of players that are currently in the game
     */
    @Override
    public int getPlayerCount() {
        return hands.length;
    }

    /**
     * Return the current score for this state
     *
     * The score is equal to the total value of the highest card on the table of each suit, as the the hanabi rules.
     *
     * @return the current score
     */
    @Override
    public int getScore() {
        int total = 0;

        for (Integer val : table.values()) {
            total += val;
        }
        return total;
    }

    /**
     * Return the starting (maximum) permitted information.
     *
     * @return the maximum amount of information the players can have
     */
    @Override
    public int getStartingInfomation() {
        return MAX_INFOMATION;
    }

    /**
     * Return the starting (maximum) permitted livess.
     *
     * @return the maximum amount of lives the players can have
     */
    @Override
    public int getStartingLives() {
        return MAX_LIVES;
    }

    /**
     * Return the current value of the table for a given suit.
     *
     * The next card to be played in this suit is equal to 1 + this value, if this value is 5 then the suit is already
     * complete.
     *
     * @param colour the suit to query
     * @return the current value of the table, 0 if no card is present for that suit.
     */
    @Override
    public int getTableValue(CardColour colour) {
        assert colour != null : "colour should never be null";
        Integer curr = table.get(colour);
        return curr == null ? 0 : curr;
    }

    /**
     * Does this state represent a game over condition?
     *
     * A game is over if:
     *  * The number of lives remaining is 0
     *  * The deck is empty and everyone has had their last go
     *  * We have obtained a perfect score
     *
     * @return true if this is a terminal state, else false
     */
    @Override
    public boolean isGameOver() {
        //check if we have run out of lives
        if (lives <= 0) {
            return true;
        }

        //check that the deck is empty, and all players have had last go
        if (!deck.hasCardsLeft() && movesLeft == 0) {
            return true;
        }

        //if we have a maximum score, we win!
        if (getScore() == MAX_SCORE) {
            return true;
        }

        return false;
    }

    /**
     * Deal a card to a player.
     *
     * This will cause the player to receive a card in the provided slot. The player will forget all information
     * previously known about this slot.
     *
     * This should not be used for determination, as the player will forget what they know about the card.
     *
     * @param player the player to give the card to
     * @param slot the slot to put the card in
     * @param card the card to give to the player
     */
    @Override
    public void setCardAt(int player, int slot, Card card) {
        BasicHand hand = hands[player];
        hand.clear(slot);
        hand.setCard(slot, card);
    }

    @Override
    public int getMovesLeft(){
        return movesLeft;
    }

    /**
     * Update the number of information tokens in this state.
     *
     * @param newValue the new number of information tokens present in the state.
     */
    @Override
    public void setInformation(int newValue) {
        assert newValue <= MAX_INFOMATION;
        assert newValue >= 0;
        information = newValue;
    }

    /**
     * Update the card in a player's hand.
     *
     * This will simply set the card present in a given position to be that card, it will not clear any information
     * that player has about that card or update the deck. It's mostly of use to agents like IS-MCTS.
     *
     * This method should not be used for dealing cards, see setCardAt for that.
     *
     * @param player the player to give the card to
     * @param slot the slot to put the card in
     * @param value the value of the card
     * @param colour the suit of the card
     */
    @Override
    @Deprecated
    public void setKnownValue(int player, int slot, Integer value, CardColour colour) {
        assert player >= 0 : "playerID must be bigger than -1";
        assert player < hands.length : "player ID higher than number of players";

        Hand hand = hands[player];
        hand.setCard(slot, new Card(value, colour));
    }

    /**
     * Update the value of the table to a new value.
     *
     * @param colour the suit to update
     * @param value the new value on the table
     */
    @Override
    public void setTableValue(CardColour colour, int value) {
        table.put(colour, value);
    }
    
    @Deprecated
    public void tick() {
    	LOG.warn("Tick is handled automaticlly, if you are applying events call actionTick instead");
    }

    /**
     * Countdown clock used to keep track of remaining moves in the end game.
     *
     * This should be called any time the state is advanced.
     */
    @Override
    public void actionTick() {
        turnNumber++;
        if (!deck.hasCardsLeft()) {
            movesLeft--;
        }
    }

    /**
     * Deal a hand to the given player.
     *
     * This will take the top handSize cards from the deck and put them into the player's hand.
     *
     * @param playerID the player to deal to.
     */
    @Override
    public void deal(int playerID) {
        for (int slot = 0; slot < handSize; slot++) {
            hands[playerID].setCard(slot, deck.getTopCard());
        }
    }

    /**
     * Check if two states are equal.
     *
     * @param o the other state
     * @return true if the states are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicState that = (BasicState) o;

        if (handSize != that.handSize) return false;
        if (information != that.information) return false;
        if (lives != that.lives) return false;
        if (movesLeft != that.movesLeft) return false;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(hands, that.hands)) return false;
        if (!deck.equals(that.deck)) return false;
        if (!table.equals(that.table)) return false;
        return discard.equals(that.discard);

    }

    /**
     * Automatically generated hashcode.
     *
     * @return a suitable hashcode
     */
    @Override
    public int hashCode() {
        int result = handSize;
        result = 31 * result + Arrays.hashCode(hands);
        result = 31 * result + deck.hashCode();
        result = 31 * result + table.hashCode();
        result = 31 * result + discard.hashCode();
        result = 31 * result + information;
        result = 31 * result + lives;
        result = 31 * result + movesLeft;
        return result;
    }

    /**
     * Return the history of events observed by this agent
     *
     * @return the list of events that have occurred in the game.
     */
    @Override
    public LinkedList<GameEvent> getHistory() {
        LOG.warn("You should be using getActionHistory, getHistory will be removed!");

        LinkedList<GameEvent> events = new LinkedList<>();
        for (HistoryEntry entry : historyEntries){
            events.addAll(entry.history);
        }

        return events;
    }

    @Override
    public int getTurnNumber() {
        return turnNumber;
    }

    /**
     * Update the game history with a new action.
     *
     * This must be invoked when new actions are received from the game engine - but this should be dealt with for you.
     * When applying actions though action.apply, this method will be called for you.
     *
     * @param playerID the player who performed the action
     * @param action the action that was performed
     * @param effects the effects of the action
     */
    public void addAction(int playerID, Action action, List<GameEvent> effects) {
        HistoryEntry entry = new HistoryEntry(playerID, action, effects);
        historyEntries.add(entry);
    }

    public List<HistoryEntry> getActionHistory(){
        return Collections.unmodifiableList(historyEntries);
    }
}
