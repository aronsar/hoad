package com.fossgalaxy.games.fireworks.ai.iggi;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.Collection;
import java.util.HashSet;

public class Utils {
    private static final int[] HAND_SIZE = {-1, -1, 5, 5, 4, 4};

    private Utils() {

    }

    /**
     * Generate a collection of actions which contains all possible actions for a given game.
     *
     * This does not take into account which actions are legal, it simply numerates the full list of actions that
     * could be issued in any Hanabi game for a given player and a given game size.
     *
     * This will return moves from a given player's perspective, ie, if you are player 1 it won't include, "tell player 1 about their REDs"
     *
     * @param playerID The current player ID
     * @param numPlayers the number of players
     * @return a collection of all possible actions
     */
    public static Collection<Action> generateAllActions(int playerID, int numPlayers) {

        HashSet<Action> list = new HashSet<>();

        for (int i = 0; i < HAND_SIZE[numPlayers]; i++) {
            list.add(new DiscardCard(i));
            list.add(new PlayCard(i));
        }

        //Legal Information Actions
        for (int player = 0; player < numPlayers; player++) {
            //can't tell self about hand
            if (player == playerID) {
                continue;
            }


            for (CardColour colour : CardColour.values()) {
                list.add(new TellColour(player, colour));
            }
            for (int i = 0; i < 5; i++) {
                list.add(new TellValue(player, i));
            }
        }

        return list;
    }

    /**
     * Return all legal moves for a given game state.
     *
     * This method will return all moves that are legal for an agent to make in a given game state. Ie, moves that will
     * not trigger rules violations.
     *
     * @param playerID The current player ID
     * @param state the game state to consider
     * @return a collection of all legal moves
     */
    public static Collection<Action> generateActions(int playerID, GameState state) {
        HashSet<Action> list = new HashSet<>();

        Hand myHand = state.getHand(playerID);
        for (int slot = 0; slot < myHand.getSize(); slot++) {
            if (myHand.hasCard(slot)) {
                list.add(new PlayCard(slot));
                if (state.getInfomation() != state.getStartingInfomation()) {
                    list.add(new DiscardCard(slot));
                }
            }
        }

        //if we have no information, abort
        if (state.getInfomation() == 0) {
            return list;
        }

        //Legal Information Actions
        for (int player = 0; player < state.getPlayerCount(); player++) {
            //can't tell self about hand
            if (player == playerID) {
                continue;
            }

            Hand hand = state.getHand(player);
            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card card = hand.getCard(slot);
                if (card != null) {
                    list.add(new TellColour(player, card.colour));
                    list.add(new TellValue(player, card.value));
                }
            }
        }

        return list;
    }

    /**
     * Return a list of legal moves, excluding tell actions which don't make any sense.
     *
     * This is the same as GenerateActions but will exclude tell actions which don't provide any additional information
     * about a card directly to the told player. This does not take into account indirect information, IE, by pointing
     * out red cards that you already know about I am telling you that all other cards are not red.
     *
     * Eg, if player 1 has red cards but knows that they are red, this will not return "Tell player 1 about RED"
     *
     * @param playerID the current player ID
     * @param state the current game state
     * @return a collection of 'useful' actions
     */
    public static Collection<Action> generateSuitableActions(int playerID, GameState state) {
        HashSet<Action> list = new HashSet<>();

        Hand myHand = state.getHand(playerID);
        for (int slot = 0; slot < myHand.getSize(); slot++) {
            if (myHand.hasCard(slot)) {
                list.add(new PlayCard(slot));
                if (state.getInfomation() != state.getStartingInfomation()) {
                    list.add(new DiscardCard(slot));
                }
            }
        }

        //if we have no information, abort
        if (state.getInfomation() == 0) {
            return list;
        }

        //Legal Information Actions
        for (int player = 0; player < state.getPlayerCount(); player++) {
            //can't tell self about hand
            if (player == playerID) {
                continue;
            }

            Hand hand = state.getHand(player);
            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card card = hand.getCard(slot);
                if (card != null) {
                    if (hand.getKnownColour(slot) == null) {
                        list.add(new TellColour(player, card.colour));
                    }

                    if (hand.getKnownValue(slot) == null) {
                        list.add(new TellValue(player, card.value));
                    }
                }
            }
        }

        return list;
    }

}
