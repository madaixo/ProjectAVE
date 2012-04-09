package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.TitleMessage;

public class ListenerClientController extends ListenerClient {
    
    public ListenerClientController(KryoClient client) {
        super(client);
    }
    
    @Override
    public void received (Connection connection, Object object) {
        NES nes = this.client.getNES();

        if (object instanceof TitleMessage) {
            nes.setCurrentRomName(((TitleMessage) object).title);
        }
    }
    
    @Override
    public void disconnected(Connection c) {
        super.disconnected(c);
        client.setStopReconnect(false);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                client.getNES().getGUI().showServerDisconnected();
            }
        });
        new Thread(new Runnable() {
            public void run() {
                client.retryConnection();
            }
        }).start();
    }
    
    @Override
    public void connected(Connection c) {
        super.connected(c);
        client.setStopReconnect(true);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                client.getNES().getGUI().hideDialog();
            }
        });
    }
}
