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
 * Created by piers on 23/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class TestTellColour {

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
    public void testTellingSelf(int self, int nPlayers) {
        state = new BasicState(nPlayers);
        state.init();
        TellColour tellColour = new TellColour(self, CardColour.BLUE);
        tellColour.apply(self, state);
    }

    @Test(expected = RulesViolation.class)
    public void testTellingAboutSomethingTheyDontHave(){
        // All reds
        for(int i = 0; i < 5; i++){
            state.getHand(1).setCard(i, new Card(i, CardColour.RED));
        }

        // Tell them blue
        TellColour tellColour = new TellColour(1, CardColour.BLUE);
        tellColour.apply(0, state);
    }

    @Test(expected = RulesViolation.class)
    public void testTellingNoInformation(){
        state.setInformation(0);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        TellColour tellColour = new TellColour(1, CardColour.BLUE);
        tellColour.apply(0, state);
    }

    @Test
    public void testTellingColour(){
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.RED));
        state.getHand(1).setCard(2, new Card(1, CardColour.RED));
        state.getHand(1).setCard(3, new Card(2, CardColour.RED));
        state.getHand(1).setCard(4, new Card(2, CardColour.RED));
        TellColour tellColour = new TellColour(1, CardColour.BLUE);
        tellColour.apply(0, state);
        assertEquals(CardColour.BLUE, state.getHand(1).getKnownColour(0));
        assertEquals(null, state.getHand(1).getKnownColour(1));
        assertEquals(null, state.getHand(1).getKnownColour(2));
        assertEquals(null, state.getHand(1).getKnownColour(3));
        assertEquals(null, state.getHand(1).getKnownColour(4));
    }
}
