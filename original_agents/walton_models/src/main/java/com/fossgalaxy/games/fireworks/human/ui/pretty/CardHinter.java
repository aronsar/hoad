package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 21/04/17.
 */
public class CardHinter {
    private int playerID;
    private Hand hand;
    private CardComponent[] cardComponents;
    private IndicatorPanel[] colourIndicators;
    private IndicatorPanel[] valueIndicators;

    public CardHinter(Hand hand) {
        this.hand = hand;
        this.cardComponents = new CardComponent[hand.getSize()];
        this.colourIndicators = new IndicatorPanel[hand.getSize()];
        this.valueIndicators = new IndicatorPanel[hand.getSize()];
    }

    public void setCard(int slot, CardComponent component) {
        cardComponents[slot] = component;
    }

    public void hoverColour(int slot) {
        Card hoverCard = hand.getCard(slot);
        if (hoverCard == null) {
            return;
        }

        hoverColour2(hoverCard.colour);
    }

    public void hoverValue2(Integer value) {
        for (int i=0; i<hand.getSize(); i++) {
            Card card = hand.getCard(i);
            if (card != null && value.equals(card.value)) {
                cardComponents[i].setHover(true);
            } else {
                cardComponents[i].setHover(false);
            }
        }
    }

    public void hoverValue(int slot) {
        Card hoverCard = hand.getCard(slot);
        if (hoverCard == null) {
            return;
        }

        hoverValue2(hoverCard.value);
    }

    public void clearHover() {
        for (CardComponent comp : cardComponents) {
            comp.setHover(false);
        }
    }

    public void setColourIndicator(int i, IndicatorPanel colourIndicator) {
        colourIndicators[i] = colourIndicator;
    }

    public void setValueIndicator(int i, IndicatorPanel valueIndicator) {
        valueIndicators[i] = valueIndicator;
    }


    private static final int unused = -1;
    public void hintValuesFor(int slot, CardColour colour, List<Card> cards) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(unused, hand, cards);
        List<Card> cardsForSlot = possibleCards.get(slot);

        List<Integer> values = cardsForSlot.stream().filter(c -> colour.equals(c.colour) ).distinct().map(c -> c.value).collect(Collectors.toList());
        valueIndicators[slot].setPossibleValues(values);
    }

    public void resetHintValues(int slot) {
        valueIndicators[slot].setPossibleValues(null);
    }

    public void hintColoursFor(int slot, int ord, List<Card> cards) {
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(unused, hand, cards);
        List<Card> cardsForSlot = possibleCards.get(slot);

        List<CardColour> values = cardsForSlot.stream().filter(c -> c.value.equals(ord) ).distinct().map(c -> c.colour).collect(Collectors.toList());
        colourIndicators[slot].setPossibleColours(values);
    }


    public void resetHintColours(int slot) {
        colourIndicators[slot].setPossibleColours(null);
    }

    public void hoverColour2(CardColour colour) {
        for (int i=0; i<hand.getSize(); i++) {
            Card card = hand.getCard(i);
            if (card != null && colour.equals(card.colour)) {
                cardComponents[i].setHover(true);
            } else {
                cardComponents[i].setHover(false);
            }
        }
    }
}
