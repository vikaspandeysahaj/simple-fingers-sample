package com.neurotec.samples;

import com.neurotec.samples.panels.MainPanel;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class SimpleFingersApplication extends Applet {

//    public static void main(String[] args) {
//
//
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                JFrame frame = new JFrame();
//                frame.setTitle("Simple Fingers Sample");
//
//                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//                frame.add(new MainPanel(), BorderLayout.CENTER);
//                frame.pack();
//                frame.setLocationRelativeTo(null);
//                frame.setVisible(true);
//            }
//        });
//    }

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
//            StackTraceElement[] stackTrace = e.getStackTrace();
//            String stackTraceString = "";
//            for (StackTraceElement element : stackTrace) {
//                stackTraceString += element.toString();
//            }
//            JOptionPane.showMessageDialog(this, String.format("error: %s", stackTraceString), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
//            JOptionPane.showMessageDialog(this, "error" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
