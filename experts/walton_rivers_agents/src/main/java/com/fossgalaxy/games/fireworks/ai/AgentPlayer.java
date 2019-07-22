package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.GameType;
import com.fossgalaxy.games.fireworks.state.NoLifeState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper for an agent policy to player.
 * <p>
 * This keeps track of state on behalf of the policy so that the agent can implement the standard interface generally
 * used by game playing agents (get a state, return an action).
 */
public class AgentPlayer implements Player {
    private final String name;
    protected final Agent policy;

    protected GameState state;
    private int playerID;

    /**
     * Create a player with a given name and policy.
     *
     * @param name   the name of this player
     * @param policy the policy this player should use
     */
    public AgentPlayer(String name, Agent policy) {
        this.name = Objects.requireNonNull(name);
        this.policy = Objects.requireNonNull(policy);

        //set the player as not currently playing a game
        this.playerID = -1;
        this.state = null;
    }

    @Override
    public Action getAction() {
        return policy.doMove(playerID, state);
    }

    /**
     * Resolve a turn.
     * 
     * We don't know enouph to apply the action (ie, the deck order and our own cards).
     * Instead, we use the effects of the action (Events) to update our state.
     * Because the action is never executed on our side, our tick counter won't update, so do that to.
     * 
     * @param actor the player who just made a move
     * @param action the move the player made
     * @param events what we saw happen when the move was made
     */
    @Override
    public void resolveTurn(int actor, Action action, List<GameEvent> events) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(events);

        // add the action to the history
        state.addAction(actor, action, events);

        // apply the effects of the actions
        for (GameEvent event : events){
            event.apply(state, this.playerID);
        }

        // tick the state
        state.actionTick();
    }

    @Override
    public void setID(int id, int nPlayers, String[] names, GameType type) {
        assert state == null;
        assert playerID == -1;

        this.playerID = id;
        if  (type.equals(GameType.NO_LIVES_CURRENT)) {
            this.state = new BasicState(nPlayers);
        } else if (type.equals(GameType.NO_LIVES_ZERO)) {
            this.state = new NoLifeState(nPlayers);
        } else {
            throw new IllegalArgumentException("Unsupported game type");
        }
        policy.receiveID(id, names);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("{name: %s, policy: %s}", name, policy);
    }

}
