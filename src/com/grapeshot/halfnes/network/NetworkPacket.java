package com.grapeshot.halfnes.network;

import java.io.Serializable;

public class NetworkPacket implements Serializable {

    private static final long serialVersionUID = 4248012900067896009L;
    private static int idCounter = 0;

    public enum PacketType { FRAME, CONTROLLER, PAUSE, RESUME, TITLE, PING, PONG }
    
    private int id;
    private PacketType type;
    
    public NetworkPacket() {
        id = NetworkPacket.getNextId();
    }
    
    public NetworkPacket(PacketType type) {
        id = NetworkPacket.getNextId();
        this.type = type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public PacketType getType() {
        return type;
    }
    
    public synchronized static int getNextId() {
        return idCounter++;
    }

    public int getId() {
        return id;
    }
}
