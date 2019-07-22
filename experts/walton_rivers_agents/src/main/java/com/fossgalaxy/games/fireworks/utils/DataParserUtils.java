package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 11/10/16.
 */
public class DataParserUtils {
    /**
     * The number of players that were in this game
     */
    public final int kPlayers;
    public final int kColors;
    public final int kRanks;
    public final int kHandSize;
    public final int kBitsPerCard;
    public final int kMaxDeckSize;
    public final int kMaxInformationTokens;
    public final int kMaxLifeTokens;
    public final int kMoveTypeCounts;

    // utility class - no instances required
    public DataParserUtils(int NumPlayer) {
        this.kPlayers = NumPlayer;
        this.kColors = 5;
        this.kRanks = 5;
        this.kHandSize = getHandSize();
        this.kBitsPerCard = this.kColors * this.kRanks;
        this.kMaxDeckSize = 50;
        this.kMaxInformationTokens = 8;
        this.kMaxLifeTokens = 3;
        this.kMoveTypeCounts = 4;
    }

    public int getHandSize() {
        if (this.kPlayers < 4) {
            return 5;
        } else {
            return 4;
        }
    }

    public int getHandSectionLength() {
        return (this.kPlayers - 1) * this.kHandSize * this.kBitsPerCard + this.kPlayers;
    }

    public int getBoardSectionLength() {
        return this.kMaxDeckSize - this.kPlayers * this.kHandSize + this.kColors * this.kRanks
                + this.kMaxInformationTokens + this.kMaxLifeTokens;
    }

    public int getDiscardSectionLength() {
        return this.kMaxDeckSize;
    }

    public int getLastActionSectionLength() {
        return this.kPlayers + this.kMoveTypeCounts + this.kPlayers + this.kColors + this.kRanks + this.kHandSize
                + this.kHandSize + this.kBitsPerCard + 2;
    }

    public int getCardKnowledgeSectionLength() {
        return this.kPlayers * this.kHandSize * (this.kBitsPerCard + this.kColors + this.kRanks);
    }

    public int getObservationShape() {
        return getHandSectionLength() + getBoardSectionLength() + getDiscardSectionLength()
                + getLastActionSectionLength() + getCardKnowledgeSectionLength();
    }

    // TODO
    public void encodeHandSection(GameState state, int offset, Vector<Integer> obs) {

    }

    // TODO
    public void encodeBoardSection(GameState state, int offset, Vector<Integer> obs) {

    }

    // TODO
    public void encodeDiscardSection(GameState state, int offset, Vector<Integer> obs) {

    }

    // TODO
    public void encodeLastActionSection(GameState state, int offset, Vector<Integer> obs) {

    }

    // TODO
    public void encodeCardKnowledgeSection(GameState state, int offset, Vector<Integer> obs) {

    }

    public void writeObservation(GameState state) {
        int offset = 0;
        Vector<Integer> obs = new Vector<Integer>(getObservationShape());

        encodeHandSection(state, offset, obs);
        encodeBoardSection(state, offset, obs);
        encodeDiscardSection(state, offset, obs);
        encodeLastActionSection(state, offset, obs);
        encodeCardKnowledgeSection(state, offset, obs);
    }

}
