package com.fossgalaxy.games.fireworks.ai.rule.finesse;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.CardInfo;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.state.events.MessageType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 11/05/17.
 */
public class PlayFinesseTold extends AbstractRule {
    private static final List<MessageType> WHITELIST = Arrays.asList(MessageType.CARD_PLAYED, MessageType.CARD_INFO_VALUE, MessageType.CARD_INFO_COLOUR, MessageType.CARD_DISCARDED);
    private static final List<MessageType> TELL = Arrays.asList(MessageType.CARD_INFO_COLOUR, MessageType.CARD_INFO_VALUE);

    @Override
    public boolean canFire(int playerID, GameState state) {
        if(state.getPlayerCount() == 2) return false;
        return super.canFire(playerID, state);
    }

    @Override
    public Action execute(int playerID, GameState state) {

        LinkedList<GameEvent> history = state.getHistory();
        List<GameEvent> filteredHistory = history.stream().filter(e -> WHITELIST.contains(e.getEvent())).collect(Collectors.toList());

        //if the history was at least 2 long
        if (filteredHistory.size() < 2) {
            return null;
        }

        //if the first was not a play && the second was not a tell
        GameEvent first = filteredHistory.get(filteredHistory.size()-1);
        GameEvent second = filteredHistory.get(filteredHistory.size()-2);
        if (!first.getEvent().equals(MessageType.CARD_PLAYED) || !TELL.contains(second.getEvent())) {
            return null;
        }

        CardInfo info = (CardInfo)second;
        int toldPlayer = info.getPlayerTold();
        Integer[] slots = info.getSlots();

        //was not told uniquely about my cards
        if (toldPlayer != playerID || !info.isUnique()) {
            return null;
        }

        //guess I play it then.
        return new PlayCard(slots[0]);
    }
}
