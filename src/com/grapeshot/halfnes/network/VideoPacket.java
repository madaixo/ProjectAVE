package com.grapeshot.halfnes.network;

import java.io.Serializable;

public class VideoPacket implements Serializable{

	private int[] bitmap;
	private int bgcolor;
	private int frametime;
	
	public VideoPacket(int[] bitmap, int bgcolor, int frametime){
		this.bitmap = bitmap;
		this.bgcolor = bgcolor;
		this.frametime = frametime;
	}
}
