package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * Created by webpigeon on 21/04/17.
 */
public class DiscardComponent extends JComponent {
    private GameState state;

    public DiscardComponent(GameState state) {
        this.setPreferredSize(new Dimension(60*5, 90*5));
        this.state = state;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;

        // Pretty mode
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cardPos = 0;
        int cardRow = 0;
        int yOffset = 15;

        Collection<Card> discards = state.getDiscards();
        for (Card card : discards) {
            CardComponent.drawCard(
                    g2,
                    card,
                    cardPos*60,
                    (cardRow*90) + yOffset,
                    60,
                    90,
                    false
            );
            cardPos++;

            if (cardPos == 5) {
                cardPos = 0;
                cardRow++;
            }

        }
    }
}
