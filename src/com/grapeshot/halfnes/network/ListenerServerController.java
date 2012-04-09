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
}
