package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Hand;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Created by webpigeon on 20/04/17.
 */
public class IndicatorPanel extends JComponent {
    private final Hand hand;
    private final int slot;
    private final boolean isColour;

    private List<Integer> possibleValues;
    private List<CardColour> possibleColours;

    public IndicatorPanel(Hand hand, int slot, boolean isColour) {
        this.setPreferredSize(new Dimension(15, 90));
        this.hand = hand;
        this.slot = slot;
        this.isColour = isColour;

        if (isColour) {
            setToolTipText("colours this player thinks this card could be");
        } else {
            setToolTipText("values this player thinks this card could be");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int height = getHeight()/5;

        if (isColour) {
            List<CardColour> myPossibleColours = possibleColours;
            if (myPossibleColours == null) {
                myPossibleColours = Arrays.asList(hand.getPossibleColours(slot));
            }

            CardColour[] legalColours = CardColour.values();
            for (int i = 0; i < legalColours.length; i++) {
                int y = i * height;

                g.setColor(Color.BLACK);
                g.fillRect(0, y, getWidth()-1, height);

                if (myPossibleColours.contains(legalColours[i])) {
                    g.setColor(GameView.getColor(legalColours[i]));
                    g.fillRect(0, y, getWidth() -1, height);
                }
                g.setColor(Color.BLACK);
                g.drawRect(0, y, getWidth()-1, height);
            }
        } else {
            boolean[] possibleB = new boolean[5];

            if (possibleValues == null) {
                //if no list override is set, use possible values from the hand
                int[] possible = hand.getPossibleValues(slot);
                for (int possVal : possible) {
                    possibleB[possVal - 1] = true;
                }
            } else {
                //if a list override is set, use that instead
                for (int i=1; i<=5; i++) {
                    possibleB[i - 1] = possibleValues.contains(i);
                }
            }

            for (int i = 0; i < possibleB.length; i++) {
                int y = i * height;

                g.setColor(Color.BLACK);
                g.fillRect(0, y, getWidth(), height);

                FontMetrics metrics = g.getFontMetrics();

                if (possibleB[i]) {
                    int h = metrics.getHeight();
                    int w = metrics.stringWidth(Integer.toString(i+1));

                    g.setColor(Color.WHITE);
                    g.drawString(Integer.toString(i+1), w, y+h);
                }

                g.setColor(Color.BLACK);
                g.drawRect(0, y, getWidth(), height);
            }
        }


    }

    public void setPossibleValues(List<Integer> possibleValues) {
        this.possibleValues = possibleValues;
        repaint();
    }

    public void setPossibleColours(List<CardColour> possibleColours) {
        this.possibleColours = possibleColours;
        repaint();
    }
}
