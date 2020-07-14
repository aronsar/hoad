package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.simple.PlayIfCertain;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 23/11/16.
 */
public class TestPlayIfCertain {

    private BasicState state;
    private PlayIfCertain instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        instance = new PlayIfCertain();
    }

    @Test
    public void testPlayIfCertain(){
       state.getHand(0).setCard(0, new Card(1, CardColour.RED));

        state.getHand(0).setKnownColour(CardColour.RED, new Integer[]{0});
        state.getHand(0).setKnownValue(1, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(PlayCard.class, action.getClass());

    }

    @Test
    public void testPlayNoCertain(){
        state.getHand(0).setCard(0, new Card(1, CardColour.BLUE));

        assertEquals(false, instance.canFire(0, state));
    }

}
