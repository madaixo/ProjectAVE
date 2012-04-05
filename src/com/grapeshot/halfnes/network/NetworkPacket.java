package com.grapeshot.halfnes.network;

import java.io.Serializable;

public class NetworkPacket implements Serializable {

    private static final long serialVersionUID = 4248012900067896009L;
    private static int idCounter = 0;

    public enum PacketType { FRAME, CONTROLLER, PAUSE, RESUME, TITLE, PING, PONG }
    
    private int id;
    private final PacketType type;
    
    /*private int[] videoBitmap = null;
    private int videoBgColor = -1;
    private long frametime = -1;
    
    private int audioSample = -1;
    private int audioFlush = -1;
    
    private int controllerByte = -1;
    
    private String title;*/
    
    
    public NetworkPacket(PacketType type) {
        id = NetworkPacket.getNextId();
        this.type = type;
    }

    /*public int[] getVideoBitmap() {
        return videoBitmap;
    }

    public void setVideoBitmap(int[] videoBitmap) {
        this.videoBitmap = videoBitmap.clone(); // without cloning the client receives an array with wrong info
    }

    public int getVideoBgColor() {
        return videoBgColor;
    }

    public void setVideoBgColor(int videoBgColor) {
        this.videoBgColor = videoBgColor;
    }

    public long getFrametime() {
        return frametime;
    }

    public void setFrametime(long frametime) {
        this.frametime = frametime;
    }

    public int getAudioSample() {
        return audioSample;
    }

    public void setAudioSample(int audioSample) {
        this.audioSample = audioSample;
    }
    
    public int getAudioFlush() {
        return audioFlush;
    }

    public void setAudioFlush(int audioFlush) {
        this.audioFlush = audioFlush;
    }

    public int getControllerByte() {
        return controllerByte;
    }

    public void setControllerByte(int controllerByte) {
        this.controllerByte = controllerByte;
    }*/

    public PacketType getType() {
        return type;
    }
    
    public synchronized static int getNextId() {
        return idCounter++;
    }

    public int getId() {
        return id;
    }

    /*public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }*/
}
