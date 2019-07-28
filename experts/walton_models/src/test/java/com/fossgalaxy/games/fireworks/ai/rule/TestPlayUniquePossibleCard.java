package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by piers on 18/05/17.
 */
public class TestPlayUniquePossibleCard {

    private PlayUniquePossibleCard instance;
    private BasicState state;

    @Before
    public void setup(){
        instance = new PlayUniquePossibleCard();
        state = new BasicState(3);
        state.init();
        // Set the cards
        Hand hand = state.getHand(0);
        hand.setCard(0, new Card(1, CardColour.BLUE));
        hand.setCard(1, new Card(2, CardColour.BLUE));
        hand.setCard(2, new Card(3, CardColour.BLUE));
        hand.setCard(3, new Card(4, CardColour.BLUE));
        hand.setCard(4, new Card(5, CardColour.BLUE));

        hand = state.getHand(1);
        hand.setCard(0, new Card(1, CardColour.RED));
        hand.setCard(1, new Card(2, CardColour.RED));
        hand.setCard(2, new Card(3, CardColour.RED));
        hand.setCard(3, new Card(4, CardColour.RED));
        hand.setCard(4, new Card(5, CardColour.RED));

        hand = state.getHand(2);
        hand.setCard(0, new Card(1, CardColour.GREEN));
        hand.setCard(1, new Card(2, CardColour.GREEN));
        hand.setCard(2, new Card(3, CardColour.GREEN));
        hand.setCard(3, new Card(4, CardColour.GREEN));
        hand.setCard(4, new Card(5, CardColour.GREEN));
    }

    // Tell player 1 about exactly one card, does he play it?
    @Test
    public void testTellExactlyOneAndTheyPlayIt(){
        TellValue tellValue = new TellValue(1, 1);
        tellValue.apply(0, state);


        assertTrue(instance.canFire(1, state));
        Action action = instance.execute(1, state);
        assertNotNull(action);
        assertEquals(PlayCard.class, action.getClass());
        PlayCard playCard = (PlayCard) action;
        assertEquals(0, playCard.slot);
    }

    @Test
    public void testTellMoreThanOneCardAndTheyIgnoreIt(){
        TellColour tellColour = new TellColour(1, CardColour.RED);
        tellColour.apply(0, state);

        assertFalse(instance.canFire(1, state));
    }

    @Test
    public void testNoTellNoPlay(){
        assertFalse(instance.canFire(1, state));
    }

    @Test
    public void testTellTwicePlayTwice(){
        TellValue tellValue = new TellValue(2, 1);

        tellValue.apply(0, state);
        new TellValue(2, 2).apply(1, state);

        assertTrue(instance.canFire(2, state));
        Action action = instance.execute(2, state);
        assertNotNull(action);
        assertEquals(PlayCard.class, action.getClass());

        new PlayCard(3).apply(2, state);
        new TellColour(1, CardColour.RED);
        new TellColour(2, CardColour.GREEN);

        assertTrue(instance.canFire(2, state));
        action = instance.execute(2, state);
        assertNotNull(action);
        assertEquals(PlayCard.class, action.getClass());


    }
}
