package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.random.TellPlayableCard;
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
public class TestTellPlayableCard {

    private BasicState state;
    private TellPlayableCard instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        instance = new TellPlayableCard();
    }

    @Test
    public void testWillTellAPlayableCard(){
        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.BLUE));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellValue || action instanceof TellColour);
    }

    @Test
    public void testWillNotTellWhenNoPlayableCard(){
        for(CardColour colour : CardColour.values()){
            state.getHand(1).setCard(colour.ordinal(), new Card(1, colour));
            state.setTableValue(colour, 1);
        }

        assertEquals(false, instance.canFire(0, state));
    }

    @Test
    public void testWithNullInHand(){
        state.getHand(1).setCard(0, null);
        state.getHand(1).setCard(1, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(1, CardColour.BLUE));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(true, action instanceof TellValue || action instanceof TellColour);
    }
}
