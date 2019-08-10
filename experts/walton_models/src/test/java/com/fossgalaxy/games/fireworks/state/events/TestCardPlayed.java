package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 24/11/16.
 */
public class TestCardPlayed {

    private BasicState state;

    @Before
    public void setup(){
        state = new BasicState(2);
        state.init();
    }

    @Test
    public void testPlayCardValid(){
        state.getHand(0).setCard(1, new Card(1, CardColour.BLUE));
        CardPlayed cardPlayed = new CardPlayed(0, 1, CardColour.BLUE, 1, GameEvent.UNKNOWN_TURN);
        cardPlayed.apply(state);

        assertEquals(1, state.getScore());
        assertEquals(1, state.getTableValue(CardColour.BLUE));
        assertEquals(null, state.getHand(0).getCard(1));
        assertEquals(state.getStartingInfomation(), state.getInfomation());
        assertEquals(state.getStartingLives(), state.getLives());
    }

    @Test
    public void testPlayCardInvalid(){
        state.getHand(0).setCard(1, new Card(2, CardColour.BLUE));
        CardPlayed cardPlayed = new CardPlayed(0, 2, CardColour.BLUE, 2, GameEvent.UNKNOWN_TURN);
        cardPlayed.apply(state);

        assertEquals(0, state.getScore());
        assertEquals(0, state.getTableValue(CardColour.BLUE));
        assertEquals(null, state.getHand(0).getCard(2));
        assertEquals(true, state.getDiscards().contains(new Card(2, CardColour.BLUE)));
        assertEquals(state.getStartingInfomation(), state.getInfomation());
        assertEquals(state.getStartingLives() - 1, state.getLives());
    }

    @Test
    public void testPlayFive(){
        state.getHand(0).setCard(0, new Card(5, CardColour.BLUE));
        state.setTableValue(CardColour.BLUE, 4);
        state.setInformation(7);
        CardPlayed cardPlayed = new CardPlayed(0, 0, CardColour.BLUE, 5, GameEvent.UNKNOWN_TURN);
        cardPlayed.apply(state);
        assertEquals(5, state.getScore());
        assertEquals(5, state.getTableValue(CardColour.BLUE));
        assertEquals(null, state.getHand(0).getCard(0));
        assertEquals(state.getStartingInfomation(), state.getInfomation());
        assertEquals(state.getStartingLives(), state.getLives());
    }

    @Test
    public void testPlayFiveAlreadyAllInformation(){
        state.getHand(0).setCard(0, new Card(5, CardColour.BLUE));
        state.setTableValue(CardColour.BLUE, 4);
        CardPlayed cardPlayed = new CardPlayed(0, 0, CardColour.BLUE, 5, GameEvent.UNKNOWN_TURN);
        cardPlayed.apply(state);
        assertEquals(5, state.getScore());
        assertEquals(5, state.getTableValue(CardColour.BLUE));
        assertEquals(null, state.getHand(0).getCard(0));
        assertEquals(state.getStartingInfomation(), state.getInfomation());
        assertEquals(state.getStartingLives(), state.getLives());
    }
}
