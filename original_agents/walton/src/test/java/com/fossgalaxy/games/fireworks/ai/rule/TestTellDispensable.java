package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 01/12/16.
 */
public class TestTellDispensable {

    private TellDispensable instance;
    private BasicState state;

    @Before
    public void setup() {
        this.state = new BasicState(2);
        this.state.init();
        this.instance = new TellDispensable();
        this.state.setInformation(7);
    }

    @Test
    public void testIndicatesDispensableCardsFiveAlreadyPlayed() {
        state.setTableValue(CardColour.BLUE, 5);

        state.getHand(1).setCard(0, new Card(2, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(1, CardColour.GREEN));
        state.getHand(1).setCard(2, new Card(1, CardColour.ORANGE));
        state.getHand(1).setCard(3, new Card(4, CardColour.RED));
        state.getHand(1).setCard(4, new Card(3, CardColour.GREEN));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(TellColour.class, action.getClass());
        TellColour tellColour = (TellColour) action;
        assertEquals(CardColour.BLUE, tellColour.colour);
    }

    @Test
    public void testIndicatesDispensableCardsLowerThanMinOnTable(){
        for(CardColour colour : CardColour.values()){
            state.setTableValue(colour, 2);
        }

        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(5, CardColour.BLUE));

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(TellValue.class, action.getClass());
        TellValue tellValue = (TellValue) action;
        assertEquals(1, tellValue.value);
    }

    @Test
    public void testIndicatesDispensableCardsLowerThanPlayedKnowsValue(){
        state.setTableValue(CardColour.BLUE, 2);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(5, CardColour.BLUE));
        state.getHand(1).setKnownValue(1, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(TellColour.class, action.getClass());
        TellColour tellColour = (TellColour) action;
        assertEquals(CardColour.BLUE, tellColour.colour);
    }

    @Test
    public void testIndicatesDispensableCardsLowerThanPlayedKnowsColour(){
        state.setTableValue(CardColour.BLUE, 2);
        state.getHand(1).setCard(0, new Card(1, CardColour.BLUE));
        state.getHand(1).setCard(1, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(2, new Card(4, CardColour.BLUE));
        state.getHand(1).setCard(3, new Card(3, CardColour.BLUE));
        state.getHand(1).setCard(4, new Card(5, CardColour.BLUE));
        state.getHand(1).setKnownColour(CardColour.BLUE, new Integer[]{0});

        assertEquals(true, instance.canFire(0, state));
        Action action = instance.execute(0, state);
        assertEquals(true, action != null);
        assertEquals(TellValue.class, action.getClass());
        TellValue tellValue = (TellValue) action;
        assertEquals(1, tellValue.value);
    }

}
