package com.grapeshot.halfnes.network;

import java.io.IOException;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkCommon.AudioMessage;
import com.grapeshot.halfnes.network.NetworkCommon.TitleMessage;
import com.grapeshot.halfnes.network.NetworkCommon.VideoMessage;


public class KryoServer {

    private Server serverController, serverAudio, serverVideo;
    private int port = 0;
    private NES nes = null;
    private int connectionId = -1;

    public KryoServer(int port, NES nes) {
        Log.DEBUG();
        this.serverController = new Server(65536, 65536);
        this.serverAudio = new Server(65536, 65536);
        this.serverVideo = new Server(65536, 65536);
        this.port = port;
        this.nes = nes;

        // register to-be-serialized classes in Kryo
        NetworkCommon.register(serverController);
        NetworkCommon.register(serverAudio);
        NetworkCommon.register(serverVideo);

        // add listeners to server
        serverController.addListener(new ListenerServerController(this));
        serverVideo.addListener(new ListenerServer(this));
        serverAudio.addListener(new ListenerServer(this));
        
        serverController.start();
        serverVideo.start();
        serverAudio.start();
    }

    public boolean canBind() {
        try {
            this.serverController.bind(this.port, (this.port + 1));
            this.serverVideo.bind(this.port+10, (this.port + 11));
            this.serverAudio.bind(this.port+20, (this.port + 21));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void closeSocket(){
        this.serverController.stop();
        this.serverAudio.stop();
        this.serverVideo.stop();
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

    public void send(TitleMessage packet) {
        serverController.sendToUDP(this.connectionId, packet);
    }

    public void send(VideoMessage packet) {
        serverVideo.sendToUDP(this.connectionId, packet);
    }

    public void send(AudioMessage packet) {
        serverAudio.sendToUDP(this.connectionId, packet);
    }

    // utility methods
    public void sendFrame(int[] audio, int[] bitmap, int bgcolor, long frametime) {
        VideoMessage pak = new VideoMessage();
        pak.bitmap = bitmap;
        pak.background = bgcolor;
        pak.frametime = frametime;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
        
        if(nes.getSoundDevice().isSoundEnabled()) {
            AudioMessage pak2 = new AudioMessage();
            pak2.buffer = audio;
            pak2.id = NetworkCommon.getNextId();  
            this.send(pak2);
        }
    }

    public void sendTitle(String title) {
        TitleMessage pak = new TitleMessage();
        pak.title = title;
        pak.id = NetworkCommon.getNextId();
        this.send(pak);
    }
}
