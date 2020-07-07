package com.fossgalaxy.games.fireworks.state.actions;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.io.Serializable;
import java.util.List;

public interface Action extends Serializable {

    List<GameEvent> apply(int playerID, GameState state);

    boolean isLegal(int playerID, GameState state);

    ActionType getType();

    int getCardIndex();

    int getTargetOffset();

    int getColor();

    String getColorName();

    int getRank();
}
