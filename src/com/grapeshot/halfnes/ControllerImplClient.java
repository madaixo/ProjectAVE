package com.grapeshot.halfnes;

import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import com.grapeshot.halfnes.network.Client;

public class ControllerImplClient extends ControllerImpl {

    private Client client;
    
    public ControllerImplClient(final java.awt.Component parent, final Preferences prefs, final int controllernum, Client client) {
        super(parent, prefs, controllernum);
        this.client = client;
    }

    @Override
    public void keyPressed(final KeyEvent arg0) {
        super.keyPressed(arg0);
        if (m.containsKey(arg0.getKeyCode())) {
        	this.client.sendControllerByte(this.controllerbyte);
        }
    }

    @Override
    public void keyReleased(final KeyEvent arg0) {
        super.keyReleased(arg0);
        if (m.containsKey(arg0.getKeyCode())) {
        	this.client.sendControllerByte(this.controllerbyte);
        }
    }
    
    public void setControllerbyte(int value) {
        this.controllerbyte = value;
    }
    
    public void setGamepadbyte(int value) {
        this.gamepadbyte = value;
    }
    
    @Override
    public void stopEventQueue() {
        super.stopEventQueue();
        this.client.closeConnection();
    }
    
    // TODO: handle the gamepad keypresses/releases
}
