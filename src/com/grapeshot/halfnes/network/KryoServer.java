package com.grapeshot.halfnes.network;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.AudioMessage;
import com.grapeshot.halfnes.network.NetworkCommon.ControllerMessage;
import com.grapeshot.halfnes.network.NetworkCommon.TitleMessage;
import com.grapeshot.halfnes.network.NetworkCommon.VideoMessage;


public class KryoServer {

    private Server serverController;
    private int port = 0;
    private NES nes = null;
    private int connectionId = -1;
    
    private long videoId = -1, audioId = -1, controllerId = -1;

    public KryoServer(int port, NES nes) {
        Log.DEBUG();
        this.serverController = new Server(65536, 65536);
        this.port = port;
        this.nes = nes;

        // register to-be-serialized classes in Kryo
        NetworkCommon.register(serverController);

        // add listeners to server
        serverController.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                NES nes = getNES();

                if (object instanceof ControllerMessage) {
                    ControllerMessage packet = (ControllerMessage) object;
                    if(packet.id > controllerId) {
                        ControllerInterfaceHost controller = nes.getController2();
                        if(controller != null) {
                            controller.setControllerbyte(packet.controllerByte);
                        }
                    }
                }

                if (object instanceof VideoMessage) {
                    VideoMessage packet = (VideoMessage) object;
                    if(packet.id > videoId) {
                        nes.setFrameTime(packet.frametime);
                        nes.getGUI().setFrame(packet.bitmap, packet.background);
                    }
                }

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

                if (object instanceof TitleMessage) {
                    nes.setCurrentRomName(((TitleMessage) object).title);
                }
            }

            public void disconnected(Connection c) {
                // TODO
            }

            public void connected(Connection c) {
                setConnectionId(c.getID());
            }
        });
        
        serverController.start();
    }

    public boolean canBind() {
        try {
            this.serverController.bind(this.port, (this.port + 1));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void closeSocket(){
        this.serverController.stop();
    }

    public NES getNES() {
        return this.nes;
    }

    public int getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(int id) {
        this.connectionId = id;
    }

    public void send(Object packet) {
        serverController.sendToUDP(this.connectionId, packet);
    }

    // utility methods
    public void sendControllerByte(int value) {
        ControllerMessage pak = new ControllerMessage();
        pak.controllerByte = value;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
    }

    public void sendFrame(int[] audio, int[] bitmap, int bgcolor, long frametime) {
        VideoMessage pak = new VideoMessage();
        pak.bitmap = bitmap;
        pak.background = bgcolor;
        pak.frametime = frametime;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
        
        AudioMessage pak2 = new AudioMessage();
        pak2.buffer = audio;
        pak2.id = NetworkCommon.getNextId();  
        this.send(pak2);
    }

    public void sendTitle(String title) {
        TitleMessage pak = new TitleMessage();
        pak.title = title;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
    }
}
