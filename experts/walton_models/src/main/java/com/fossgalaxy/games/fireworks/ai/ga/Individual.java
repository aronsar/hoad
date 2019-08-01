package com.fossgalaxy.games.fireworks.ai.ga;

import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.*;

/**
 * Created by webpigeon on 24/02/17.
 */
public class Individual {
    private static final int NUM_REPEATS = 5;
    private static final double MUTUATE_THEASHOLD = 0.5;

    private int[] actions;
    private Random random;

    public Individual(int length) {
        this.actions = new int[length];
        this.random = new Random();
    }

    public Individual(Individual parent) {
        this.actions = Arrays.copyOf(parent.actions, parent.actions.length);
        this.random = new Random();
    }

    public double multiEval(GameState state, int myID) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindCard(myID, state.getHand(myID), state.getDeck().toList());
        List<Integer> bindOrder = DeckUtils.bindOrder(possibleCards);

        double totScore = 0;

        for (int i=0; i<NUM_REPEATS; i++) {
            GameState detState = determinise(state, myID, possibleCards, bindOrder);
            totScore += evaluate(detState, myID);
        }

        return totScore / NUM_REPEATS;
    }

    public GameState determinise(GameState state, int agentID, Map<Integer, List<Card>> possibleCards, List<Integer> bindOrder) {
        GameState clone = state.getCopy();

        Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);

        Deck deck = clone.getDeck();
        Hand myHand = clone.getHand(agentID);
        for (int slot = 0; slot < myHand.getSize(); slot++) {
            Card hand = myHandCards.get(slot);
            myHand.bindCard(slot, hand);
            deck.remove(hand);
        }
        deck.shuffle();

       return clone;
    }

    public int evaluate(GameState state, int myID) {
        GameState forward = state;
        int myMoves = 0;
        int playerId = 0;

        while(!forward.isGameOver() && myMoves < actions.length) {
            List<Action> possible = new ArrayList<>(Utils.generateActions(playerId, forward));
            if (playerId == myID) {
                Action myAction = possible.get( Math.abs(actions[myMoves++] % possible.size()) );
                myAction.apply(playerId, forward);
            } else {
                //if it's someone else's go - make a random move
                Action myAction = possible.get(random.nextInt(possible.size()));
                myAction.apply(playerId, forward);
            }
            playerId = (playerId + 1) % state.getPlayerCount();
        }

        return forward.getScore();
    }

    public Individual copy() {
        return new Individual(this);
    }

    public Individual randomise() {
        for (int i=0; i<actions.length; i++) {
            actions[i] = random.nextInt(24);
        }
        return this;
    }

    public Individual mutate() {
        for (int i=0; i<actions.length; i++) {
            if (random.nextDouble() < MUTUATE_THEASHOLD) {
                actions[i] = random.nextInt(24);
            }
        }
        return this;
    }

    public Action getAction(int move, int agentID, GameState state) {
        System.out.println(Arrays.toString(actions));
        List<Action> possible = new ArrayList<>(Utils.generateActions(agentID, state));
        return possible.get( Math.abs(actions[move] % possible.size()) );
    }
}
