package com.grapeshot.halfnes.network;

public class TitlePacket extends NetworkPacket{

	private String title;
	
	public TitlePacket(String title){
		super(PacketType.TITLE);
		this.title = title;
	}
	
	public String getTitle(){
		return this.title;
	}
}
