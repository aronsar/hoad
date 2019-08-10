package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
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
 * Created by piers on 22/11/16.
 */
public class TestTellPlayableCardOuter {

    private BasicState state;
    private TellPlayableCardOuter instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        instance = new TellPlayableCardOuter();
    }

    @Test
    public void testTellPlayableCard(){
        // something we want
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));

        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellColour || action instanceof TellValue);
        if(action instanceof  TellColour){
            TellColour tellColour = (TellColour) action;
            assertEquals(true, tellColour.colour == CardColour.BLUE);
        }
        if(action instanceof TellValue){
            TellValue tellValue = (TellValue) action;
            assertEquals(true, tellValue.value == 1);
        }
    }

    @Test
    public void testTellPlayableCardNoPlayableCards(){
        for(CardColour colour : CardColour.values()){
            state.getHand(0).setCard(colour.ordinal(), new Card(1, colour));
            state.setTableValue(colour, 1);
        }
        assertEquals(false, instance.canFire(1, state));
    }

    @Test
    public void testTellPlayableCardKnowValue(){
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(0).setKnownValue(1, new Integer[]{0});
        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(TellColour.class, action.getClass());
    }

    @Test
    public void testTellPlayableCardKnowColour(){
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(0).setKnownColour(CardColour.BLUE, new Integer[]{0});
        assertEquals(true, instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertEquals(true, action != null);
        assertEquals(TellValue.class, action.getClass());
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
