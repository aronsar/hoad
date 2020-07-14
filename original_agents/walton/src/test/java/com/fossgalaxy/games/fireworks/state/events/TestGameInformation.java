package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.BasicState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by piers on 25/11/16.
 */
public class TestGameInformation {

    @Test
    public void testGameInformation(){
        GameInformation information = new GameInformation(2, 5, 6, 2);
        BasicState state = new BasicState(2);
        information.apply(state);

        assertEquals(6, state.getInfomation());
        assertEquals(2, state.getLives());
    }
}
