/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
public class NoiseTimer extends Timer {

    int valuespos = 0;
    private int[] values = genvalues(1, 1);
    private int prevduty = 1;

    public void NoiseTimer() {
        period = 0;
    }

    public void setduty(int duty) {
        if(duty != prevduty){
        values = genvalues(duty, values[valuespos]);
        valuespos = 0;
        }
        prevduty = duty;
    }

    public final void clock() {
        if (--position < 0) {
            position = period;
            ++valuespos;
            valuespos %= values.length;
        }
    }

    public final int getval() {
        return (values[valuespos] & 1);
    }

    @Override
    public final void reset() {
        position = 0;
    }

    @Override
    public final void clock(final int cycles) {
        for (int i = 0; i < cycles; ++i) {
            if (--position < 0) {
                position = period;
                position = period;
                ++valuespos;
                valuespos %= values.length;
            }
        }
    }

    @Override
    public final void setperiod(final int newperiod) {
        period = newperiod;
    }

    public static int[] genvalues(int whichbit, int seed) {
        int[] tehsuck = new int[(whichbit == 1) ? 32767 : 93];
        for (int i = 0; i < tehsuck.length; ++i) {
            seed = (seed >> 1)
                    | ((utils.getbit(seed, whichbit)
                    ^ utils.getbit(seed, 0))
                    ? 16384 : 0);
            tehsuck[i] = seed;
        }
        return tehsuck;

    }
}
