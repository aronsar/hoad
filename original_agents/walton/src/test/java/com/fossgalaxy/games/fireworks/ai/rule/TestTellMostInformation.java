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
import static org.junit.Assert.assertNotNull;

/**
 * Created by piers on 09/12/16.
 */
public class TestTellMostInformation {

    private BasicState state;
    private TellMostInformation instance;

    @Before
    public void setup() {
        this.state = new BasicState(3);
        this.state.init();
        this.instance = new TellMostInformation();

        /*for(int player = 0;player < state.getPlayerCount(); player++){
            for(int slot = 0; slot < state.getHandSize(); slot++){
                state.getHand(player).setHasCard(slot, true);
            }
        }*/
    }

    @Test
    public void testTellsMostInformationColour() {
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(5, CardColour.BLUE));

        state.getHand(2).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(2).setCard(1, new Card(2, CardColour.RED));
        state.getHand(2).setCard(2, new Card(3, CardColour.GREEN));
        state.getHand(2).setCard(3, new Card(4, CardColour.ORANGE));
        state.getHand(2).setCard(4, new Card(5, CardColour.GREEN));

        // PLayer 0 tells best answer is to tell player 1 about Blue (5 card)
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(TellColour.class, action.getClass());
        TellColour tellColour = (TellColour) action;
        assertEquals(CardColour.BLUE, tellColour.colour);
        assertEquals(1, tellColour.player);
    }

    @Test
    public void testTellsMostInformationColourWhenAlreadyKnowsValues(){
        instance = new TellMostInformation(true);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(1, CardColour.BLUE));

        state.getHand(2).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(2).setCard(1, new Card(2, CardColour.RED));
        state.getHand(2).setCard(2, new Card(3, CardColour.GREEN));
        state.getHand(2).setCard(3, new Card(4, CardColour.ORANGE));
        state.getHand(2).setCard(4, new Card(5, CardColour.GREEN));
        // Equal odds on Colour or Value
        state.getHand(1).setKnownValue(1, new Integer[]{0});
        // now it is 5 pieces for colour and 4 for value
        // PLayer 0 tells best answer is to tell player 1 about Blue (5 card)
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(TellColour.class, action.getClass());
        TellColour tellColour = (TellColour) action;
        assertEquals(CardColour.BLUE, tellColour.colour);
        assertEquals(1, tellColour.player);
    }

    @Test
    public void testTellsMostInformationValueWhenAlreadyKnowsColour(){
        instance = new TellMostInformation(true);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(1, CardColour.BLUE));

        state.getHand(2).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(2).setCard(1, new Card(2, CardColour.RED));
        state.getHand(2).setCard(2, new Card(3, CardColour.GREEN));
        state.getHand(2).setCard(3, new Card(4, CardColour.ORANGE));
        state.getHand(2).setCard(4, new Card(5, CardColour.GREEN));

        // Equal odds on Colour or Value
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0});
        // Not is is 5 pieces for number and 4 for colour.

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(TellValue.class, action.getClass());
        TellValue tellValue = (TellValue) action;
        assertEquals(1, tellValue.value);
        assertEquals(1, tellValue.player);
    }

    @Test
    public void testTellsMostInformationValue(){
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.RED));
        state.getHand(1).setCard(2, new Card(1, CardColour.GREEN));
        state.getHand(1).setCard(3, new Card(1, CardColour.ORANGE));
        state.getHand(1).setCard(4, new Card(1, CardColour.GREEN));

        state.getHand(2).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(2).setCard(1, new Card(2, CardColour.RED));
        state.getHand(2).setCard(2, new Card(3, CardColour.GREEN));
        state.getHand(2).setCard(3, new Card(4, CardColour.ORANGE));
        state.getHand(2).setCard(4, new Card(5, CardColour.GREEN));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertNotNull(action);
        assertEquals(TellValue.class, action.getClass());
        TellValue tellValue = (TellValue) action;
        assertEquals(1, tellValue.value);
        assertEquals(1, tellValue.player);
    }


}
