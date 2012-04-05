package com.grapeshot.halfnes;

public class ControllerImplHost implements ControllerInterfaceHost {
    
    private int latchbyte = 0, controllerbyte = 0, outbyte = 0, gamepadbyte = 0;
    
    public ControllerImplHost() {
        // nothing to do
    }
    
    public void strobe() {
        //shifts a byte out
        outbyte = latchbyte & 1;
        latchbyte = ((latchbyte >> 1) | 0x100);
    }

    public int getbyte() {
        return outbyte;
    }

    public void output(final boolean state) {
        latchbyte = gamepadbyte | controllerbyte;
    }
    
    public void startEventQueue() {
        // nothing to do
    }

    public void stopEventQueue() {
        // nothing to do
    }
    
    public void setGamepadbyte(int value) {
        this.gamepadbyte = value;
    }
    
    public void setControllerbyte(int value) {
        this.controllerbyte = value;
    }
}
