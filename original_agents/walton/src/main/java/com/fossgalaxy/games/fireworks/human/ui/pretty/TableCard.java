package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;

import javax.swing.*;
import java.awt.*;

/**
 * Created by webpigeon on 21/04/17.
 */
public class TableCard extends JComponent {
    private GameState state;
    private CardColour cardColour;

    private transient Stroke outline = new BasicStroke(2);
    private transient Stroke outlineMissing = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{9}, 0);

    public TableCard(GameState state, CardColour colour) {
        this.setPreferredSize(new Dimension(90, 135));
        this.setMinimumSize(getPreferredSize());
        this.setMaximumSize(getPreferredSize());
        this.state = state;
        this.cardColour = colour;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Pretty mode
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(outline);

        Color javaColour = GameView.getColor(cardColour);
        String cardVal = Integer.toString(state.getTableValue(cardColour));

        if (cardVal.equals("0")) {
            g2.setStroke(outlineMissing);
        } else {
            g2.setStroke(outline);
        }

        g.setColor(javaColour);
        g.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);

        g.setColor(javaColour.darker().darker());
        g.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);

        //draw the numbers
        FontMetrics metrics = g.getFontMetrics();
        int w = metrics.stringWidth(cardVal) / 2;

        g.setColor(GameView.getTextColour(javaColour));
        g.drawString(cardVal, getWidth() / 2 - w, getHeight() / 2);
    }

}
