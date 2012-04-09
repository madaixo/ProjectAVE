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
        
        try {
            this.client.connect(5000, this.hostAddress, this.port, this.port+1);
            this.clientVideo.connect(5000, this.hostAddress, this.port+10, this.port+11);
            this.clientAudio.connect(5000, this.hostAddress, this.port+20, this.port+21);
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
