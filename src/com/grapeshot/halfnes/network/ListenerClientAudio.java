package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryonet.Connection;
import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.AudioMessage;

public class ListenerClientAudio extends ListenerClient {

    private long audioId = -1;
    
    public ListenerClientAudio(KryoClient client) {
        super(client);
    }
    
    @Override
    public void received (Connection connection, Object object) {
        NES nes = this.client.getNES();

        if (object instanceof AudioMessage) {
            AudioMessage packet = (AudioMessage) object;
            if(packet.id > audioId) {
                int[] audioSamples = packet.buffer; 

                AudioOutInterface ai = nes.getSoundDevice();
                for(int i = 0; i < audioSamples.length; i++)
                    ai.outputSample(audioSamples[i]);

                ai.flushFrame(false);
            }
        } 
    }
}
