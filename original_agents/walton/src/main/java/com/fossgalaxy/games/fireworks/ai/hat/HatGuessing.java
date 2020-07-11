package com.fossgalaxy.games.fireworks.ai.hat;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.rule.DiscardOldestFirst;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.TimedHand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by piers on 11/11/16.
 */
public class HatGuessing implements Agent {
    static final int[][] ENCODING = new int[][]{
            new int[]{99, 0, 1, 2, 3},
            new int[]{3, 99, 0, 1, 2},
            new int[]{2, 3, 99, 0, 1},
            new int[]{1, 2, 3, 99, 0},
            new int[]{0, 1, 2, 3, 99},
    };
    private static final int NOT_FOUND = 99;
    private static final int[] copies = {0, 3, 2, 2, 2, 1};
    private int playerID;

    private Recommendation lastToldAction;
    private int cardsPlayedSinceHint = 0;

    private Rule discardOldest;

    public HatGuessing() {
        this.discardOldest = new DiscardOldestFirst();
        this.lastToldAction = null;
    }


    @Override
    public Action doMove(int agentID, GameState state) {
        //we're not allowed internal state - so re-play the game to figure out what it should look like
        replayHistory(state);

        // 1. If the most recent recommendation was to play a card and no card has been played since the lat hint, play the recommended card
        // 2. If the most recent recommendation was to play a card, one card has been played
        //    since the hint was given, and the players have made fewer than two errors, play the
        //    recommended card
        if (lastToldAction != null && lastToldAction.isPlay()) {
            if (cardsPlayedSinceHint == 0) {
                return lastToldAction.recommended;
            }
            if (cardsPlayedSinceHint == 1 && state.getLives() > 1) {
                return lastToldAction.recommended;
            }
        }

        // 3. If the players have a hint token, give a hint.
        if (state.getInfomation() > 0) {
            Recommendation myIdea = doRecommend(agentID, state);
            return Recommendation.encode(myIdea, agentID, state);
        }

        // 4. If the most recent recommendation was to discard a card, discard the requested card.
        if (lastToldAction != null && lastToldAction.isDiscard()) {
            return lastToldAction.recommended;
        }

        // 5. Discard card c 1 (Oldest CardComponent)
        if (discardOldest.canFire(agentID, state)) {
            return discardOldest.execute(agentID, state);
        }

        //ok, what happens now? :S
        throw new IllegalStateException("No hat guessing rule fired, no idea what to do...");
    }

    /**
     * Is the card dead by the Hat Guessing definition
     *
     * @param state The current state
     * @param card  The card we are interested in
     * @return boolean indicating the deadness of the card
     */
    public boolean isDead(GameState state, Card card) {
        return state.getTableValue(card.colour) >= card.value;
    }

    /**
     * Is the card playable by the Hat Guessing definition
     *
     * @param state The current state
     * @param card  The card we are interested in
     * @return boolean indicating the playableness of the card
     */
    public boolean isPlayable(GameState state, Card card) {
        return state.getTableValue(card.colour) == card.value - 1;
    }


    /**
     * places a card can be:
     * 1) in the deck (still in deck or our hand)
     * 2) in the discard pile (played incorrectly or discarded cards)
     * 3) on the table (card is no longer needed, a copy was played successfully)
     * 4) another player's hand (our copy is not the only one)
     *
     * @param state The current state
     * @param card  The card we are interested in
     * @return boolean indicating if the card is indispensable
     */
    public boolean isIndispensable(GameState state, Card card) {

        //if the card is dead, then it's not needed and we can discard it
        if (isDead(state, card)) {
            return false;
        }

        long copiesInDeck = state.getDeck().toList().stream().filter(card::equals).count();

        //if there is at least 1 copy in the deck we're fine
        if (copiesInDeck > 0) {
            return false;
        }

        //figure out how many copies have already been discarded/played incorrectly
        int totalCopies = copies[card.value];
        long copiesIsDiscard = state.getDiscards().stream().filter(card::equals).count();
        if (copiesIsDiscard < totalCopies - 1) {
            return false; //we've can't account for all of them in either the discard - they're still in play somewhere
        }

        //
        return true;
    }

    private GameState replayHistory(GameState state) {
        return fastForward(state.getHistory(), state.getPlayerCount());
    }

    private GameState fastForward(List<GameEvent> events, int nPlayers) {
        //create a game state
        GameState ff = new BasicState(nPlayers);

        //reset history
        lastToldAction = null;
        cardsPlayedSinceHint = 0;

        for (GameEvent event : events) {
            event.apply(ff);
            applyEffect(ff, event);
        }

        return ff;
    }

    /**
     * Gets the integer recommendation for the Hat Guessing algorithm for a single hand
     * <p>
     * Used to both calculate the encoding and decoding of a hint.
     *
     * @param agentToTell The agent to calculate the hint for
     * @param state       The current game state
     * @return The value to tell between 0-7
     */
    public int getRecommendationForAHand(int agentToTell, GameState state) {

        TimedHand hand = (TimedHand) state.getHand(agentToTell);
        Integer[] handOrder = new Integer[]{0, 1, 2, 3};
        Arrays.sort(handOrder, Comparator.comparingInt(hand::getAge));


        //track the stuff we'll need for the rules
        int lowestPlayable = NOT_FOUND;
        int lowestValue = 99;
        int lowestDead = NOT_FOUND;

        int highestDispensible = NOT_FOUND;
        int highestDispensibleValue = -1;

        //because of the order of iteration, we can ignore
        //cards with the same value (ties), as the correct card will have
        //already been selected.
        for (int slot : handOrder) {
            Card card = hand.getCard(slot);

            //guard against the card being missing (end of the game)
            if (card == null) {
                continue;
            }

            // 1. Recommend that the playable card of rank 5 with lowest index be played.
            // we can shortcut everything else, we know we're going to do this.
            if (card.value == 5 && isPlayable(state, card)) {
                return slot;
            }

            //oldest playable
            if (card.value < lowestValue && isPlayable(state, card)) {
                lowestPlayable = slot;
                lowestValue = card.value;
            }

            //oldest dead
            if (lowestDead == NOT_FOUND && isDead(state, card)) {
                lowestDead = slot;
            }

            //highest rank dispensible (not indispensable)
            if (card.value > highestDispensibleValue && !isIndispensable(state, card)) {
                highestDispensible = slot;
                highestDispensibleValue = card.value;
            }
        }

        // 2. Recommend that the playable card with lowest rank (value) be played. If there is a tie for
        //    lowest rank, recommend the one with lowest index.
        if (lowestPlayable != NOT_FOUND) {
            return lowestPlayable;
        }

        // 3. Recommend that the dead card with lowest index (oldest) be discarded.
        if (lowestDead != NOT_FOUND) {
            return lowestDead + 4;
        }

        // 4. Recommend that the card with highest rank (value) that is not indispensable be discarded.
        //    If there is a tie, recommend the one with lowest index.
        if (highestDispensible != NOT_FOUND) {
            return highestDispensible + 4;
        }

        // 5. Recommend that oldest (c1) be Discarded
        return handOrder[0] + 4;
    }


    /**
     * Playable: a card that can be successfully played with the current game state.
     * <p>
     * Dead: a card that has the same rank and suit of a successfully played card.
     * <p>
     * Indispensable: a card for which all other identical copies have been removed from
     * the game, i.e. a card that if removed from the game will imply a perfect score cannot
     * be obtained.
     *
     * @param agentID the current agentID
     * @param state the current game state
     * @return the recommendation that should be made
     */
    public Recommendation doRecommend(int agentID, GameState state) {

        int sum = 0;
        for (int position = 0; position < 5; position++) {
            if (position == agentID) {
                continue;
            }
            sum += getRecommendationForAHand(position, state);
        }
        return Recommendation.values()[sum % 8];
    }


    @Override
    public void receiveID(int agentID, String[] names) {
        playerID = agentID;
    }


    public int getMissingPiece(GameState state, int whoTold, int treasureChest) {
        int sum = 0;
        for (int position = 0; position < 5; position++) {
            if (position == playerID || position == whoTold) {
                continue;
            }
            sum += getRecommendationForAHand(position, state);

        }

        //find our piece
        for (int i = 0; i < 8; i++) {
            if ((sum + i) % 8 == treasureChest) {
                return i;
            }
        }
        throw new IllegalStateException("There was no treasure me hearties");
    }

    public void applyEffect(GameState state, GameEvent event) {
        switch (event.getEvent()) {
            case CARD_INFO_COLOUR: {
                CardInfoColour tellColour = (CardInfoColour) event;
                int recommendation = 4 + getEncodedValue(tellColour.getPerformer(), tellColour.getPlayerTold());
                lastToldAction = Recommendation.values()[getMissingPiece(state, tellColour.getPerformer(), recommendation)];
                cardsPlayedSinceHint = 0;
                break;
            }
            case CARD_INFO_VALUE: {
                CardInfoValue tellValue = (CardInfoValue) event;
                int recommendation = getEncodedValue(tellValue.getPerformer(), tellValue.getPlayerTold());
                lastToldAction = Recommendation.values()[getMissingPiece(state, tellValue.getPerformer(), recommendation)];
                cardsPlayedSinceHint = 0;
                break;
            }
            case CARD_PLAYED:
                cardsPlayedSinceHint++;
        }
    }

    /**
     * Helper method to understand the hint system
     *
     * @param whoTold Who told the hint
     * @param toldWho Who the hint was told to
     * @return The value
     */
    public int getEncodedValue(int whoTold, int toldWho) {
        return ENCODING[whoTold][toldWho];
    }

    @Override
    public String toString() {
        return String.format("%s", "HatGuessing");
    }
}
