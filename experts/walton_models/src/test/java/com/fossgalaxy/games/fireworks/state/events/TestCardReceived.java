package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by webpigeon on 12/12/16.
 */
public class TestCardReceived {

    @Test
    public void testConstructor() {
        final boolean present = false;
        final int playerID = 1;
        final int slotID = 1;

        CardReceived instance = new CardReceived(playerID, slotID, present, GameEvent.UNKNOWN_TURN);

        assertEquals(present, instance.isReceived());
        assertEquals(playerID, instance.getPlayerId());
        assertEquals(slotID, instance.getSlotId());
        assertEquals(true, instance.isVisibleTo(playerID));
    }

    @Test
    public void testApplyFalse() {
        final boolean present = false;
        final int playerID = 1;
        final int slotID = 1;

        CardReceived instance = new CardReceived(playerID, slotID, present, GameEvent.UNKNOWN_TURN);

        Hand hand = mock(Hand.class);
        GameState state = mock(GameState.class);
        when(state.getHand(playerID)).thenReturn(hand);

        instance.apply(state, playerID);

        verify(hand).setHasCard(slotID, present);
    }

    @Test
    public void testApplyTrue() {
        final boolean present = true;
        final int playerID = 1;
        final int slotID = 1;

        CardReceived instance = new CardReceived(playerID, slotID, present, GameEvent.UNKNOWN_TURN);

        Hand hand = mock(Hand.class);
        GameState state = mock(GameState.class);
        when(state.getHand(playerID)).thenReturn(hand);

        instance.apply(state, playerID);

        verify(hand).setHasCard(slotID, present);
    }


}
