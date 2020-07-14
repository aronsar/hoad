package com.fossgalaxy.games.fireworks.state;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fossgalaxy.games.fireworks.state.CardColour.*;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.c;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.setKnowledge;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.v;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 25/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestHandEquals {

    public Object[] parametersForTestEquals() {
        return $(
                $(4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), 4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), true),
                $(4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), 4, v(2, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), false),
                $(4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), 4, v(1, 2, 3, 4), c(RED, RED, GREEN, ORANGE), false),
                $(4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), 4, v(2, 2, 3, 4), c(RED, RED, GREEN, ORANGE), false),
                $(4, v(1, 2, 3, 4), c(BLUE, RED, GREEN, ORANGE), 5, v(1, 2, 3, 4, 5), c(BLUE, RED, GREEN, ORANGE, WHITE), false)
        );
    }

    @Test
    @Parameters(method = "parametersForTestEquals")
    public void testBasicHandEquals(int size1, Integer[] values1, CardColour[] colours1, int size2, Integer[] values2, CardColour[] colours2, boolean expected) {
        Hand hand1 = new BasicHand(size1);
        Hand hand2 = new BasicHand(size2);
        setKnowledge(hand1, values1, colours1);
        setKnowledge(hand2, values2, colours2);

        assertEquals(expected, hand1.equals(hand2));
    }

    @Test
    @Parameters(method = "parametersForTestEquals")
    public void testNegativeHandEquals(int size1, Integer[] values1, CardColour[] colours1, int size2, Integer[] values2, CardColour[] colours2, boolean expected) {
        Hand hand1 = new NegativeHand(size1);
        Hand hand2 = new NegativeHand(size2);
        setKnowledge(hand1, values1, colours1);
        setKnowledge(hand2, values2, colours2);

        assertEquals(expected, hand1.equals(hand2));
    }

    @Test
    @Parameters(method = "parametersForTestEquals")
    public void testTimedHandEquals(int size1, Integer[] values1, CardColour[] colours1, int size2, Integer[] values2, CardColour[] colours2, boolean expected) {
        Hand hand1 = new TimedHand(size1);
        Hand hand2 = new TimedHand(size2);
        setKnowledge(hand1, values1, colours1);
        setKnowledge(hand2, values2, colours2);

        assertEquals(expected, hand1.equals(hand2));
    }

    @Test
    @Parameters(method = "parametersForTestEquals")
    public void testShieldedHandEquals(int size1, Integer[] values1, CardColour[] colours1, int size2, Integer[] values2, CardColour[] colours2, boolean expected) {
        BasicHand h1 = new BasicHand(size1);
        BasicHand h2 = new BasicHand(size2);

        Hand hand1 = new ShieldedHand(h1);
        Hand hand2 = new ShieldedHand(h2);
        setKnowledge(h1, values1, colours1);
        setKnowledge(h2, values2, colours2);

        assertEquals(expected, hand1.equals(hand2));
    }
}
