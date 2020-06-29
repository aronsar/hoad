package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.GameState;

import java.awt.*;

/**
 * Created by webpigeon on 21/04/17.
 */
public class LifeComponent extends InfoComponent {
    protected final Color borderColour = GameView.TANGO_DARK.darker().darker();

    public LifeComponent(GameState state) {
        super(state);
    }

    @Override
    protected void paintComponent(Graphics g) {

        //super.paintComponent(g);

        int pad = 5;
        int radius = 120 / 4;
        int info = state.getLives();

        int tokenWidth = getWidth() / (radius + pad);
        int currCol = 0;
        int currRow = 0;

        int yOffset = 0;
        int xOffset = 0;

        Graphics2D g2 = (Graphics2D)g;
        g2.translate(10, 10);
        g2.setStroke(outline);

        // Pretty mode
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i=0; i<state.getStartingLives(); i++) {

            if (info > 0) {
                g2.setColor(GameView.TANGO_DARK);
                g2.fillOval(
                        currCol * radius + (pad * currCol) + xOffset,
                        currRow * radius + (pad * currRow) + yOffset,
                        radius,
                        radius
                );
            }

            g2.setColor(borderColour);
            g2.drawOval(
                    currCol * radius + (pad * currCol) + xOffset,
                    currRow * radius + (pad * currRow) + yOffset,
                    radius,
                    radius
            );
            info--;
            currCol++;

            if (currCol >= tokenWidth) {
                currCol = 0;
                currRow++;
            }

        }

    }
}
