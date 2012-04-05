/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import javax.sound.sampled.*;

import com.grapeshot.halfnes.network.Server;

/**
 *
 * @author Andrew
 */
public class SwingAudioImpl implements AudioOutInterface {

    private boolean soundEnable;
    private SourceDataLine sdl;
    protected byte[] audiobuf;
    protected int bufptr = 0;
    private float outputvol;

    private Server server = null;

    public SwingAudioImpl(final NES nes, final int samplerate) {
        soundEnable = nes.getPrefs().getBoolean("soundEnable", true);
        outputvol = (float) (nes.getPrefs().getInt("outputvol", 13107) / 16384.);
        if (soundEnable) {
            final int samplesperframe = (int) Math.ceil((samplerate * 2) / 60.);
            audiobuf = new byte[samplesperframe];
            try {
                AudioFormat af = new AudioFormat(
                        samplerate,
                        16,//bit
                        1,//channel
                        true,//signed
                        false //little endian
                        //(works everywhere, afaict, but macs need 44100 sample rate)
                        );
                sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af, samplesperframe * 8); //create 4 frame audio buffer
                sdl.start();
            } catch (LineUnavailableException a) {
                System.err.println(a);
                nes.messageBox("Unable to inintialize sound.");
                soundEnable = false;
            } catch (IllegalArgumentException a) {
                System.err.println(a);
                nes.messageBox("Unable to inintialize sound.");
                soundEnable = false;
            }
        }
    }

    public final void flushFrame(final boolean waitIfBufferFull) {

        //if(server != null) server.sendSoundFlush();

        if (soundEnable) {

            // if (sdl.available() == sdl.getBufferSize()) {
            //     System.err.println("Audio is underrun");
            // }
            if (sdl.available() < bufptr) {
                // System.err.println("Audio is blocking");
                if (waitIfBufferFull) {

                    //write to audio buffer and don't worry if it blocks
                    sdl.write(audiobuf, 0, bufptr);
                }
                //else don't bother to write if the buffer is full
            } else {
                sdl.write(audiobuf, 0, bufptr);
            }
        }

        bufptr = 0;

    }
    int dckiller = 16384;



    public final void outputSample(int sample) {

        //if(server != null) server.sendSoundSample(sample);

        if (soundEnable) {
            sample *= outputvol;
            sample += dckiller;
            // dckiller += (sample > 0 ? -1 : 1);
            dckiller -= sample >> 12;
            if (sample < -32768) {
                sample = -32768;
            }
            if (sample > 32767) {
                sample = 32767;
            }
            audiobuf[bufptr] = (byte) (sample & 0xff);
            audiobuf[bufptr + 1] = (byte) ((sample >> 8) & 0xff);
            bufptr += 2;
        }
    }

    public void pause() {
        if (soundEnable) {
            sdl.flush();
            sdl.stop();
        }
    }

    public void resume() {
        if (soundEnable) {
            sdl.start();
        }
    }

    public final void destroy() {
        if (soundEnable) {
            sdl.stop();
            sdl.close();
        }
    }

    public final boolean bufferHasLessThan(final int samples) {
        //returns true if the audio buffer has less than the specified amt of samples remaining in it
        return (sdl == null) ? false : ((sdl.getBufferSize() - sdl.available()) <= samples);
    }

    public void setServer(Server server){
        this.server = server;
    }
}
