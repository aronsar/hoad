package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.CardDrawn;
import com.fossgalaxy.games.fireworks.state.events.CheatEvent;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Game runner that allows the agents to know what cards are in their hands.
 */
public class GameRunnerCheat extends GameRunner {

    public GameRunnerCheat(String gameID, int expectedPlayers) {
        super(gameID, expectedPlayers);
    }

    /**
     * Tell the players about an action that has occurred
     *
     * @param actor the player who performed the action
     * @param action the action the player performed
     * @param events the events that resulted from that action
     */
    protected void notifyAction(int actor, Action action, Collection<GameEvent> events) {

        for (int i = 0; i < players.length; i++) {
            // filter events to just those that are visible to the player
            List<GameEvent> visibleEvents = new ArrayList<>();
            for (GameEvent event : events) {
                if (event.isVisibleTo(i)) {
                    visibleEvents.add(event);
                }

                if (event instanceof CardDrawn) {
                    visibleEvents.add(new CheatEvent(i, state.getHand(i), event.getTurnNumber()));
                }
            }

            players[i].resolveTurn(actor, action, visibleEvents);
        }

    }

}
