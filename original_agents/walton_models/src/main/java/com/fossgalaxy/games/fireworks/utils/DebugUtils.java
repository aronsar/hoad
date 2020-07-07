package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 11/10/16.
 */
public class DebugUtils {

    // utility class - no instances required
    private DebugUtils() {

    }

    public static void printState(Logger logger, GameState state) {

        logger.debug("{} players, information: {}, lives: {}, score: {}", state.getPlayerCount(), state.getInfomation(),
                state.getLives(), state.getScore());

        printTable(logger, state);
        printHands(logger, state);
        printDeck(logger, state);
        printDiscard(logger, state);
    }

    public static void printTable(Logger logger, GameState state) {
        Arrays.stream(CardColour.values()).forEach(c -> logger.debug("{} {}", c, state.getTableValue(c)));
    }

    public static void printHands(Logger logger, GameState state) {
        for (int player = 0; player < state.getPlayerCount(); player++) {
            Hand hand = state.getHand(player);

            logger.debug("\tplayer {}'s hand", player);
            logger.debug("\t\t {} {} {} {}", "slot", "card", "known colour", "known value");
            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card real = hand.getCard(slot);
                logger.debug("\t\t {} {} {} {}", slot, real, hand.getKnownColour(slot), hand.getKnownValue(slot));
            }
        }
    }

    public static void printDiscard(Logger logger, GameState state) {
        logger.debug("discard pile:");
        Map<Card, Long> cardCounts = histogram(state.getDiscards());
        cardCounts.forEach((card, count) -> logger.debug("\t{} x {}", card, count));
    }

    public static void printDeck(Logger logger, GameState state) {
        logger.debug("Deck:");
        Map<Card, Long> cardCounts = histogram(state.getDeck().toList());
        cardCounts.forEach((card, count) -> logger.debug("\t{} x {}", card, count));
    }

    public static <T> String getHistStr(Map<T, Long> hist) {
        List<String> result = new ArrayList<>();
        hist.forEach((key, value) -> result.add(getHistEntry(key, value)));
        return result.toString();
    }

    public static <T> String getHistEntry(T key, Long value) {
        if (value == 1) {
            return key.toString();
        } else {
            return String.format("%s x %d", key, value);
        }
    }

    public static <T> Map<T, Long> histogram(Collection<T> c) {
        return c.stream().collect(Collectors.groupingBy(r -> r, Collectors.counting()));
    }

}
