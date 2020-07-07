package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 23/11/16.
 */
public class TestTellUnknown {

    private BasicState state;
    private TellUnknown instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
        instance = new TellUnknown();
    }

    @Test
    public void testUnknownCardsToTell() {
        for (int i = 0; i < 5; i++) {
            state.getHand(0).setCard(i, new Card(i, CardColour.RED));
        }

        // Knows nothing about the cards
        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
    }

    @Test
    public void testUnknownCardsToTellKnowsValue() {
        for (int i = 0; i < 5; i++) {
            state.getHand(0).setCard(i, new Card(i, CardColour.RED));
            state.getHand(0).setKnownValue(i, new Integer[]{i});
        }

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(TellColour.class, action.getClass());
    }

    @Test
    public void testUnknownCardsToTellKnowsColour() {
        for (int i = 0; i < 5; i++) {
            state.getHand(0).setCard(i, new Card(i, CardColour.RED));
        }
        state.getHand(0).setKnownColour(CardColour.RED, new Integer[]{0, 1, 2, 3, 4});

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(TellValue.class, action.getClass());
    }

    @Test
    public void testNoUnknownCardsToTell() {
        for (int i = 0; i < 5; i++) {
            state.getHand(0).setCard(i, new Card(i, CardColour.RED));
            state.getHand(0).setKnownValue(i, new Integer[]{i});
        }
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0, 1, 2, 3, 4});
        assertEquals(false, instance.canFire(1, state));
    }

    @Test
    public void testWithNullInHand(){
        state.getHand(0).setCard(0, null);
        state.getHand(0).setCard(1, new Card(1, CardColour.BLUE));

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
    }
}
