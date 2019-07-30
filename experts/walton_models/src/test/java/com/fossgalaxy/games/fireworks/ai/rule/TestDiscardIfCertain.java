package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.simple.DiscardIfCertain;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 23/11/16.
 */
public class TestDiscardIfCertain {

    private BasicState state;
    private DiscardIfCertain instance;

    @Before
    public void setup(){
        state = new BasicState(2);
        state.setInformation(7);
        instance = new DiscardIfCertain();
    }

    @Test
    public void testDiscardIfCertain(){
        for(CardColour colour : CardColour.values()) {
            state.setTableValue(colour, 1);
            state.getHand(0).setCard(colour.ordinal(), new Card(1, colour));
            state.getHand(0).setKnownColour(colour, new Integer[]{colour.ordinal()});
        }
        state.getHand(0).setKnownValue(1, new Integer[]{0, 1, 2, 3, 4});

        // 0 should discard a card
        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(DiscardCard.class, action.getClass());
    }

    @Test
    public void testWillNotDiscardWhenNotCertain(){
        for(CardColour colour : CardColour.values()) {
            state.setTableValue(colour, 1);
            state.getHand(0).setCard(colour.ordinal(), new Card(1, colour));
            state.getHand(0).setKnownColour(colour, new Integer[]{colour.ordinal()});
        }
//        state.getHand(0).setKnownValue(1, new Integer[]{0, 1, 2, 3, 4});

        assertEquals(false, instance.canFire(0, state));

    }
}
