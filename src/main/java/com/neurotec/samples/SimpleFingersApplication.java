package com.neurotec.samples;

import com.neurotec.samples.panels.MainPanel;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class SimpleFingersApplication extends Applet {

    public void init() {
        final Component parent = this;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {

                    JFrame frame = new JFrame();
                    frame.setTitle("Muzima Fingerprint Identification");
                    MainPanel panel = new MainPanel();
                    frame.add(panel);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    //frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    add(frame);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
