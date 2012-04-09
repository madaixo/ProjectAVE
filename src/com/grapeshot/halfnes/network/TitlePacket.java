package com.grapeshot.halfnes.network;

public class TitlePacket extends NetworkPacket{

    private static final long serialVersionUID = -8364221577822557500L;

    private String title;

    public TitlePacket() {}
    
    public TitlePacket(String title){
        super(PacketType.TITLE);
        this.title = title;
    }

    public String getTitle(){
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
}
