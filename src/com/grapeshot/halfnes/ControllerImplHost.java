package com.grapeshot.halfnes;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ControllerImplHost implements ControllerInterface {
    
    private int port;
    private final ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private int latchbyte = 0, controllerbyte = 0, outbyte = 0, gamepadbyte = 0;
    
    public ControllerImplHost() {
        this.port = 18452;
    }
    
    public ControllerImplHost(int port) {
        this.port = port;   // port value is correct by the time it gets here
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
        thread.execute(new Server(this));
    }

    public void stopEventQueue() {
        thread.shutdownNow();
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setGamepadbyte(int value) {
        this.gamepadbyte = value;
    }
    
    public void setControllerbyte(int value) {
        this.controllerbyte = value;
    }
}
