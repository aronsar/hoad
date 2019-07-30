package com.fossgalaxy.games.fireworks.utils;

import java.util.LinkedList;
import java.util.Vector;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Created by webpigeon on 11/10/16.
 */
public class DataParserUtils {
    /**
     * The number of players that were in this game
     */
    public Vector<String[]> all_act_info;
    public static Vector<String> start_deck;
    public static Vector<String> start_hands;
    public static int game_num = -1;

    public DataParserUtils() {
        this.all_act_info = new Vector<String[]>();
    }

    public void writeAction(Action action) {
        // act_info: [action_type, played_card_color, played_card_rank]
        String[] act_info = new String[3];
        act_info[0] = String.format("%s", action.getType());
        act_info[1] = action.getColorName();
        act_info[2] = String.format("%s", action.getRank());

        this.all_act_info.add(act_info);
    }

    public void writeData(GameState state, Action action) {
        writeAction(action);
    }

    public void writeToDisk() {
        game_num += 1;
        writeOnlyActAndDeck();
    }

    public void writeOnlyActAndDeck() {
        for (int i = 0; i < this.all_act_info.size(); i++) {
            // Write meta data: game_num, deck_size
            System.out.printf(game_num + "," + (start_deck.size() + start_hands.size()));

            // Write action: action_type, played_card_color, played_card_rank
            // In the future when playing with player > 2, might need to include
            // target_offset
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

            System.out.printf("\n");
        }
    }

    // Recording the starting deck cards when the game is initialized
    public static void RecordStartDeck(LinkedList<Card> cards) {
        start_deck = new Vector<String>();
        Card curr_card;

        while (!cards.isEmpty()) {
            curr_card = cards.pop();
            String card_name = CardString(curr_card.colour, curr_card.value - 1); // Need -1 since c++ range from 0~4
            start_deck.add(card_name);
        }
    }

    // Recording the starting hand cards of each player when the game is initialized
    public static void RecordStartHands(LinkedList<Card> cards) {
        start_hands = new Vector<String>();
        Card curr_card;

        while (!cards.isEmpty()) {
            curr_card = cards.pop();
            String card_name = CardString(curr_card.colour, curr_card.value - 1); // Need -1 since c++ range from 0~4
            start_hands.add(card_name);
        }
    }

    // Generate CardString to store in csv
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
}
