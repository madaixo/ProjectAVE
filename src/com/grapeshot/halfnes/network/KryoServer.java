package com.grapeshot.halfnes.network;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkPacket.PacketType;


public class KryoServer {

    private Server server;
    private int port = 0;
    private NES nes = null;
    private int connectionId = -1;

    public KryoServer(int port, NES nes) {
        Log.DEBUG();
        this.server = new Server(65536, 65536);
        this.port = port;
        this.nes = nes;

        // register to-be-serialized classes in Kryo
        Kryo kryo = server.getKryo();
        kryo.register(NetworkPacket.class);
        kryo.register(FramePacket.class);
        kryo.register(TitlePacket.class);
        kryo.register(ControllerPacket.class);
        kryo.register(NetworkPacket.PacketType.class);
        kryo.register(int[].class);
        // kryo.register(String.class);
        
        // add listeners to server
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
             // all received or sent objects are NetworkPackets except KryoNet keep-alive packets and such
                if (object instanceof NetworkPacket) {
                    NetworkPacket packet = (NetworkPacket) object;
                    PacketType type = packet.getType();

                    NES nes = getNES();

                    switch(type) {
                    case FRAME:
                        int[] audioSamples = ((FramePacket) packet).getAudioSamples();

                        AudioOutInterface ai = nes.getSoundDevice();
                        for(int i = 0; i < audioSamples.length; i++)
                            ai.outputSample(audioSamples[i]);

                        ai.flushFrame(false);
                        nes.setFrameTime(((FramePacket) packet).getFrametime());
                        nes.getGUI().setFrame(((FramePacket) packet).getBitmap(), ((FramePacket) packet).getBgcolor());
                        break;
                    case CONTROLLER:
                        ControllerInterfaceHost controller = nes.getController2();
                        if(controller != null) {
                            controller.setControllerbyte(((ControllerPacket) packet).getControllerByte());
                        }
                        break;
                    case PAUSE:
                        // TODO: emulation is paused for some reason, stop emulation and show a message
                        break;
                    case RESUME:
                        // TODO: resume a pause emulation session
                        break;
                    case TITLE:
                        nes.setCurrentRomName(((TitlePacket) packet).getTitle());
                        break;
                    case PING:
                        sendPong();
                        break;
                    default:
                        break;
                    }
                }
            }

            public void disconnected(Connection c) {
                // TODO
            }
            
            public void connected(Connection c) {
                setConnectionId(c.getID());
            }
        });
        
        server.start();
    }

    public boolean canBind() {
        try {
            this.server.bind(this.port/*, (this.port + 1)*/);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void closeSocket(){
        this.server.stop();
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

    public void send(NetworkPacket packet) {
        server.sendToTCP(this.connectionId, packet);
    }
    
    // utility methods
    public void sendControllerByte(int value) {
        ControllerPacket pak = new ControllerPacket(value);
        this.send(pak);
    }

    public void sendFrame(int[] audio, int[] bitmap, int bgcolor, long frametime){
        FramePacket pak = new FramePacket(audio.clone(), bitmap.clone(), bgcolor, frametime);
        this.send(pak);
    }

    public void sendPause() {
        NetworkPacket pak = new NetworkPacket(PacketType.PAUSE);
        this.send(pak);
    }

    public void sendResume() {
        NetworkPacket pak = new NetworkPacket(PacketType.RESUME);
        this.send(pak);
    }

    public void sendTitle(String title) {
        TitlePacket pak = new TitlePacket(title);
        this.send(pak);
    }

    public void sendPing() {
        NetworkPacket pak = new NetworkPacket(PacketType.PING);
        this.send(pak);
    }

    public void sendPong() {
        NetworkPacket pak = new NetworkPacket(PacketType.PONG);
        this.send(pak);
    }
}
