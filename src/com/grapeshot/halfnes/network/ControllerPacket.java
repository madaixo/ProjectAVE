package com.grapeshot.halfnes.network;

public class ControllerPacket extends NetworkPacket{

    private static final long serialVersionUID = -6603864086181345504L;

    int controllerByte;

    public ControllerPacket() {}
    
    public ControllerPacket(int value){
        super(PacketType.CONTROLLER);
        this.controllerByte = value;
    }

    public int getControllerByte(){
        return this.controllerByte;
    }
    
    public void setControllerByte(int value) {
        this.controllerByte = value;
    }
}
