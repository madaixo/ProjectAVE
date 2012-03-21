package com.grapeshot.halfnes;

import com.grapeshot.halfnes.audio.*;

//HalfNES, Copyright Andrew Hoffman, October 2010
public class APU {

    public int samplerate = 1;
    private final Timer[] timers = {new SquareTimer(8), new SquareTimer(8),
        new TriangleTimer(), new NoiseTimer()};
    private final double cyclespersample;
    public final NES nes;
    CPU cpu;
    CPURAM cpuram;
    private int apucycle = 0, remainder = 0;
    private static final int[] noiseperiod = {4, 8, 16, 32, 64, 96, 128,
        160, 202, 254, 380, 508, 762, 1016, 2034, 4068};
    // different for PAL
    private long accum = 0;
    private ExpansionSoundChip expnSound;
    private boolean soundFiltering;
    private final int[] tnd_lookup, square_lookup;
    private AudioOutInterface ai;

    public APU(final NES nes, final CPU cpu, final CPURAM cpuram) {
        //fill square, triangle volume lookup tables
        square_lookup = new int[31];
        for (int i = 0; i < square_lookup.length; ++i) {
            square_lookup[i] = (int) ((95.52 / (8128.0 / i + 100)) * 49151);
        }
        tnd_lookup = new int[203];
        for (int i = 0; i < tnd_lookup.length; ++i) {
            tnd_lookup[i] = (int) ((163.67 / (24329.0 / i + 100)) * 49151);
        }
        //then init the audio stream
        this.nes = nes;
        this.cpu = cpu;
        this.cpuram = cpuram;
        soundFiltering = nes.getPrefs().getBoolean("soundFiltering", false);
        samplerate = nes.getPrefs().getInt("sampleRate", 44100);
        cyclespersample = 1789773.0 / samplerate;
        ai = new SwingAudioImpl(nes, samplerate);
    }

    public boolean bufferHasLessThan(int samples) {
        return ai.bufferHasLessThan(samples);
    }

    public final int read(final int addr) {
        updateto((int) cpu.cycles);
        switch (addr) {
            case 0x15:
                //returns channel status
                //for future ref: NEED to put those ternary operators in parentheses!
                //otherwise order of operations does the wrong thing.
                final int returnval = ((lengthctr[0] > 0) ? 1 : 0)
                        + ((lengthctr[1] > 0) ? 2 : 0)
                        + ((lengthctr[2] > 0) ? 4 : 0)
                        + ((lengthctr[3] > 0) ? 8 : 0)
                        + ((dmcsamplesleft > 0) ? 16 : 0)
                        + (statusframeint ? 64 : 0)
                        + (statusdmcint ? 128 : 0);
                if (statusframeint) {
                    //System.err.println("Frame interrupt ack at " + cpu.cycles);
                    --cpu.interrupt;
                }
                statusframeint = false;
                //System.err.println("*" + utils.hex(returnval));
                return returnval;
            case 0x16:
                nes.getcontroller1().strobe();
                return nes.getcontroller1().getbyte() | 0x40;
            case 0x17:
                nes.getcontroller2().strobe();
                return nes.getcontroller2().getbyte() | 0x40;
            default:
                return 0x40; //open bus
        }
    }
    final private static int[] dutylookup = {1, 2, 4, 6};

    public void addExpnSound(ExpansionSoundChip chip) {
        expnSound = chip;
    }

    public void destroy() {
        ai.destroy();
    }

    public void pause() {
        ai.pause();
    }

    public void resume() {
        ai.resume();
    }

    public final void write(final int reg, final int data) {
        //This is how values written to any of the APU's memory
        //mapped registers change the state of the system.
        updateto((int) cpu.cycles - 1);
        //System.err.println("Wrote " + utils.hex(data) + " to " + utils.hex(reg) + " @ cycle " + cpu.cycles);
        switch (reg) {
            case 0x0:
                //length counter 1 halt
                lenctrHalt[0] = utils.getbit(data, 5);
                // pulse 1 duty cycle
                timers[0].setduty(dutylookup[data >> 6]);
                // and envelope
                envConstVolume[0] = utils.getbit(data, 4);
                envelopeValue[0] = data & 15;
                //setvolumes();
                break;
            case 0x1:
                //pulse 1 sweep setup
                //sweep enabled
                sweepenable[0] = utils.getbit(data, 7);
                //sweep divider period
                sweepperiod[0] = (data >> 4) & 7;
                //sweep negate flag
                sweepnegate[0] = utils.getbit(data, 3);
                //sweep shift count
                sweepshift[0] = (data & 7);
                sweepreload[0] = true;
                break;
            case 0x2:
                // pulse 1 timer low bit
                timers[0].setperiod((timers[0].getperiod() & 0xfe00) + (data << 1));
                break;
            case 0x3:
                // length counter load, timer 1 high bits
                if (lenCtrEnable[0]) {
                    lengthctr[0] = lenctrload[data >> 3];
                }
                timers[0].setperiod((timers[0].getperiod() & 0x1ff) + ((data & 7) << 9));
                // sequencer restarted
                timers[0].reset();
                //envelope also restarted
                envelopeStartFlag[0] = true;
                break;
            case 0x4:
                //length counter 2 halt
                lenctrHalt[1] = utils.getbit(data, 5);
                // pulse 2 duty cycle
                timers[1].setduty(dutylookup[data >> 6]);
                // and envelope
                envConstVolume[1] = utils.getbit(data, 4);
                envelopeValue[1] = data & 15;
                //setvolumes();
                break;
            case 0x5:
                //pulse 2 sweep setup
                //sweep enabled
                sweepenable[1] = utils.getbit(data, 7);
                //sweep divider period
                sweepperiod[1] = (data >> 4) & 7;
                //sweep negate flag
                sweepnegate[1] = utils.getbit(data, 3);
                //sweep shift count
                sweepshift[1] = (data & 7);
                sweepreload[1] = true;
                break;
            case 0x6:
                // pulse 2 timer low bit
                timers[1].setperiod((timers[1].getperiod() & 0xfe00) + (data << 1));
                break;
            case 0x7:
                if (lenCtrEnable[1]) {
                    lengthctr[1] = lenctrload[data >> 3];
                }
                timers[1].setperiod((timers[1].getperiod() & 0x1ff) + ((data & 7) << 9));
                // sequencer restarted
                timers[1].reset();
                //envelope also restarted
                envelopeStartFlag[1] = true;
                break;
            case 0x8:
                //triangle linear counter load
                linctrreload = data & 0x7f;
                //and length counter halt
                lenctrHalt[2] = utils.getbit(data, 7);
                break;
            case 0x9:
                break;
            case 0xA:
                // triangle low bits of timer
                timers[2].setperiod((((timers[2].getperiod() * 1) & 0xff00) + data) / 1);
                break;
            case 0xB:
                // triangle length counter load
                // and high bits of timer
                if (lenCtrEnable[2]) {
                    lengthctr[2] = lenctrload[data >> 3];
                }
                timers[2].setperiod((((timers[2].getperiod() * 1) & 0xff) + ((data & 7) << 8)) / 1);
                linctrflag = true;
                break;
            case 0xC:
                //noise halt and envelope
                lenctrHalt[3] = utils.getbit(data, 5);
                envConstVolume[3] = utils.getbit(data, 4);
                envelopeValue[3] = data & 0xf;
                //setvolumes();
                break;
            case 0xD:
                break;
            case 0xE:
                timers[3].setduty(utils.getbit(data, 7) ? 6 : 1);
                timers[3].setperiod(noiseperiod[data & 15]);
                break;
            case 0xF:
                //noise length counter load, envelope restart
                if (lenCtrEnable[3]) {
                    lengthctr[3] = lenctrload[data >> 3];
                }
                envelopeStartFlag[3] = true;
                break;
            case 0x10:
                dmcirq = utils.getbit(data, 7);
                dmcloop = utils.getbit(data, 6);
                dmcrate = dmcperiods[data & 0xf];
                break;
            case 0x11:
                dmcvalue = data;
                if (dmcvalue > 0x7f) {
                    dmcvalue = 0x7f;
                }
                if (dmcvalue < 0) {
                    dmcvalue = 0;
                }
                break;
            case 0x12:
                dmcstartaddr = (data << 6) + 0xc000;
                break;
            case 0x13:
                dmcsamplelength = (data << 4) + 1;
                break;
            case 0x14:
                //sprite dma
                cpuram.write(0x2003, 0);
                //^shouldn't actually do that... but otherwise gauntlet breaks
                //No Idea what nestopia is doing.
                for (int i = 0; i < 256; ++i) {
                    cpuram.write(0x2004, cpuram.read((data << 8) + i));
                }
                cpu.cycles += 513;
                break;
            case 0x15:
                //status register
                // counter enable(silence channel when bit is off)
                for (int i = 0; i < 4; ++i) {
                    lenCtrEnable[i] = utils.getbit(data, i);
                    //THIS was the channels not cutting off bug! If you toggle a channel's
                    //status on and off very quickly then the length counter should
                    //IMMEDIATELY be forced to zero.
                    if (!lenCtrEnable[i]) {
                        lengthctr[i] = 0;
                    }
                }
                if (utils.getbit(data, 4)) {
                    if (dmcsamplesleft == 0) {
                        restartdmc();
                    }
                } else {
                    dmcsamplesleft = 0;
                    dmcsilence = true;
                }
                if (statusdmcint) {
                    --cpu.interrupt;
                }
                statusdmcint = false;
                break;
            case 0x16:
                // latch controller 1 + 2
                nes.getcontroller1().output(utils.getbit(data, 0));
                nes.getcontroller2().output(utils.getbit(data, 0));
                break;
            case 0x17:
                ctrmode = utils.getbit(data, 7) ? 5 : 4;
                apuintflag = utils.getbit(data, 6);
                framectr = 0;
                if (utils.getbit(data, 7)) {
                    clockframecounter();
                }
                framectroffset = cpu.cycles % 7445;
                if ((ctrmode == 5 || apuintflag) && statusframeint) {
                    statusframeint = false;
                    --cpu.interrupt;
                    //System.err.println("Frame interrupt off at " + cpu.cycles);
                }
                break;
        }
    }
    private int framectroffset = 3000;

    public final void updateto(final int cpucycle) {
        //still have to run this even if sound is disabled, some games rely on DMC IRQ etc.
        if (soundFiltering) {
            //linear sampling code
            //should really be a FIR filter + decimator instead
            //but I don't have the DSP experience to design something like that
            //that would be fast enough to work / not require calculating every sample
            //this works well enough at eliminating aliasing anyway.
            while (apucycle < cpucycle) {
                ++remainder;
                clockdmc();
                if (apucycle % 7445 == framectroffset) {
                    clockframecounter();
                }
                timers[0].clock();
                timers[1].clock();
                if (lengthctr[2] > 0 && linearctr > 0) {
                    timers[2].clock();
                }
                timers[3].clock();
                if (expnSound != null) {
                    expnSound.clock(1);
                }
                accum += getOutputLevel();

                if ((apucycle % cyclespersample) < 1) {
                    //not quite right - there's a non-integer # cycles per sample.
                    ai.outputSample((short) (accum / remainder));
                    remainder = 0;
                    accum = 0;
                }
                ++apucycle;
            }
        } else {
            //point sampling code
            while (apucycle < cpucycle) {
                ++remainder;
                clockdmc();
                if (apucycle % 7445 == framectroffset) {
                    clockframecounter();
                }
                if ((apucycle % cyclespersample) < 1) {
                    //not quite right - there's a non-integer # cycles per sample.
                    timers[0].clock(remainder);
                    timers[1].clock(remainder);
                    if (lengthctr[2] > 0 && linearctr > 0) {
                        timers[2].clock(remainder);
                    }
                    timers[3].clock(remainder);
                    int mixvol = getOutputLevel();
                    if (expnSound != null) {
                        expnSound.clock(remainder);
                    }
                    remainder = 0;
                    ai.outputSample(mixvol);
                }
                ++apucycle;
            }
        }
    }

    private int getOutputLevel() {
        int vol = square_lookup[volume[0] * timers[0].getval()
                + volume[1] * timers[1].getval()];
        vol += tnd_lookup[
                    3 * timers[2].getval()
                + 2 * volume[3] * timers[3].getval()
                + dmcvalue];
        if (expnSound != null) {
            vol *= 0.8;
            vol += expnSound.getval();
        }
        vol -= 32767;
        return vol; //as usual, lack of unsigned types causes unending pain.
    }

    public final void finishframe() {
        updateto(29781);
        apucycle = 0;
        ai.flushFrame(nes.isFrameLimiterOn());
    }
    private boolean apuintflag = true, statusdmcint = false, statusframeint = false;
    private int framectr = 0, ctrmode = 4;

    private void clockframecounter() {
        //System.err.println("frame ctr clock " + framectr);
        //should be ~4x a frame, 240 Hz
        //but the problem is this isn't exactly related to the video signal,
        //it's a completely separate timer, so the phase can shift in relation to the
        //video signal. also in the current implementation APU interrupts can only be fired when
        //an APU register is written/read from, or @ end of frame. So both of those need work
        if (framectr < 4) {
            setenvelope();
            setlinctr();
        }
        if ((ctrmode == 4 && (framectr == 1 || framectr == 3))
                || (ctrmode == 5 && (framectr == 0 || framectr == 2))) {
            setlength();
            setsweep();
        }
        if (!apuintflag && (framectr == 3) && (ctrmode == 4)) {
            if (!statusframeint) {
                ++cpu.interrupt;
                //System.err.println("frame interrupt set at " + cpu.cycles);
                statusframeint = true;
            }

        }
        ++framectr;
        framectr %= ctrmode;
        setvolumes();
    }
    private boolean[] lenCtrEnable = {true, true, true, true};
    private int[] volume = {0, 0, 0, 0};

    private void setvolumes() {
        volume[0] = ((lengthctr[0] <= 0 || sweepsilence[0]) ? 0 : (((envConstVolume[0]) ? envelopeValue[0] : envelopeCounter[0])));
        volume[1] = ((lengthctr[1] <= 0 || sweepsilence[1]) ? 0 : (((envConstVolume[1]) ? envelopeValue[1] : envelopeCounter[1])));
        volume[3] = ((lengthctr[3] <= 0) ? 0 : ((envConstVolume[3]) ? envelopeValue[3] : envelopeCounter[3]));
        //System.err.println("setvolumes " + volume[1]);
    }
    //instance variables for dmc unit
    private final static int[] dmcperiods = {428, 380, 340, 320, 286, 254,
        226, 214, 190, 160, 142, 128, 106, 84, 72, 54};
    private int dmcrate = 0x36,dmcpos = 0,dmcshiftregister = 0,dmcbuffer = 0,
            dmcvalue = 0,dmcsamplelength = 0,dmcsamplesleft = 0,
            dmcstartaddr = 0xc000, dmcaddr = 0xc000, dmcbitsleft = 8;
    private boolean dmcsilence = true, dmcirq = false, dmcloop = false;

    private void clockdmc() {
        ++dmcpos;
        if (dmcpos > dmcrate) {
            dmcpos = 0;
            if (!dmcsilence) {
                dmcvalue += (utils.getbit(dmcshiftregister, 0) ? 2 : -2);
                //DMC output register doesn't wrap around
                if (dmcvalue > 0x7f) {
                    dmcvalue = 0x7f;
                }
                if (dmcvalue < 0) {
                    dmcvalue = 0;
                }
                dmcshiftregister >>= 1;
                --dmcbitsleft;
                if (dmcbitsleft <= 0) {
                    dmcbitsleft = 8;
                    dmcshiftregister = dmcbuffer;
                    dmcfillbuffer();
                }
            }
        }
    }

    private void dmcfillbuffer() {
        if (dmcsamplesleft > 0) {
            dmcbuffer = cpuram.read(dmcaddr++);
            cpu.cycles += 2;
            //DPCM Does steal cpu cycles - this should actually vary between 1-4
            //can't do this properly without a cycle accurate cpu/ppu
            if (dmcaddr > 0xffff) {
                dmcaddr = 0x8000;
            }
            --dmcsamplesleft;
        } else {
            if (dmcloop) {
                restartdmc();
            } else if (dmcirq && !statusdmcint) {
                ++cpu.interrupt;
                statusdmcint = true;
                //System.err.println("dmc irq");
            } else {
                dmcsilence = true;
            }
        }

    }

    private void restartdmc() {
        dmcaddr = dmcstartaddr;
        dmcsamplesleft = dmcsamplelength;
        dmcsilence = false;
    }
    private final int[] lengthctr = {0, 0, 0, 0};
    private final static int[] lenctrload = {10, 254, 20, 2, 40, 4, 80, 6,
        160, 8, 60, 10, 14, 12, 26, 14, 12, 16, 24, 18, 48, 20, 96, 22,
        192, 24, 72, 26, 16, 28, 32, 30};
    private final boolean[] lenctrHalt = {true, true, true, true};

    private void setlength() {
        for (int i = 0; i < 4; ++i) {
            if (!lenctrHalt[i] && lengthctr[i] > 0) {
                --lengthctr[i];
                if (lengthctr[i] == 0) {
                    setvolumes();
                }
            }
        }
    }
    private int linearctr = 0;
    private int linctrreload = 0;
    private boolean linctrflag = false;

    private void setlinctr() {
        if (linctrflag) {
            linearctr = linctrreload;
        } else if (linearctr > 0) {
            --linearctr;
        }
        if (!lenctrHalt[2]) {
            linctrflag = false;
        }
    }
    //instance variables for envelope units
    private final int[] envelopeValue = {15, 15, 15, 15};
    private final int[] envelopeCounter = {0, 0, 0, 0};
    private final int[] envelopePos = {0, 0, 0, 0};
    private final boolean[] envConstVolume = {true, true, true, true};
    private final boolean[] envelopeStartFlag = {false, false, false, false};

    private void setenvelope() {
        for (int i = 0; i < 4; ++i) {
            if (envelopeStartFlag[i]) {
                envelopeStartFlag[i] = false;
                envelopePos[i] = envelopeValue[i] + 1;
                envelopeCounter[i] = 15;
            } else {
                --envelopePos[i];
            }
            if (envelopePos[i] <= 0) {
                envelopePos[i] = envelopeValue[i] + 1;
                if (envelopeCounter[i] > 0) {
                    --envelopeCounter[i];
                } else if (lenctrHalt[i] && envelopeCounter[i] <= 0) {
                    envelopeCounter[i] = 15;
                }
            }
        }
    }
    //instance variables for sweep unit
    private final boolean[] sweepenable = {false, false},
            sweepnegate = {false, false},
            sweepsilence = {false, false},
            sweepreload = {false, false};
    private final int[] sweepperiod = {15, 15}, sweepshift = {0, 0}, sweeppos = {0, 0};

    private void setsweep() {
        for (int i = 0; i < 2; ++i) {
            sweepsilence[i] = false;
            if (sweepreload[i]) {
                sweepreload[i] = false;
                sweeppos[i] = sweepperiod[i];
            } else {
                ++sweeppos[i];
                final int rawperiod = timers[i].getperiod();
                int shiftedperiod = (rawperiod >> sweepshift[i]);
                if (sweepnegate[i]) {
                    //invert bits of period
                    //add 1 on second channel only
                    shiftedperiod = -shiftedperiod + i;
                }
                shiftedperiod += timers[i].getperiod();
                if ((rawperiod < 8)) {
                    // silence channel
                    sweepsilence[i] = true;
                } else if (sweepenable[i] && (sweepshift[i] != 0) && lengthctr[i] > 0
                        && sweeppos[i] > sweepperiod[i]) {
                    sweeppos[i] = 0;
                    timers[i].setperiod(shiftedperiod);
                }
            }
        }
    }
}
