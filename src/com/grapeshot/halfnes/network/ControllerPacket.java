package com.grapeshot.halfnes.network;

public class ControllerPacket extends NetworkPacket{

    private static final long serialVersionUID = -6603864086181345504L;

    int value;

    public ControllerPacket(int value){
        super(PacketType.CONTROLLER);
        this.value = value;
    }

    public int getControllerByte(){
        return this.value;
    }
}
