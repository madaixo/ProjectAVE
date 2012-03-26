package com.grapeshot.halfnes;

public class ControllerFake implements ControllerInterface {

    public int getbyte() {
        return 0;
    }

    public void strobe() {
        // do nothing
    }

    public void output(final boolean state) {
        // do nothing
    }

    public void startEventQueue() {
        // do nothing
    }

    public void stopEventQueue() {
        // do nothing
    }
}
