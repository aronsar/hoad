package com.fossgalaxy.games.fireworks.ai.rule.logic;

import com.fossgalaxy.games.fireworks.state.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fossgalaxy.games.fireworks.state.CardColour.*;
import static com.fossgalaxy.games.fireworks.state.TestBasicHand.v;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 02/12/16.
 */
@RunWith(JUnitParamsRunner.class)
public class testHandUtils {

    private BasicState state = new BasicState(2);

    @Before
    public void setup() {
        state = new BasicState(2);
        state.setInformation(7);
    }

    public Object[] parametersForHighestScorePossible() {
        return $(
                $(1, 0, 5),
                $(1, 3, 0),
                $(2, 2, 1),
                $(3, 2, 2),
                $(4, 2, 3),
                $(5, 1, 4)
        );
    }

    @Test
    @Parameters(method = "parametersForHighestScorePossible")
    public void testHighestScorePossible(int cardValue, int numToRemove, int maxScore) {
        state.init();
        final CardColour colour = BLUE;
        for (int i = 0; i < numToRemove; i++) {
            state.addToDiscard(new Card(cardValue, colour));
        }

        assertEquals(maxScore, HandUtils.getHighestScorePossible(state, colour));
    }

    public Object[] parametersForGetTableScores() {
        return $(
                $(v(1, 2, 3, 4, 5), v(1, 2, 3, 4, 5)),
                $(v(1, 1, 1, 1, 1), v(1, 1, 1, 1, 1))
        );
    }

    @Test
    @Parameters(method = "parametersForGetTableScores")
    public void testGetTableScores(Integer[] scores, Integer[] expected) {
        setTableValues(scores);

        Integer[] result = HandUtils.getTableScores(state);

        assertArrayEquals(expected, result);
    }

    public Object[] parametersForGetMinTableValue() {
        return $(
                $(v(1, 1, 1, 1, 1), 1),
                $(v(1, 1, 1, 1, 2), 1),
                $(v(1, 1, 1, 2, 2), 1),
                $(v(1, 1, 2, 2, 2), 1),
                $(v(1, 2, 2, 2, 2), 1),
                $(v(2, 2, 2, 2, 2), 2),
                $(v(0, 1, 1, 1, 1), 0)
        );
    }

    public void testGetMinTableValue(Integer[] scores, int expected) {
        setTableValues(scores);

        assertEquals(expected, HandUtils.getMinTableValue(state));
    }

    private void setTableValues(Integer[] scores) {
        for (int i = 0; i < scores.length; i++) {
            state.setTableValue(CardColour.values()[i], scores[i]);
        }
    }

    public Object[] parametersForIsSafeBecauseValueLowerThanMinOnTable() {
        return $(
                $(v(1, 1, 1, 1, 1), 1, true),
                $(v(1, 1, 1, 1, 0), 1, false),
                $(v(2, 2, 2, 2, 2), 1, true)
        );
    }

    @Test
    @Parameters(method = "parametersForIsSafeBecauseValueLowerThanMinOnTable")
    public void testIsSafeBecauseValueLowerThanMinOnTable(Integer[] scores, Integer testValue, boolean expected) {
        setTableValues(scores);
        assertEquals(expected, HandUtils.isSafeBecauseValueLowerThanMinOnTable(state, testValue));
    }

    public Object[] parametersForIsSafeBecauseFiveAlreadyPlayed() {
        return $(
                $(v(5, 5, 5, 5, 5), RED, true),
                $(v(5, 5, 5, 5, 5), BLUE, true),
                $(v(5, 5, 5, 5, 5), GREEN, true),
                $(v(5, 5, 5, 5, 5), ORANGE, true),
                $(v(5, 5, 5, 5, 5), WHITE, true),
                $(v(4, 5, 5, 5, 5), RED, false),
                $(v(5, 4, 5, 5, 5), BLUE, false),
                $(v(5, 5, 4, 5, 5), GREEN, false),
                $(v(5, 5, 5, 4, 5), ORANGE, false),
                $(v(5, 5, 5, 5, 4), WHITE, false)
        );
    }

    @Test
    @Parameters(method = "parametersForIsSafeBecauseFiveAlreadyPlayed")
    public void testIsSafeBecauseFiveAlreadyPlayed(Integer[] scores, CardColour testColour, boolean expected) {
        setTableValues(scores);
        assertEquals(expected, HandUtils.isSafeBecauseFiveAlreadyPlayed(state, testColour));
    }

    public Object[] parametersForIsSafeBecauseValueLowerThanPlayed() {
        return $(
                $(v(2, 2, 2, 2, 2), RED, 1, true),
                $(v(2, 2, 2, 2, 2), BLUE, 1, true),
                $(v(2, 2, 2, 2, 2), GREEN, 1, true),
                $(v(2, 2, 2, 2, 2), ORANGE, 1, true),
                $(v(2, 2, 2, 2, 2), WHITE, 1, true),
                $(v(0, 2, 2, 2, 2), RED, 1, false),
                $(v(2, 0, 2, 2, 2), BLUE, 1, false),
                $(v(2, 2, 0, 2, 2), GREEN, 1, false),
                $(v(2, 2, 2, 0, 2), ORANGE, 1, false),
                $(v(2, 2, 2, 2, 0), WHITE, 1, false)
        );
    }

    @Test
    @Parameters(method = "parametersForIsSafeBecauseValueLowerThanPlayed")
    public void testIsSafeBecauseValueLowerThanPlayed(Integer[] scores, CardColour testColour, Integer testValue, boolean expected) {
        setTableValues(scores);
        assertEquals(expected, HandUtils.isSafeBecauseValueLowerThanPlayed(state, testColour, testValue));
    }

    public Object[] parametersForTestHasUnidentifiedCard(){
        return $(
            $(v(1, 2, 3, 4, 5), v(null, null, null, null, null), 1, 0),
            $(v(1, 2, 3, 4, 5), v(null, null, null, null, null), 2, 1),
            $(v(1, 2, 3, 4, 5), v(null, null, null, null, null), 3, 2),
            $(v(1, 2, 3, 4, 5), v(null, null, null, null, null), 4, 3),
            $(v(1, 2, 3, 4, 5), v(null, null, null, null, null), 5, 4),
            $(v(1, 1, 3, 4, 5), v(1, null, null, null, null), 1, 1),
            $(v(1, 1, 3, 4, 5), v(1, null, null, null, null), 2, -1)

            );
    }

    @Test
    @Parameters(method = "parametersForTestHasUnidentifiedCard")
    public void testHasUnidentifiedCard(Integer[] handValues, Integer[] knownValues, int value, int expected){
        Hand hand = new TimedHand(handValues.length);

        for(int slot = 0; slot < hand.getSize(); slot++){
            hand.setCard(slot, new Card(handValues[slot], RED));
            //hand.setHasCard(slot, true);
            if(knownValues[slot] != null){
                hand.setKnownValue(knownValues[slot], new Integer[]{slot});
            }
        }

        assertEquals(expected, HandUtils.hasUnidentifiedCard(hand, value));


    }
}
