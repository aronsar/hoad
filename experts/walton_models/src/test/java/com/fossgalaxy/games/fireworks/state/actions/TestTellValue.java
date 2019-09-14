package com.fossgalaxy.games.fireworks.state.actions;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.RulesViolation;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestTellValue {

    private BasicState state;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
    }

    public Object[] paramsForTellingSelf() {
        return $(
                $(0, 2), $(1, 2),
                $(0, 3), $(1, 3), $(2, 3),
                $(0, 4), $(1, 4), $(2, 4), $(3, 4),
                $(0, 5), $(1, 5), $(2, 5), $(3, 5), $(4, 5)
        );
    }

    @Test(expected = RulesViolation.class)
    @Parameters(method = "paramsForTellingSelf")
    public void testTellingSelf(int self, int nPLayers) {
        state = new BasicState(nPLayers);
        state.init();
        TellValue tellValue = new TellValue(self, 1);
        tellValue.apply(self, state);
    }

    @Test(expected = RulesViolation.class)
    public void testTellingAboutSomethingTheyDontHave() {
        for (CardColour colour : CardColour.values()) {
            state.getHand(1).setCard(colour.ordinal(), new Card(1, colour));
        }
        TellValue tellValue = new TellValue(1, 2);
        tellValue.apply(0, state);
    }

    @Test(expected = RulesViolation.class)
    public void testTellingNoInformation() {
        state.setInformation(0);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        TellValue tellValue = new TellValue(1, 1);
        tellValue.apply(0, state);
    }

    @Test
    public void testTellingValue(){
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(3, CardColour.BLUE));
        TellValue tellValue = new TellValue(1, 1);
        tellValue.apply(0, state);
        assertEquals(1, state.getHand(1).getKnownValue(0).intValue());
        assertEquals(null, state.getHand(1).getKnownValue(1));
        assertEquals(null, state.getHand(1).getKnownValue(2));
        assertEquals(null, state.getHand(1).getKnownValue(3));
        assertEquals(null, state.getHand(1).getKnownValue(4));
    }
}
