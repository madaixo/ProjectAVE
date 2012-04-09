package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.ControllerMessage;

public class ListenerServerController extends ListenerServer {

    private long controllerId = -1;
    
    public ListenerServerController(KryoServer server) {
        super(server);
    }

    @Override
    public void received(Connection connection, Object object) {
        NES nes = this.server.getNES();

        if (object instanceof ControllerMessage) {
            ControllerMessage packet = (ControllerMessage) object;
            if(packet.id > controllerId) {
                ControllerInterfaceHost controller = nes.getController2();
                if(controller != null) {
                    controller.setControllerbyte(packet.controllerByte);
                }
            }
        }
    }
    
    @Override
    public void disconnected(Connection c) {
        super.disconnected(c);
        if(server.getNES().getHostMode()) {   // if false means user disabled
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    server.getNES().getGUI().showClientDisconnected();
                }
            });
        }
    }
    
    @Override
    public void connected(Connection c) {
        super.connected(c);
        this.server.sendTitle(this.server.getNES().getCurrentRomName());
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                server.getNES().getGUI().hideDialog();
            }
        });
    }
}
