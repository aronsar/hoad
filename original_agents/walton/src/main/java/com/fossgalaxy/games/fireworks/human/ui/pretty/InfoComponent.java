package com.fossgalaxy.games.fireworks.human.ui.pretty;

import com.fossgalaxy.games.fireworks.human.ui.GameView;
import com.fossgalaxy.games.fireworks.state.GameState;

import javax.swing.*;
import java.awt.*;

/**
 * Created by webpigeon on 21/04/17.
 */
public class InfoComponent extends JComponent {
    protected final GameState state;
    protected final Stroke outline = new BasicStroke(2);
    protected final Color outlineColor = GameView.TANGO_BLUE.darker().darker();

    public InfoComponent(GameState state) {
        this.setPreferredSize(new Dimension(35*8, 45));
        this.setBackground(Color.RED);
        this.state = state;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int pad = 5;
        int radius = 30;
        int info = state.getInfomation();

        int tokenWidth = getWidth() / (radius + pad*2);
        int currCol = 0;
        int currRow = 0;
        int yOffset = 0;
        int xOffset = 5;


        Graphics2D g2 = (Graphics2D)g;
        g2.translate(10, 10);
        g2.setStroke(outline);

        // Pretty mode
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i=0; i<state.getStartingInfomation(); i++) {

                if (info > 0) {
                    g2.setColor(GameView.TANGO_BLUE);
                    g2.fillOval(
                            currCol * radius + (pad * currCol) + xOffset,
                            currRow * radius + (pad*2 * currRow) + yOffset,
                            radius,
                            radius
                    );
                }

                g2.setColor(outlineColor);
                g2.drawOval(
                        currCol * radius + (pad * currCol) + xOffset,
                        currRow * radius + (pad*2 * currRow) + yOffset,
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
