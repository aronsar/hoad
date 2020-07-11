package com.fossgalaxy.games.fireworks.ai.mcts.model;

import com.fossgalaxy.games.fireworks.state.Card;

import java.util.List;

/**
 * Created by Piers on 09/08/2016.
 * <p>
 * Takes in observations of cards including partial observations (information told about a card) and calculates probabilities
 * for remaining cards.
 * <p>
 * We see many cards, each of which improves the accuracy of the model.
 * <p>
 * Start off with list of all cards. Provide observations about what we see - crossing them off.
 * <p>
 * What happens to information we are told?  It can make certain cards more likely to be something.
 * <p>
 * To do this we need to - for each card in our hand store information about what it could be.
 * <p>
 * If we know its colour - we can choose cards from the deck that match.
 * Likewise for the number.
 * <p>
 * It can improve statistics about what is in the deck. Knowing there is a red in the hand reduces chance of a red in the deck.
 * <p>
 * By how much?
 * <p>
 * we know there are 6 reds in the deck somewhere.
 * <p>
 * We are then told there is a red in our hand.
 * <p>
 * That means there are 5 reds in the deck.
 * <p>
 * How to store that?
 * <p>
 * N cards in hand, 50 - (N*M) - T - P cards in deck.
 * <p>
 * Store an array of probabilities for each card.
 * <p>
 * Probability that it is in the deck and that it is in the hand.
 * <p>
 * If we know nothing about that card - it is equal probability equal to 1/remaining cards
 * <p>
 * Adjust for knowledge.
 */
public class DeckModel {

    // [card][hand or deck]
    private double[] probabilities;
    private boolean initialised = false;

    public DeckModel() {
        probabilities = new double[50 * 2];
    }

    /**
     * Provide the model with a list of cards that are visible at the start of the game.
     * <p>
     * These are used to calculate intial probabilities for cards in your hand and the deck.
     *
     * @param observableCards the cards that we can see
     */
    public void initialObservations(List<Card> observableCards) {
        for (Card card : observableCards) {

        }
    }


    private double get(int card, boolean hand) {
        return probabilities[card + ((hand) ? 1 : 0)];
    }

    private void set(int card, boolean hand, double value) {
        probabilities[card + ((hand) ? 1 : 0)] = value;
    }
}
