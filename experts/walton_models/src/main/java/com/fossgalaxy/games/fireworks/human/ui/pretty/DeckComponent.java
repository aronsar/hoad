package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.GameState;

import javax.swing.*;
import java.awt.*;

/**
 * Created by webpigeon on 20/04/17.
 */
public class DeckComponent extends JComponent {
    private boolean hover;
    private GameState state;

    public DeckComponent(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(60, 90));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        int handSize = state.getHandSize();
        int cardsRemaining = Math.max(0, state.getDeck().getCardsLeft() - handSize);

        CardComponent.drawCard(g2, GameView.TANGO_DARK, Integer.toString(cardsRemaining), 0, 0, getWidth(), getHeight(), false);
    }

    public void setHover(boolean hover) {
        this.hover = hover;
        repaint();
    }

}
