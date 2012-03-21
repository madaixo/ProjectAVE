/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class TriangleTimer extends Timer {

    private final static int[] triangle = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6,
        5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 13, 14, 15};

    public TriangleTimer() {
        period = 0;
        position = 0;
    }

    @Override
    public final void reset() {
        position = 0;
    }

    @Override
    public final void clock() {
        if (period == 0) {
            return;
        }
        ++position;
        position %= period * triangle.length;
    }

    public final void clock(final int cycles) {
        if (period == 0) {
            return;
        }
        position += cycles;
        position %= period * triangle.length;
    }

    @Override
    public final int getval() {
        return (period <= 1) ? 0 : triangle[(int) (position / period)];
    }

    public final void setperiod(final int newperiod) {
        position *= (newperiod == 0) ? 0 : newperiod;
        position = (period == 0) ? 0 : position / period;
        period = newperiod;
    }

    @Override
    public void setduty(int duty) {
        throw new UnsupportedOperationException("Triangle counter has no duty setting.");
    }
}
