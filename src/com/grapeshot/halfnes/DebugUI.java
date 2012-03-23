package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import javax.swing.*;

public class DebugUI extends JFrame {
    // StrokeInformer aStrokeInformer = new StrokeInformer();

    private ShowFrame fbuf;
    private int screenScaleFactor;
    private long[] frametimes = new long[60];
    private int frametimeptr = 0;
    private Repainter painter = new Repainter();

    public DebugUI() {
        screenScaleFactor = 2;
        fbuf = new ShowFrame();
        fbuf.setIgnoreRepaint(true);
    }

    public void run() {
        this.setTitle("HalfNES  Debug " + NES.VERSION);
        this.setResizable(false);

        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setContentPane(fbuf);
        this.pack();
        this.setVisible(true);
    }

    public void messageBox(String s) {
        JOptionPane.showMessageDialog(fbuf, s);
    }

    public void setFrame(BufferedImage b) {
        fbuf.nextFrame = b;

        java.awt.EventQueue.invokeLater(painter);
        //do the actual screen update on the event thread, basically all this does is blit the new frame
    }

    public class Repainter implements Runnable {

        public void run() {
            fbuf.repaint();
        }
    }

    public class ShowFrame extends javax.swing.JPanel {

        public BufferedImage nextFrame;
        /**
         *
         */
        private static final long serialVersionUID = 7221889725306697285L;

        public ShowFrame() {
            this.setBounds(0, 0, 256 * screenScaleFactor, 240 * screenScaleFactor);
            this.setPreferredSize(new Dimension(256 * screenScaleFactor, 240 * screenScaleFactor));
        }

        @Override
        public void paint(final Graphics g) {
            g.drawImage(nextFrame, 0, 0, 256 * screenScaleFactor, 240 * screenScaleFactor, null);
        }
    }
}
