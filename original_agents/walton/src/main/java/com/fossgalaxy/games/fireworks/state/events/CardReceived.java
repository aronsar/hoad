package com.fossgalaxy.games.fireworks.state.events;

import com.fossgalaxy.games.fireworks.state.GameState;

/**
 * Created by piers on 08/12/16.
 */
public class CardReceived extends GameEvent {

    private final int playerId;
    private final int slotId;
    private final boolean received;

    public CardReceived(int playerId, int slotId, boolean received, int turnNumber) {
        super(MessageType.CARD_RECEIVED, turnNumber);
        this.playerId = playerId;
        this.slotId = slotId;
        this.received = received;
    }

    public boolean isReceived() {
        return received;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getSlotId() {
        return slotId;
    }


    @Override
    public void apply(GameState state, int myPlayerID) {
        assert myPlayerID==this.playerId;
        state.getHand(playerId).setHasCard(slotId, received);
    }

    @Override
    public boolean isVisibleTo(int playerID) {
        return playerID == this.playerId;
    }

    @Override
    public String toString(){
        if (received) {
            return String.format("player %d was informed that slot %d contains a card", playerId, slotId);
        } else {
            return String.format("player %d was informed that slot %d is now empty (no cards left!)", playerId, slotId);
        }
    }
}
