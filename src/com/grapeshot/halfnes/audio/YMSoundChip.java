/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.DebugUI;
import java.awt.image.BufferedImage;
import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
public class YMSoundChip implements ExpansionSoundChip {

    //note: this is the cutdown version from the vrc7. Only 6 channels, no percussion.
    //Exponential table:
    //x = 0..255, y = round((power(2, x/256)-1)*1024)
    //Log-sin table:
    //x = 0..255, y = round(-log(sin((x+0.5)*pi/256/2))/log(2)*256)
    //Envelopes, LFO and attenuators aren't in yet.
    private final int[] vol = new int[6], freq = new int[6],
            octave = new int[6], instrument = new int[6], remainder = new int[6];
    private final boolean[] key = new boolean[6];
    private int counter = 0; //free running counter for indices
    private double[] out = new double[6], wave = {0, 0, 0, 0, 0, 0};
    private final int[][] instdata = { //instrument parameters
        //this table is from kevtris's ancient documentation.
        {00, 00, 00, 00, 00, 00, 00, 00},
        {0x05, 0x03, 0x10, 0x06, 0x74, 0xA1, 0x13, 0xF4},
        {0x05, 0x01, 0x16, 0x00, 0xF9, 0xA2, 0x15, 0xF5},
        {0x01, 0x41, 0x11, 0x00, 0xA0, 0xA0, 0x83, 0x95},
        {0x01, 0x41, 0x17, 0x00, 0x60, 0xF0, 0x83, 0x95},
        {0x24, 0x41, 0x1F, 0x00, 0x50, 0xB0, 0x94, 0x94},
        {0x05, 0x01, 0x0B, 0x04, 0x65, 0xA0, 0x54, 0x95},
        {0x11, 0x41, 0x0E, 0x04, 0x70, 0xC7, 0x13, 0x10},
        {0x02, 0x44, 0x16, 0x06, 0xE0, 0xE0, 0x31, 0x35},
        {0x48, 0x22, 0x22, 0x07, 0x50, 0xA1, 0xA5, 0xF4},
        {0x05, 0xA1, 0x18, 0x00, 0xA2, 0xA2, 0xF5, 0xF5},
        {0x07, 0x81, 0x2B, 0x05, 0xA5, 0xA5, 0x03, 0x03},
        {0x01, 0x41, 0x08, 0x08, 0xA0, 0xA0, 0x83, 0x95},
        {0x21, 0x61, 0x12, 0x00, 0x93, 0x92, 0x74, 0x75},
        {0x21, 0x62, 0x21, 0x00, 0x84, 0x85, 0x34, 0x15},
        {0x21, 0x62, 0x0E, 0x00, 0xA1, 0xA0, 0x34, 0x15}};
    private final static double[] freqtbl = genfreqtbl();
    private final static int[] logsin = genlogsintbl(), exp = genexptbl(), am = genamtbl();
    private final static double[] multbl = {2, 1, 1 / 2, 1 / 3, 1 / 4, 1 / 5,
        1 / 6, 1 / 7, 1 / 8, 1 / 9, 1 / 10, 1 / 10, 1 / 12, 1 / 15, 1 / 15},
            fbtbl = {0, Math.PI / 16, Math.PI / 8, Math.PI / 4, Math.PI / 2, Math.PI, 2 * Math.PI, 4 * Math.PI},
            vib = genvibtbl();

    public YMSoundChip() {
        //some debug code to make a scope view:
//        DebugUI d = new DebugUI();
//        BufferedImage b = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
//        d.run();
//
//        for (int i = 0; i < 1024; ++i) {
//            int j = logsin(i);
//            int k = (int) exp(logsin(i));
//            b.setRGB(i / 4, clamp(-j + 128), 0xFF0000);
//            b.setRGB(i / 4, clamp(-k + 128), 0x00FFFF);
//            b.setRGB(i / 4, clamp(-i + 128), 0x00FF00);
//            b.setRGB(i / 4, clamp(128), 0xffffff);
//        }
//        d.setFrame(b);
    }

    public static int clamp(final int a) {
        return (a != (a & 0xff)) ? ((a < 0) ? 0 : 255) : a;
    }

    private static double[] genvibtbl() {
        //3.6 mhz / 6 = 600 khz sample rate
        double[] tbl = new double[93750];
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = 10 * Math.sin(2 * Math.PI * 6.7 / 600000.);
        }
        return tbl;
    }

    private static int[] genamtbl() {
        //3.6 mhz / 6 = 600 khz sample rate
        int[] tbl = new int[93750];
        for (int x = 0; x < tbl.length; ++x) {
            tbl[x] = (int) (20 * Math.sin(2 * Math.PI * 3.3 / 600000.) + 20);
        }
        return tbl;
    }

    private static int[] genlogsintbl() {
        int[] tbl = new int[256];
        for (int i = 0; i < tbl.length; ++i) {
            //y = round(-log(sin((x+0.5)*pi/256/2))/log(2)*256)
            tbl[i] = (int) Math.round(-Math.log(Math.sin((i + 0.5) * Math.PI / 256 / 2)) / Math.log(2) * 256);
        }
        return tbl;
    }

    private static int[] genexptbl() {
        int[] tbl = new int[256];
        for (int i = 0; i < tbl.length; ++i) {
            //y = round((power(2, x/256)-1)*1024)
            tbl[i] = (int) Math.round((Math.pow(2, i / 256.) - 1) * 1024.);
        }
        return tbl;
    }

    private static double[] genfreqtbl() {
        double[] tbl = new double[4096];
        for (int i = 0; i < tbl.length; ++i) {
            tbl[i] = 49722 * (i & 512) / Math.pow(2, (19 - (i >> 9))) * 2.793296e-7 * 2 * Math.PI;
        }
        return tbl;
    }

    @Override
    public final void write(int register, int data) {
        switch (register) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                //parameters for instrument 0
                instdata[0][register & 7] = data;
                break;
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15: //frequency registers for ch. 0-5
                int n = register - 0x10;
                freq[n] = (freq[n] & 0xf00) | data;
                break;
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x25: // ???tooof
//f: Upper bit of frequency
//o: Octave Select 
//t: Channel keying on/off (key on = note starts, key off: note decays).
//?: bit 5 is sustain which doesn't seem to work on this chip, 6 and 7 unused
                int m = register - 0x20;
                freq[m] = (freq[m] & 0xff) | ((data & 1) << 8);
                octave[m] = (data & 0xf) >> 1;
                key[m] = utils.getbit(data, 4);
                break;
            case 0x30:
            case 0x31:
            case 0x32:
            case 0x33:
            case 0x34:
            case 0x35: //top 4 bits instrument number, bottom 4 volume
                int j = register - 0x30;
                instrument[j] = (data >> 4) & 0xf;
                vol[j] = data & 0xf;
                break;
            default:
                System.err.println(utils.hex(register) + " doesn't exist " + utils.hex(data));
        }
    }
    int ch = 0;

    @Override
    public final void clock(final int cycle) {
        //chip runs at 3.58 mhz, so clock 2 cycles per call
        //BUT each channel is only updated once every 72 cycles on the real chip?

        for (int i = 0; i < cycle; ++i) {
            ++ch;
            ch %= 6;
            {

                if (key[ch]) {
                    //System.out.println(ch);
                    wave[ch] += (16. / 49722.) * (freq[ch] << (octave[ch]));
                    //Tuned this with audacity so it's definitely ok this time.
                    int[] inst = instdata[instrument[ch]];
                    out[ch] = operator(
                            operator(wave[ch] * multbl[inst[0] & 0xf] //modulator base freq and multiplier
                            + (utils.getbit(inst[0], 6) ? vib[counter] : 0),//modulator vibrato
                            -(inst[2] & 0x1f) * (octave[ch] * (inst[2] & 0xe0) + 1) //modulator volume
                            - (utils.getbit(inst[0], 7) ? am[counter] : 0),
                            utils.getbit(inst[3], 3))//modulator rectify
                            + (utils.getbit(inst[1], 6) ? vib[counter] : 0)//carrier vibrato
                            + wave[ch] * multbl[inst[1] & 0xf]//carrier freq multiplier
                            + ((-out[ch]) * (fbtbl[inst[3] & 7])),//carrier feedback
                            vol[ch] - (utils.getbit(inst[1], 7) ? am[counter] : 0),//carrier volume
                            utils.getbit(inst[3], 4));//carrier rectify
                }
                ++counter;
                counter %= 93750;
            }
        }
        // out = exp(logsin(phase2 + exp(logsin(phase1) + gain1)) + gain2)
    }

    private double operator(double phase, int gain, boolean rectify) {
        return exp(logsin((int) phase, rectify) + gain);
    }

    private double exp(final int val) {
        double mantissa = exp[(val & 0xff)];
        int exponent = val >> 8;
        return Math.scalb(mantissa + 1024, exponent) * s;
    }
    private int s; // ugly hackish sign flag

    private int logsin(final int x, boolean rectify) {
        //note: there's no way to properly represent output sign here...
        //actual hardware needs to be able to add to this while it's in log domain
        //to change the volume, and two's complement screws that up.
        //logs of negative numbers should be in complex domain anyway.
        //i'm going to do the wrong thing and use a class variable. instead of passing
        //around 3.6 million tuples per second.
        //can get away with this since operators are always applied in series.
        switch ((x >> 8) & 3) {
            case 0:
                s = -1;
                return -logsin[(x & 0xff)];
            case 1:
                s = -1;
                return -logsin[255 - (x & 0xff)];
            case 2:
                s = rectify ? 0 : 1;
                return -logsin[(x & 0xff)];
            case 3:
            default:
                s = rectify ? 0 : 1;
                return rectify ? -logsin[255 - (x & 0xff)] : 0;
        }
    }

    @Override
    public final int getval() {
        double ret = 0;
        for (int i = 0; i < 6; ++i) {
            ret += out[i];
        }
        return clampS(ret);
    }

    public static int clampS(final double a) {
        return (int) ((a < -65536) ? -65536 : ((a > 65535) ? 65535 : a));
    }
}
