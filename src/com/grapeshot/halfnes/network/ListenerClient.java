package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ListenerClient extends Listener {

    protected KryoClient client;
    
    public ListenerClient(KryoClient client) {
        this.client = client;
    }
    
    public void disconnected(Connection c) {
        // TODO
        // client.reconnect(int timeout) <- if fails disconnect for good 
    }
    
    public void connected(Connection c) {
        this.client.setConnectionId(c.getID());
    }
}
