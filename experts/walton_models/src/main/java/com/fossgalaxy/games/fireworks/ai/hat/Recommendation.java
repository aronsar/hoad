package com.fossgalaxy.games.fireworks.ai.hat;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;

/**
 * Hat Guessing recommendation protocol.
 */
public enum Recommendation {
    PLAY_SLOT_1(new PlayCard(0), true),
    PLAY_SLOT_2(new PlayCard(1), true),
    PLAY_SLOT_3(new PlayCard(2), true),
    PLAY_SLOT_4(new PlayCard(3), true),
    DISCARD_SLOT_1(new DiscardCard(0), false),
    DISCARD_SLOT_2(new DiscardCard(1), false),
    DISCARD_SLOT_3(new DiscardCard(2), false),
    DISCARD_SLOT_4(new DiscardCard(3), false);

    public final Action recommended;
    private final boolean isPlayAction;

    Recommendation(Action recommended, boolean play) {
        this.recommended = recommended;
        this.isPlayAction = play;
    }

    public static Action encode(Recommendation recommendation, int myID, GameState state) {
        //in order to avoid rule violations, we need to make sure our
        //tell actions are legal, thus we need to use a card in the
        //hand to do this.
        if (recommendation.ordinal() < 4) {
            //first four, encode as rank (value)
            for (int playerID = 0; playerID < HatGuessing.ENCODING[myID].length; playerID++) {
                if (HatGuessing.ENCODING[myID][playerID] == recommendation.ordinal()) {
                    for (int i = 0; i < 4; i++) {
                        Card cardToUse = state.getHand(playerID).getCard(i);
                        if (cardToUse == null) continue;
                        return new TellValue(playerID, cardToUse.value);
                    }
                }
            }
        } else {
            //second four, encode has suit (colour)
            for (int playerID = 0; playerID < HatGuessing.ENCODING[myID].length; playerID++) {
                if (HatGuessing.ENCODING[myID][playerID] == recommendation.ordinal() - 4) {
                    for (int i = 0; i < 4; i++) {
                        Card cardToUse = state.getHand(playerID).getCard(i);
                        if (cardToUse == null) continue;
                        return new TellColour(playerID, cardToUse.colour);
                    }
                }
            }
        }
        throw new IllegalStateException("This guy had no cards - is he actually playing or spectating?");
    }

    public static Recommendation playSlot(int slot) {
        Recommendation[] recs = Recommendation.values();
        return recs[slot];
    }

    public static Recommendation discardSlot(int slot) {
        Recommendation[] recs = Recommendation.values();
        return recs[4 + slot];
    }

    public boolean isPlay() {
        return isPlayAction;
    }

    public boolean isDiscard() {
        return !isPlayAction;
    }
}
