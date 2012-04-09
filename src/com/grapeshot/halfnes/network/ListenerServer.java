package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ListenerServer extends Listener {
    
    protected KryoServer server;
    
    public ListenerServer(KryoServer server) {
        this.server = server;
    }
    
    public void disconnected(Connection c) {
        // TODO
    }

    public void connected(Connection c) {
        this.server.setConnectionId(c.getID());
    }
}
