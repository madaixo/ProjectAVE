package com.grapeshot.halfnes;

import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

public class ControllerImplClient extends ControllerImpl {

    private static Client client;
    
    public ControllerImplClient(final java.awt.Component parent, final Preferences prefs, final int controllernum, String hostAddress, int hostPort) {
        super(parent, prefs, controllernum);
        client = new Client(hostAddress, hostPort);
    }

    @Override
    public void keyPressed(final KeyEvent arg0) {
        super.keyPressed(arg0);
        client.sendControllerbyte(this.controllerbyte);
    }

    @Override
    public void keyReleased(final KeyEvent arg0) {
        super.keyReleased(arg0);
        client.sendControllerbyte(this.controllerbyte);
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
        client.closeConnection();
    }
    
    // TODO: handle the gamepad keypresses/releases
}
