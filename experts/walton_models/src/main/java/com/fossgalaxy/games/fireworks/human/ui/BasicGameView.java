package com.fossgalaxy.games.fireworks.human.ui;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piers on 11/04/17.
 */
public class BasicGameView extends GameView {

    public static final int MARGIN = 15;
    private transient final Object _state = new Object();
    private final Map<Color, Color> textColors = new HashMap<>();

    private GameState state;
    private int myPlayerID;

    public BasicGameView(int width, int height) {
        super();
        this.setPreferredSize(new Dimension(width, height));

        textColors.put(Color.WHITE, Color.BLACK);
        textColors.put(Color.BLACK, Color.WHITE);
        textColors.put(Color.YELLOW, Color.BLACK);
        textColors.put(Color.RED, Color.BLACK);
        textColors.put(Color.BLUE, Color.WHITE);
        textColors.put(Color.GREEN, Color.BLACK);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        //Mat Areas
        if (state != null) {
            final int cellWidth = cardWidth() + MARGIN;
            final int cellHeight = cellWidth * 3/2 + MARGIN;

            graphics.setColor(Color.CYAN);
            graphics.fillRect(0, 0, cellWidth*5, cellHeight*5);

            graphics.setColor(Color.CYAN);
            graphics.fillRect(0, cellHeight*6, cellWidth*12, cellHeight*3 );
        }

        if (state != null) {
            synchronized (_state) {

                for (int playerID=0; playerID<state.getPlayerCount(); playerID++) {
                    Hand hand = state.getHand(playerID);

                    for (int slot = 0; slot < hand.getSize(); slot++) {
                        Color color;
                        String number;

                        if (playerID == myPlayerID) {
                            Integer thoughtVal = hand.getKnownValue(slot);
                            color = getColor(hand.getKnownColour(slot));
                            number = thoughtVal == null ? "?" : thoughtVal.toString();
                        } else {
                            Card card = hand.getCard(slot);
                            if (card != null) {
                                color = getColor(card.colour);
                                number = card.value.toString();
                            } else {
                                color = Color.GRAY;
                                number = "";
                            }
                        }

                        drawCard(graphics, playerID, slot, number, color);
                    }
                }


                for (CardColour colour : CardColour.values()) {
                    drawCard(graphics, state.getPlayerCount() + 1, colour.ordinal(), Integer.toString(state.getTableValue(colour)), getColor(colour));
                }

                if (myPlayerID != -1) {
                    drawCard(graphics, state.getPlayerCount() + 1, 8, Integer.toString(Math.max(0, state.getDeck().getCardsLeft() - state.getHand(myPlayerID).getSize())), Color.BLACK);
                    drawCard(graphics, state.getPlayerCount() + 1, 9, Integer.toString(state.getDiscards().size()), Color.WHITE);
                }

                for (int i=0; i<state.getInfomation(); i++) {
                    drawInfo(graphics, state.getPlayerCount()+2, i, Color.BLUE, Color.BLACK);
                }

                for (int i=state.getInfomation(); i<state.getStartingInfomation(); i++) {
                    drawInfo(graphics, state.getPlayerCount()+2, i, null, Color.BLACK);
                }

                for (int i=0; i<state.getLives(); i++) {
                    drawInfo(graphics, state.getPlayerCount()+3, i, Color.RED, Color.BLACK);
                }

                final int cardsPerRow = 5;
                int i=0;
                Collection<Card> discards = state.getDiscards();
                for (Card card : discards) {
                    drawCard(graphics, i/cardsPerRow, 8 + (i % cardsPerRow), Integer.toString(card.value), getColor(card.colour));
                    i++;
                }

            }
        }

    }


    private void drawCard(Graphics graphics, int playerID, int slot, String number, Color color) {
        final int cardWidth = cardWidth();
        final int cardHeight = cardWidth * 3/2;

        graphics.setColor(color);

        // Draw rectangle
        int x = (slot * (cardWidth + MARGIN)) + MARGIN;
        int y = (playerID * cardHeight) + MARGIN * playerID;
        graphics.fillRect(x, y, cardWidth, cardHeight);

        graphics.setColor(textColors.getOrDefault(color, Color.BLACK));
        graphics.drawString(number, x + cardWidth/2, y + cardHeight/2);
    }

    private void drawInfo(Graphics graphics, int row, int column, Color color, Color outline) {
        final int cardWidth = cardWidth();
        final int cardHeight = cardWidth * 3/2;

        // Draw rectangle
        int x = (column * (cardWidth + MARGIN)) + MARGIN;
        int y = (row * cardHeight) + MARGIN * row;


        if (color != null) {
            graphics.setColor(color);
            graphics.fillOval(x, y, cardWidth, cardWidth);
        }

        graphics.setColor(outline);
        graphics.drawOval(x, y, cardWidth, cardWidth);

    }

    private int cardWidth() {
        int cards = getNumCards();
        int margin = cards * MARGIN;
        return (getWidth() - margin) / cards;
    }

    private int getNumCards() {
        return state.getPlayerCount() * state.getHandSize();
    }

    @Override
    public void setState(GameState state, int id) {
        synchronized (_state) {
            System.out.println("State being set");
            this.state = state;
            this.myPlayerID = id;
        }
        this.repaint();
    }
}
