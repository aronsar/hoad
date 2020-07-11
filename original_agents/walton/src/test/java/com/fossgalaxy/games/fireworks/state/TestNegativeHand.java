package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.ai.rule.RuleUtils;
import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
public class TestNegativeHand {

    private NegativeHand hand;

    @Before
    public void setup() {
        hand = new NegativeHand(5);
        hand.init();
        RuleUtils.setHasCards(hand);
    }

    @Test
    @Parameters({"0", "1", "2", "3", "4"})
    public void testClear(int slot) {
        hand.setKnownColour(BLUE, new Integer[]{slot});
        hand.setKnownValue(1, new Integer[]{slot});

        hand.clear(slot);

        assertEquals(null, hand.getKnownColour(slot));
        assertEquals(null, hand.getKnownValue(slot));
    }

    public Object[] parametersForTestKnownColour() {
        return $(
                $(c(BLUE, RED, GREEN, ORANGE), 4, WHITE),
                $(c(BLUE, RED, GREEN, GREEN), 4, null)
        );
    }

    @Test
    @Parameters(method = "parametersForTestKnownColour")
    public void testKnownColour(CardColour[] colours, int slot, CardColour expected) {
        setKnowledge(hand, colours);
        assertEquals(expected, hand.getKnownColour(slot));
    }

    public Object[] parametersForTestKnownValue() {
        return $(
                $(v(1, 2, 3, 4), 4, 5),
                $(v(1, 2, 4, 4), 4, null)
        );
    }

    @Test
    @Parameters(method = "parametersForTestKnownValue")
    public void testKnownValue(Integer[] v, int slot, Integer expected) {
        setKnowledge(hand, v);
        assertEquals(expected, hand.getKnownValue(slot));
    }

    public Object[] parametersForTestPossibleColours() {
        return $(
                $(null, 1, c(BLUE, GREEN, WHITE, ORANGE, RED)),
                $(c(BLUE), 1, c(GREEN, WHITE, ORANGE, RED)),
                $(c(BLUE, GREEN), 2, c(WHITE, ORANGE, RED)),
                $(c(BLUE, GREEN, WHITE), 3, c(ORANGE, RED)),
                $(c(BLUE, GREEN, WHITE, ORANGE), 4, c(RED)),
                $(c(BLUE, GREEN, WHITE, ORANGE, RED), 4, c(RED))
        );
    }

    @Test
    @Parameters(method = "parametersForTestPossibleColours")
    public void testPossibleColours(CardColour[] colours, int testSlot, CardColour[] expected) {
        setKnowledge(hand, colours);
        assertEquals(set(expected), set(hand.getPossibleColours(testSlot)));
    }

    public Object[] parametersForTestPossibleValues() {
        return $(
                $(null, 1, v(1, 2, 3, 4, 5)),
                $(v(1), 1, v(2, 3, 4, 5)),
                $(v(1, 2), 2, v(3, 4, 5)),
                $(v(1, 2, 3), 3, v(4, 5)),
                $(v(1, 2, 3, 4), 4, v(5)),
                $(v(1, 2, 3, 4, 5), 4, v(5))
        );
    }

    @Test
    @Parameters(method = "parametersForTestPossibleValues")
    public void testPossibleValues(Integer[] values, int testSlot, Integer[] expected) {
        setKnowledge(hand, values);
        assertEquals(set(expected), set(hand.getPossibleValues(testSlot)));
    }

    public Object[] parametersForTestIsPossible() {
        return $(
                $(0, new Card(1, BLUE), v(1, null, null, null), c(BLUE, null, null, null), true),
                $(0, new Card(1, BLUE), v(1, 5, 2, 3, 2), c(BLUE, RED, WHITE, GREEN, ORANGE), true),
                $(0, new Card(1, BLUE), v(null, 2, 3, 4, 5), c(BLUE, RED, WHITE, GREEN, ORANGE), true)
        );
    }

    @Test
    @Parameters(method = "parametersForTestIsPossible")
    public void testIsPossible(int slot, Card card, Integer[] values, CardColour[] colours, boolean expected) {
        setKnowledge(hand, values, colours);
        assertEquals(expected, hand.isPossible(slot, card));
    }

    public <T> Set<T> set(T[] values) {
        HashSet<T> set = new HashSet<T>();
        set.addAll(Arrays.asList(values));
        return set;
    }

    public Set<Integer> set(int... values) {
        HashSet<Integer> set = new HashSet<>();
        for (int i : values) {
            set.add(i);
        }
        return set;
    }
}
