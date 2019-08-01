package com.fossgalaxy.games.fireworks.state;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static com.fossgalaxy.games.fireworks.state.CardColour.*;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.c;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.v;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertArrayEquals;

/**
 * Created by piers on 29/11/16.
 * <p>
 * Cards are sorted by colour first and then by value
 */
@RunWith(JUnitParamsRunner.class)
public class TestCard {

    public Object[] parametersForTestCardCompareTo() {
        return $(
                $(cards(v(1, 2, 3, 4, 5), c(BLUE, BLUE, BLUE, BLUE, BLUE)), cards(v(1, 2, 3, 4, 5), c(BLUE, BLUE, BLUE, BLUE, BLUE))),
                $(cards(v(5, 4, 3, 2, 1), c(BLUE, BLUE, BLUE, BLUE, BLUE)), cards(v(1, 2, 3, 4, 5), c(BLUE, BLUE, BLUE, BLUE, BLUE))),
                $(cards(v(1, 2, 3, 4, 5), c(RED, BLUE, GREEN, ORANGE, WHITE)), cards(v(1, 2, 3, 4, 5), c(RED, BLUE, GREEN, ORANGE, WHITE))),
                $(cards(v(1, 2, 3, 4, 5), c(WHITE, ORANGE, GREEN, BLUE, RED)), cards(v(5, 4, 3, 2, 1), c(RED, BLUE, GREEN, ORANGE, WHITE))),
                $(cards(v(5, 4, 3, 2, 1), c(WHITE, ORANGE, GREEN, BLUE, RED)), cards(v(1, 2, 3, 4, 5), c(RED, BLUE, GREEN, ORANGE, WHITE)))
                );
    }

    @Test
    @Parameters(method = "parametersForTestCardCompareTo")
    public void testCardCompareTo(Card[] input, Card[] expected) {
        Arrays.sort(input);
        assertArrayEquals(expected, input);
    }

    public static Card[] cards(Integer[] values, CardColour[] colours) {
        int length = Math.min(values.length, colours.length);
        Card[] cards = new Card[length];
        for (int i = 0; i < length; i++) {
            cards[i] = new Card(values[i], colours[i]);
        }
        return cards;
    }

}
