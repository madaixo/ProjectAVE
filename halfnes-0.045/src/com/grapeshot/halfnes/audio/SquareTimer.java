/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class SquareTimer extends Timer {

    protected int[] values;

    public final void clock() {
        if (period == 0) {
            return;
        }
        ++position;
        position %= period * values.length;
    }

    public final void clock(final int cycles) {
        if (period == 0) {
            return;
        }
        position += cycles;
        position %= period * values.length;
    }

    public SquareTimer(final int ctrlen) {
        values = new int[ctrlen];
        period = 0;
        position = 0;
        setduty(ctrlen / 2);
    }

    public final void reset() {
        position = 0;
    }

    public final void setduty(final int duty) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = (i < duty) ? 1 : 0;
        }
        if (position >= values.length * period) {
            position = (period > 0) ? (position % (period * values.length)) : 0;
        }
    }

    public final int getval() {
        return (period <= 1) ? 0 : values[(int) (position / period)];
    }

    public final void setperiod(final int newperiod) {
        position *= (newperiod == 0) ? 0 : newperiod;
        position = (period == 0) ? 0 : position / period;
        period = newperiod;
    }
}
