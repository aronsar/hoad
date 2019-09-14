package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by piers on 11/05/17.
 */

public class TestTellIllInformed {

    private BasicState state;
    private TellIllInformed instance;

    @Before
    public void setup() {
        state = new BasicState(5);
        instance = new TellIllInformed();

        // Playable card
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
    }


    @Test
    public void testWillTellIllInformedPlayer() {
        assertTrue(instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertTrue(action.getClass() == TellValue.class || action.getClass() == TellColour.class);
        if (action instanceof TellValue) {
            TellValue tellValue = (TellValue) action;
            assertEquals(1, tellValue.value);
        }
        if (action instanceof TellColour) {
            TellColour tellColour = (TellColour) action;
            assertEquals(CardColour.BLUE, tellColour.colour);
        }
    }

    @Test
    public void testWillTellColourKnowsValue() {
        state.getHand(1).setKnownValue(1, new Integer[]{0});
        assertTrue(instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(TellColour.class, action.getClass());
        assertEquals(CardColour.BLUE, ((TellColour) action).colour);
    }

    @Test
    public void testWillTellValueKnowsColour() {
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0});
        assertTrue(instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(TellValue.class, action.getClass());
        assertEquals(1, ((TellValue) action).value);
    }

    @Test
    public void testWillNotTellKnowsCard() {
        state.getHand(1).setKnownValue(1, new Integer[]{0});
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0});
        assertFalse(instance.canFire(0, state));
    }

}
