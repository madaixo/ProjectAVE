package com.grapeshot.halfnes.network;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;

import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.AudioMessage;
import com.grapeshot.halfnes.network.NetworkCommon.ControllerMessage;
import com.grapeshot.halfnes.network.NetworkCommon.TitleMessage;
import com.grapeshot.halfnes.network.NetworkCommon.VideoMessage;


public class KryoClient {
    
    private Client client;
    private int port = 0;
    private String hostAddress = null;
    private NES nes = null;
    private int connectionId = -1;    
    private long videoId = -1, audioId = -1, controllerId = -1;
    
    public KryoClient(String hostAddress, int port, NES nes) {
        Log.DEBUG();
        this.client = new Client(65536, 65536);
        this.client.start();
        
        this.nes = nes;
        this.port = port;
        this.hostAddress = hostAddress;

        // register to-be-serialized classes in Kryo
        NetworkCommon.register(client);
        
        // add listeners to server
        client.addListener(new Listener() {
            public void received (Connection connection, Object object) {
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
                // client.reconnect(int timeout) <- if fails disconnect for good 
            }
            
            public void connected(Connection c) {
                setConnectionId(c.getID());
            }
        });
        
        try {
            this.client.connect(5000, this.hostAddress, this.port, this.port+1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public NES getNES() {
        return this.nes;
    }
    
    public int getConnectionId() {
        return this.connectionId;
    }
    
    public String getHostAddress() {
        return this.hostAddress;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setConnectionId(int id) {
        this.connectionId = id;
    }
    
    public boolean openConnection() {
        return true;
        /*
        try {
            this.client.connect(5000, this.hostAddress, this.port);
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        }
        */
    }

    public void closeConnection() {
        this.client.stop();
    }

    public void send(Object packet) {
        client.sendUDP(packet);
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
