package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.VideoMessage;

public class ListenerClientVideo extends ListenerClient {

    private long videoId = -1;
    
    public ListenerClientVideo(KryoClient client) {
        super(client);
    }
    
    @Override
    public void received (Connection connection, Object object) {
        NES nes = this.client.getNES();

        if (object instanceof VideoMessage) {
            VideoMessage packet = (VideoMessage) object;
            if(packet.id > videoId) {
                nes.setFrameTime(packet.frametime);
                nes.getGUI().setFrame(packet.bitmap, packet.background);
            }
        }
    }
}
