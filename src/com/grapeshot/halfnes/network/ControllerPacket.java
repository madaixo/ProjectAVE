package com.grapeshot.halfnes.network;

public class ControllerPacket extends NetworkPacket{

	int value;
	
	public ControllerPacket(int value){
		super(PacketType.CONTROLLER);
		this.value = value;
	}
	
	public int getControllerByte(){
		return this.value;
	}
}
