package com.fossgalaxy.games.fireworks.human.ui.pretty;

import javax.swing.*;
import java.awt.*;

/**
 * Created by webpigeon on 20/04/17.
 */
public class MockApp {

    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.setPreferredSize(new Dimension(800, 600));

        JComponent handView = Box.createHorizontalBox();

        for (int i=0; i<5; i++) {
            JComponent cardView = Box.createVerticalBox();

            Box box = Box.createHorizontalBox();
            box.add(buildIndicators());
            box.add(buildCard());
            box.add(buildIndicators());

            cardView.add(box);

            Box buttons = Box.createHorizontalBox();
            buttons.add(new JButton("Colour"));
            buttons.add(new JButton("Value"));
            cardView.add(buttons);

            handView.add(cardView);
        }

        frame.add(handView);

        frame.pack();
        frame.setVisible(true);
    }

    public static JComponent buildCard() {
        JPanel jPanel = new JPanel();
        jPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        return jPanel;
    }

    public static JComponent buildIndicators() {
        JComponent box = new JPanel(new GridLayout(5, 1));

        for (int i=0; i<5; i++) {
            box.add(new JLabel(""+ Integer.toString(i+1)));
        }

        return box;
    }
}
