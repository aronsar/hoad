package com.fossgalaxy.games.fireworks.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.HistoryEntry;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.ActionType;
import com.fossgalaxy.games.fireworks.state.events.CardDiscarded;
import com.fossgalaxy.games.fireworks.state.events.CardDrawn;
import com.fossgalaxy.games.fireworks.state.events.CardInfo;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardInfoValue;
import com.fossgalaxy.games.fireworks.state.events.CardPlayed;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

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
    public int steps;
    public int current_player;
    public Vector<int[]> all_obs;
    public Vector<int[]> all_act;
    public Vector<Integer> all_act_id;
    public Vector<String[]> all_act_info;
    public static Vector<String> start_deck;
    public static Vector<String> start_hands;
    public static int game_num = -1;

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
        this.steps = -1;
        this.current_player = 0;
        this.all_obs = new Vector<int[]>();
        this.all_act = new Vector<int[]>();
        this.all_act_id = new Vector<Integer>();
        this.all_act_info = new Vector<String[]>();
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
    public void encodeHandSection(GameState state, int offset, int[] obs) {
        int start_offset = offset;
        // Encode all other player's hands cards
        for (int player = 0; player < this.kPlayers; player++) {
            // Skip current player's hand deck
            if (player == this.current_player) {
                continue;
            }

            Hand hand = state.getHand(player);
            for (int slot = 0; slot < this.kHandSize; slot++) {
                Card card = hand.getCard(slot);

                if (card != null) {
                    Integer card_rank = card.value;
                    CardColour card_color = card.colour;

                    obs[offset + CardIndex(card_color, card_rank, this.kRanks)] = 1;
                    // For Debug Purpose
                    // System.out.printf("Slot %d, Player %d, Card Color %s %d, Card Rank %d, Card
                    // Index %d\n", slot,
                    // player, card_color, ColorToIndex(card_color), card_rank,
                    // CardIndex(card_color, card_rank, this.kRanks));
                    // System.out.printf("Offset %d, Index: %d\n", offset,
                    // offset + CardIndex(card_color, card_rank, this.kRanks));
                }
                offset += this.kBitsPerCard;
            }
        }

        // For each player, set a bit if their hand is missing a card.
        for (int player = 0; player < this.kPlayers; player++) {
            if (state.getHand(player).getSize() < this.kHandSize) {
                obs[offset + player] = 1;
            }
        }
        offset += this.kPlayers;
        assert (offset - start_offset == getHandSectionLength());
    }

    // TODO
    public void encodeBoardSection(GameState state, int offset, int[] obs) {
        int start_offset = offset;
        for (int i = 0; i < state.getDeck().getCardsLeft(); i++) {
            obs[offset + i] = 1;
        }
        start_offset += (kMaxDeckSize - kHandSize * kPlayers);
        for (CardColour color : CardColour.values()) {
            if (state.getTableValue(color) == 1) {
                obs[offset + state.getTableValue(color) - 1] = 1;
            }
            offset += kRanks;
        }
        // information tokens
        for (int i = 0; i < state.getInformation(); i++) {
            obs[offset + i] = 1;
        }
        offset += state.getStartingInfomation();
        // life tokens
        for (int i = 0; i < state.getLives(); i++) {
            obs[offset + i] = 1;
        }
        offset += state.getStartingLives();
        assert (offset - start_offset == getBoardSectionLength());
    }

    // TODO
    public void encodeDiscardSection(GameState state, int offset, int[] obs) {
        int start_offset = offset;
        int discard_counts[] = new int[(kColors * kRanks) + 1];
        Collection<Card> cards = state.getDiscards();
        for (Card card : cards) {
            discard_counts[CardIndex(card.colour, card.value, this.kRanks)] += 1;
        }
        for (int c = 0; c < kColors; ++c) {
            for (int r = 0; r < kRanks; ++r) {
                int num_discarded = discard_counts[c * kRanks + r];
                for (int i = 0; i < num_discarded; i++) {
                    obs[offset + i] = 1;
                }
                if (r == 0) {
                    offset += 3;
                } else if (r == kRanks - 1) {
                    offset += 1;
                } else {
                    offset += 2;
                }
            }
        }
        assert (offset - start_offset == getDiscardSectionLength());
    }

    // TODO
    public void encodeLastActionSection(GameState state, int offset, int[] obs) {
        int start_offset = offset;
        List<HistoryEntry> moves = state.getActionHistory();
        Iterator movesit = moves.iterator();
        HistoryEntry hist = null;
        List<GameEvent> last_moves = null;
        GameEvent last_move = null;
        if (movesit.hasNext()) {
            hist = (HistoryEntry) movesit.next();
        }
        if (hist != null) {
            last_moves = hist.history;
        }
        if (last_moves != null) {
            for (Iterator i = last_moves.iterator(); i.hasNext();) {
                GameEvent move = (GameEvent) i.next();
                if (!(move instanceof CardDrawn)) {
                    last_move = move;
                    break;
                }
            }
        }
        if (last_move == null) {
            offset += getLastActionSectionLength();
        } else {
            obs[offset + hist.playerID] = 1;
            offset += kPlayers;
            if (last_move instanceof CardPlayed) {
                obs[offset] = 1;
            } else if (last_move instanceof CardDiscarded) {
                obs[offset + 1] = 1;
            } else if (last_move instanceof CardInfoColour) {
                obs[offset + 2] = 1;
            } else if (last_move instanceof CardInfoValue) {
                obs[offset + 3] = 1;
            }
            offset += 4;
            if (last_move instanceof CardInfoColour || last_move instanceof CardInfoValue) {
                obs[offset + ((CardInfo) last_move).getPlayerTold()] = 1;
            }
            offset += kPlayers;
            if (last_move instanceof CardInfoColour) {
                obs[offset + ((CardInfoColour) last_move).getColour().ordinal()] = 1;
            }
            offset += kColors;
            if (last_move instanceof CardInfoValue) {
                obs[offset + ((CardInfoValue) last_move).getValue()] = 1;
            }
            offset += kRanks;
            if (last_move instanceof CardInfoColour || last_move instanceof CardInfoValue) {
                for (Integer i : ((CardInfo) last_move).getSlots()) {
                    obs[offset + i] = 1;
                }
            }
            offset += kHandSize;
            if (last_move instanceof CardDiscarded) {
                obs[offset + ((CardDiscarded) last_move).getSlotId()] = 1;
            } else if (last_move instanceof CardPlayed) {
                obs[offset + ((CardPlayed) last_move).getSlotId()] = 1;
            }
            offset += kHandSize;
            if (last_move instanceof CardDiscarded) {
                obs[offset + CardIndex(((CardDiscarded) last_move).getColour(), ((CardDiscarded) last_move).getValue(),
                        kRanks)] = 1;
            } else if (last_move instanceof CardPlayed) {
                obs[offset + CardIndex(((CardPlayed) last_move).getColour(), ((CardPlayed) last_move).getValue(),
                        kRanks)] = 1;
            }
            offset += kBitsPerCard;
            if (last_move instanceof CardPlayed) {
                if (((CardPlayed) last_move).scored) {
                    obs[offset] = 1;
                }
                if (((CardPlayed) last_move).information) {
                    obs[offset + 1] = 1;
                }
            }
            offset += 2;
        }
        assert (offset - start_offset == getLastActionSectionLength());
    }

    // TODO
    public void encodeCardKnowledgeSection(GameState state, int offset, int[] obs) {
        int start_offset = offset;
        for (int player = 0; player < this.kPlayers; player++) {
            Hand hand = state.getHand(player);
            Card cards[] = new Card[hand.getSize()];
            for (int i = 0; i < hand.getSize(); i++) {
                cards[i] = hand.getCard(i);
            }
            int num_cards = 0;
            for (int i = 0; i < hand.getSize(); i++) {
                if (hand.isPossible(i, cards[i])) {
                    obs[offset + CardIndex(cards[i].colour, cards[i].value, this.kRanks)] = 1;
                }
                offset += kBitsPerCard;
                // Add bits for explicitly revealed colors and ranks.
                if (hand.getKnownColour(i) != null) {
                    obs[offset + ColorToIndex(cards[i].colour)] = 1;
                }
                offset += kColors;
                if (hand.getKnownValue(i) != null) {
                    obs[offset + cards[i].value] = 1;
                }
                offset += kRanks;
                ++num_cards;
            }
            // A player's hand can have fewer cards than the initial hand size.
            // Leave the bits for the absent cards empty (adjust the offset to skip
            // bits for the missing cards).
            if (num_cards < kHandSize) {
                offset += (kHandSize - num_cards) * (kBitsPerCard + kColors + kRanks);
            }
        }
        assert (offset - start_offset == getCardKnowledgeSectionLength());
    }

    public void initSteps() {
        this.steps += 1;
        this.current_player = this.steps % this.kPlayers;
        // Sanity Check
        assert (this.current_player >= 0 && this.current_player <= 4);
    }

    public int[] writeObservation(GameState state, int[] obs_vec) {
        int offset = 0;
        encodeHandSection(state, offset, obs_vec);
        encodeBoardSection(state, offset, obs_vec);
        encodeDiscardSection(state, offset, obs_vec);
        encodeLastActionSection(state, offset, obs_vec);
        encodeCardKnowledgeSection(state, offset, obs_vec);

        return obs_vec;
    }

    /**
     * Handles Action
     */
    public int getMaxDiscardMoves() {
        return this.kHandSize;
    }

    public int getMaxPlayMoves() {
        return this.kHandSize;
    }

    public int getMaxRevealColorMoves() {
        return (this.kPlayers - 1) * this.kColors;
    }

    public int getMaxRevealRankMoves() {
        return (this.kPlayers - 1) * this.kRanks;
    }

    public int getMaxMoves() {
        return getMaxDiscardMoves() + getMaxPlayMoves() + getMaxRevealColorMoves() + getMaxRevealRankMoves();
    }

    public int getMoveUID(Action action) {
        ActionType type = action.getType();
        // System.out.printf("Acion: %s %s %s\n", action.getType(),
        // action.getColorName(), action.getRank());

        String[] act_info = new String[3];
        act_info[0] = String.format("%s", action.getType());
        act_info[1] = action.getColorName();
        act_info[2] = String.format("%s", action.getRank());
        this.all_act_info.add(act_info);

        int uid = -1;
        switch (type) {
        case DISCARD:
            uid = action.getCardIndex();
            // System.out.printf("DISCARD %d\n", uid);
            break;
        case PLAY:
            uid = getMaxDiscardMoves() + action.getCardIndex();
            // System.out.printf("PLAY %d\n", uid);

            break;
        case REVEAL_COLOR:
            assert (action.getTargetOffset() != -1);
            assert (action.getColor() != -1);

            uid = getMaxDiscardMoves() + getMaxPlayMoves() + Math.max(action.getTargetOffset() - 1, 0) * this.kColors
                    + action.getColor();
            // System.out.printf("REVEAL_COLOR %d\n", uid);
            break;
        case REVEAL_RANK:
            assert (action.getTargetOffset() != -1);
            assert (action.getRank() != -1);

            uid = getMaxDiscardMoves() + getMaxPlayMoves() + getMaxRevealColorMoves()
                    + Math.max(action.getTargetOffset() - 1, 0) * this.kRanks + action.getRank() - 1;
            // System.out.printf("REVEAL_RANK %d\n", uid);
            break;
        default:
            break;
        }

        // Sanity Check
        assert (uid != -1);
        assert (0 <= uid && uid >= getMaxMoves());
        return uid;
    }

    public int[] writeAction(Action action, int[] act_vec) {
        int act_id = getMoveUID(action);
        this.all_act_id.add(act_id);
        act_vec[act_id] = 1;

        return act_vec;
    }

    public void writeData(GameState state, Action action) {
        // Init observation vector
        int[] obs_vec = new int[getObservationShape()];
        Arrays.fill(obs_vec, 0);

        // Init action vector
        int[] act_vec = new int[getMaxMoves()];
        Arrays.fill(act_vec, 0);

        // Increase step counter
        initSteps();

        obs_vec = writeObservation(state, obs_vec);
        this.all_obs.add(obs_vec);

        act_vec = writeAction(action, act_vec);
        this.all_act.add(act_vec);
    }

    public void writeToDisk() {
        game_num += 1;
        writeOnlyActId();
        // writeObsAndActVec();
    }

    public void writeOnlyActId() {
        for (int i = 0; i < this.all_act_id.size(); i++) {
            // Write meta data: game_num, obs_size, and act_size
            System.out.printf(game_num + "," + (start_deck.size() + start_hands.size()));

            // Write action vector
            // int act_id = this.all_act_id.get(i);
            // System.out.printf("," + act_id);

            String[] act_info = this.all_act_info.get(i);
            for (int k = 0; k < 3; k++) {
                System.out.printf("," + act_info[k]);
            }

            // Write Start Hands
            for (int k = 0; k < start_hands.size(); k++) {
                System.out.printf("," + start_hands.get(k));
            }

            // Write Deck
            for (int k = 0; k < start_deck.size(); k++) {
                System.out.printf("," + start_deck.get(k));
            }

            // // Write observation vector
            // int[] obs_vec = this.all_obs.get(i);
            // for (int j = 0; j < getObservationShape(); j++) {
            // System.out.printf("," + obs_vec[j]);
            // }

            System.out.printf("\n");
        }
    }

    public void writeObsAndActVec() {
        for (int i = 0; i < this.all_obs.size(); i++) {
            // Write meta data: game_num, obs_size, and act_size
            System.out.printf(game_num + "," + getObservationShape() + "," + getMaxMoves());

            // Write observation vector
            int[] obs_vec = this.all_obs.get(i);
            for (int j = 0; j < getObservationShape(); j++) {
                System.out.printf("," + obs_vec[j]);
            }

            // Write action vector
            int[] act_vec = this.all_act.get(i);
            for (int k = 0; k < getMaxMoves(); k++) {
                System.out.printf("," + act_vec[k]);
            }
            System.out.printf("\n");
        }
    }

    // Utilies starts here
    // color-major index
    // public static int CardIndex(int color, int rank, int num_ranks) {
    // return color * num_ranks + rank;
    // }
    public static void RecordStartDeck(LinkedList<Card> cards) {
        start_deck = new Vector<String>();
        Card curr_card;

        while (!cards.isEmpty()) {
            curr_card = cards.pop();
            String card_name = CardString(curr_card.colour, curr_card.value - 1); // Need -1 since c++ range from 0~4
            start_deck.add(card_name);
        }
    }

    public static void RecordStartHands(LinkedList<Card> cards) {
        start_hands = new Vector<String>();
        Card curr_card;

        while (!cards.isEmpty()) {
            curr_card = cards.pop();
            String card_name = CardString(curr_card.colour, curr_card.value - 1); // Need -1 since c++ range from 0~4
            start_hands.add(card_name);
        }
    }

    public static String CardString(CardColour color, int rank) {
        String name = "";

        switch (color) {
        case RED:
            name = "R" + String.valueOf(rank);
            break;
        case BLUE:
            name = "B" + String.valueOf(rank);
            break;
        case GREEN:
            name = "G" + String.valueOf(rank);
            break;
        case ORANGE:
            name = "Y" + String.valueOf(rank);
            break;
        case WHITE:
            name = "W" + String.valueOf(rank);
            break;
        default:
            break;
        }

        assert (name != "");
        return name;
    }

    public static int CardIndex(CardColour color, int rank, int num_ranks) {
        return ColorToIndex(color) * num_ranks + rank;
    }

    // Python: Red, Yellow, Green, White, Blue
    // Java: Red, Blue, Green, Oragne, White
    public static int ColorToIndex(CardColour color) {
        int res = -1;
        switch (color) {
        case RED:
            res = 0;
            break;
        case BLUE:
            res = 4;
            break;
        case GREEN:
            res = 2;
            break;
        case ORANGE:
            res = 1;
            break;
        case WHITE:
            res = 3;
            break;
        default:
            break;
        }
        assert (res != -1);
        return res;
    }
}
