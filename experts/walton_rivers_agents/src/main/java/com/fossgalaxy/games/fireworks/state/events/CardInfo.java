package com.fossgalaxy.games.fireworks.state.events;

import java.util.Collection;

/**
 * Created by webpigeon on 11/05/17.
 */
public abstract class CardInfo extends GameEvent {
    protected final int playerTold;
    protected final int playerTelling;
    protected final Integer[] slots;

    public CardInfo(MessageType id, int playerTold, int playerTelling, Collection<Integer> slotsList, int turnNumber) {
        super(id, turnNumber);
        this.playerTold = playerTold;
        this.playerTelling = playerTelling;

        Integer[] slots = new Integer[slotsList.size()];
        slotsList.toArray(slots);

        this.slots = slots;
    }



    public CardInfo(MessageType id, int playerTold, int playerTelling, Integer[] slots) {
        super(id);
        this.playerTold = playerTold;
        this.playerTelling = playerTelling;
        this.slots = slots;
    }

    public boolean wasToldTo(int playerID) {
        return playerTold == playerID;
    }

    public boolean wasToldBy(int playerID) {
        return playerTelling == playerID;
    }

    public int getPlayerTold() {
        return playerTold;
    }

    public int getPerformer() {
        return playerTelling;
    }

    public boolean isUnique() {
        return slots.length == 1;
    }

    public Integer[] getSlots(){
        return slots;
    }

    @Deprecated
    public int getPlayerId() {
        return getPlayerTold();
    }
}
