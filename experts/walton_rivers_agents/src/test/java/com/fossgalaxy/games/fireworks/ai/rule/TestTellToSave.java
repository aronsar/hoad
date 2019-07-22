package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestTellToSave {

    private BasicState state;
    private TellToSave instance;

    @Before
    public void setup() {
        state = new BasicState(2);
        state.init();
        instance = new TellToSave();
    }

    @Test
    public void testWontTellUselessCard() {
        state = spy(state);

        when(state.getDiscards()).thenReturn(Arrays.asList(
                new Card(3, CardColour.BLUE),
                new Card(3, CardColour.BLUE),
                new Card(4, CardColour.BLUE)
        ));

        state.getHand(1).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));


        assertEquals(false, instance.canFire(0, state));
    }

    @Test
    public void testWillTellUniqueCard(){
        state = spy(state);
        when(state.getDiscards()).thenReturn(Arrays.asList(
            new Card(4, CardColour.BLUE)
        ));

        state.getHand(1).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));


        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
        if(action instanceof TellColour){
            TellColour tellColour = (TellColour) action;
            assertEquals(CardColour.BLUE, tellColour.colour);
            assertEquals(1, tellColour.player);
        }else if(action instanceof TellValue){
            TellValue tellValue = (TellValue) action;
            assertEquals(4, tellValue.value);
            assertEquals(1, tellValue.player);
        }
    }

    @Test
    public void testWillTellFive(){
        state = spy(state);

        state.getHand(1).setCard(0, new Card(5, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));


        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
        if(action instanceof TellColour){
            TellColour tellColour = (TellColour) action;
            assertEquals(CardColour.BLUE, tellColour.colour);
            assertEquals(1, tellColour.player);
        }else if(action instanceof TellValue){
            TellValue tellValue = (TellValue) action;
            assertEquals(5, tellValue.value);
            assertEquals(1, tellValue.player);
        }
    }

    @Test
    public void testWillNotTellUniqueAlreadyKnownAbout(){
        state = spy(state);
        when(state.getDiscards()).thenReturn(Arrays.asList(
                new Card(4, CardColour.BLUE)
        ));

        state.getHand(1).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));

        state.getHand(1).setKnownValue(4, new Integer[]{0});
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0, 1, 2, 3, 4});

        assertEquals(false, instance.canFire(0, state));
    }

    @Test
    public void testWillNotTellUniqueAlreadyKnownValue(){
        state = spy(state);
        when(state.getDiscards()).thenReturn(Arrays.asList(
                new Card(4, CardColour.BLUE)
        ));

        state.getHand(1).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));

        state.getHand(1).setKnownValue(4, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));
    }

    @Test
    public void testWillNotTellUniqueAlreadyKnownColour(){
        state = spy(state);
        when(state.getDiscards()).thenReturn(Arrays.asList(
                new Card(4, CardColour.BLUE)
        ));

        state.getHand(1).setCard(0, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(2, CardColour.BLUE));

        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0, 1, 2, 3, 4});

        assertEquals(true, instance.canFire(0, state));
    }
}
