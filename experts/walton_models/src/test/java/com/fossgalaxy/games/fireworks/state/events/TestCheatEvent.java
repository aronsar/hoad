package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by webpigeon on 12/12/16.
 */
public class TestCheatEvent {

    @Test
    public void testPlayerMatches() {
        int playerID = 0;
        Hand hand = mock(Hand.class);
        CheatEvent event = new CheatEvent(playerID, hand, GameEvent.UNKNOWN_TURN);

        assertEquals(true, event.isVisibleTo(playerID));
        assertEquals(false, event.isVisibleTo(1));
    }

    @Test(expected = NullPointerException.class)
    public void testNullHandCausesException() {
        int playerID = 0;
        Hand hand = null;
        new CheatEvent(playerID, hand, GameEvent.UNKNOWN_TURN);
    }

    @Test
    public void testApply() {
        final int playerID = 1;

        //generate the hand to use
        Card blueOne = new Card(1, CardColour.BLUE);
        Card redTwo = new Card(2, CardColour.RED);
        Card blueThree = new Card(3, CardColour.BLUE);

        Hand handToClone = mock(Hand.class);
        when(handToClone.getSize()).thenReturn(3);
        when(handToClone.getCard(0)).thenReturn(blueOne);
        when(handToClone.getCard(1)).thenReturn(redTwo);
        when(handToClone.getCard(2)).thenReturn(blueThree);

        CheatEvent event = new CheatEvent(playerID, handToClone, GameEvent.UNKNOWN_TURN);

        //generate a mocked out state
        Hand handToReplace = mock(Hand.class);
        when(handToReplace.getSize()).thenReturn(3);

        GameState state = mock(GameState.class);
        when(state.getHand(playerID)).thenReturn(handToReplace);

        event.apply(state);

        //check that the cheat event generates the correct responses
        verify(handToReplace).setKnownColour(CardColour.BLUE, new Integer[]{0,2});
        verify(handToReplace).setKnownColour(CardColour.RED, new Integer[]{1});
        verify(handToReplace).setKnownValue(1, new Integer[]{0});
        verify(handToReplace).setKnownValue(2, new Integer[]{1});
        verify(handToReplace).setKnownValue(3, new Integer[]{2});
    }

}
