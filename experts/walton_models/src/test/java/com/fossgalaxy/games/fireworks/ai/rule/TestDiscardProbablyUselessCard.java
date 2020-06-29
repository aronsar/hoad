package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardProbablyUselessCard;
import com.fossgalaxy.games.fireworks.state.BasicState;
import org.junit.Before;

/**
 * Created by piers on 12/12/16.
 *
 * All i need to do now is concoct a few scenarios wehre cards are easy to work out probability that they are useless.
 */
public class TestDiscardProbablyUselessCard {

    private BasicState state;
    private DiscardProbablyUselessCard instance;

    @Before
    public void setup() {
        this.state = new BasicState(2);
        this.state.init();
    }



}
