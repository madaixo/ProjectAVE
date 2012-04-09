package com.grapeshot.halfnes.network;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.ControllerMessage;


public class KryoClient {
    
    private Client client, clientVideo, clientAudio;
    private int port = 0;
    private String hostAddress = null;
    private NES nes = null;
    private int connectionId = -1;
    
    private boolean stopReconnect = false;
    
    public KryoClient(String hostAddress, int port, NES nes) {
        Log.DEBUG();
        this.client = new Client(65536, 65536);
        this.clientVideo = new Client(65536, 65536);
        this.clientAudio = new Client(65536, 65536);
        this.client.start();
        this.clientVideo.start();
        this.clientAudio.start();
        
        this.nes = nes;
        this.port = port;
        this.hostAddress = hostAddress;

        // register to-be-serialized classes in Kryo
        NetworkCommon.register(client);
        NetworkCommon.register(clientVideo);
        NetworkCommon.register(clientAudio);
        
        // add listeners to server
        client.addListener(new ListenerClientController(this));
        clientVideo.addListener(new ListenerClientVideo(this));
        clientAudio.addListener(new ListenerClientAudio(this));
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
        // TODO: choose more sane ports. Try to use random ports for video and audio, ask server.
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                nes.getGUI().showConnecting();
            }
        });
        try {
            this.client.connect(5000, this.hostAddress, this.port, this.port+1);
            this.clientVideo.connect(5000, this.hostAddress, this.port+10, this.port+11);
            this.clientAudio.connect(5000, this.hostAddress, this.port+20, this.port+21);
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    nes.getGUI().hideDialog();
                }
            });
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void closeConnection() {
        this.stopReconnect = true;  
        this.client.stop();
        this.clientAudio.stop();
        this.clientVideo.stop();
    }
    
    public void retryConnection() {
        while(!stopReconnect) {
            try {
                this.clientVideo.connect(5000, this.hostAddress, this.port+10, this.port+11);
                this.clientAudio.connect(5000, this.hostAddress, this.port+20, this.port+21);
                this.client.connect(5000, this.hostAddress, this.port, this.port+1);
                break;
            } catch (IOException e) {
                this.clientVideo.close();
                this.clientAudio.close();
                this.client.close();
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                continue;
            }
        }
        
        stopReconnect = false;
    }
    
    public void setStopReconnect(boolean val) {
        this.stopReconnect = val;
    }

    public void send(ControllerMessage packet) {
        client.sendUDP(packet);
    }

    // utility methods
    public void sendControllerByte(int value) {
        ControllerMessage pak = new ControllerMessage();
        pak.controllerByte = value;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
    }
}
