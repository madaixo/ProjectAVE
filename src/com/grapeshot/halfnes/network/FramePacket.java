package com.grapeshot.halfnes.network;

public class FramePacket extends NetworkPacket {

    private static final long serialVersionUID = 1295710491405836304L;

    private int[] audioSamples;
    private int[] bitmap;
    int bgcolor;
    long frametime;

    public FramePacket(int[] audioSamples, int[] bitmap, int bgcolor, long frametime) {
        super(PacketType.FRAME);
        this.audioSamples = audioSamples;
        this.bitmap = bitmap;
        this.bgcolor = bgcolor;
        this.frametime = frametime;
    }

    public int[] getAudioSamples(){
        return this.audioSamples;
    }

    public int[] getBitmap(){
        return this.bitmap;
    }

    public int getBgcolor(){
        return this.bgcolor;
    }

    public long getFrametime(){
        return this.frametime;
    }

}
