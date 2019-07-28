package com.fossgalaxy.games.fireworks.state;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.CardPlayed;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestNoLifeState {

    @Test
    public void testScoreIsOKIfLivesRemaining(){
        GameState state = new NoLifeState(3);

        // create some fake cards for us to test with
        state.setCardAt(0, 1, new Card(1, CardColour.RED));
        state.setCardAt(1, 0, new Card(2, CardColour.BLUE));

        // play the red 1
        Action action = new PlayCard(1);
        action.apply(0, state);

        assertEquals(1, state.getScore());
    }

    @Test
    public void testScoreIsZeroIfLivesRemaining(){
        GameState state = new NoLifeState(3);

        // create some fake cards for us to test with
        state.setCardAt(0, 1, new Card(1, CardColour.RED));
        state.setCardAt(1, 0, new Card(2, CardColour.BLUE));

        // play the red 1
        Action action = new PlayCard(1);
        action.apply(0, state);

        // nuke the lives
        state.setLives(0);

        assertEquals(0, state.getScore());
    }

}
