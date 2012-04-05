package com.grapeshot.halfnes.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkPacket.PacketType;

public abstract class NetworkPeer /* implements Runnable */ {
    
    protected NES nes = null;
    BlockingQueue<NetworkPacket> queue = new LinkedBlockingQueue<NetworkPacket>();
    Reader reader = null;
    Writer writer = null;

    public NetworkPeer() {
        this.reader = new Reader();
        this.writer = new Writer();
    }

    
    // utility methods
    public void sendSoundSample(int sample) {
        NetworkPacket pak = new NetworkPacket(PacketType.AUDIO_SAMPLE);
        pak.setAudioSample(sample);
        queue.offer(pak);
    }

    public void sendSoundFlush() {
        NetworkPacket pak = new NetworkPacket(PacketType.AUDIO_FLUSH);
        pak.setAudioSample(0);
        queue.offer(pak);
    }
    
    public void sendVideoFrame(int[] bitmap, int bgcolor, long frametime) {
        NetworkPacket pak = new NetworkPacket(PacketType.VIDEO);
        pak.setVideoBitmap(bitmap);
        pak.setVideoBgColor(bgcolor);
        pak.setFrametime(frametime);
        queue.offer(pak);
    }
    
    public void sendControllerByte(int value) {
        NetworkPacket pak = new NetworkPacket(PacketType.CONTROLLER);
        pak.setControllerByte(value);
        queue.offer(pak);
    }
    
    public void sendPause() {
        NetworkPacket pak = new NetworkPacket(PacketType.PAUSE);
        queue.offer(pak);
    }
    
    public void sendResume() {
        NetworkPacket pak = new NetworkPacket(PacketType.RESUME);
        queue.offer(pak);
    }
    
    public void sendTitle(String title) {
        NetworkPacket pak = new NetworkPacket(PacketType.TITLE);
        pak.setTitle(title);
        queue.offer(pak);
    }
    
    public void sendPing() {
        NetworkPacket pak = new NetworkPacket(PacketType.PING);
        queue.offer(pak);
    }
    
    public void sendPong() {
        NetworkPacket pak = new NetworkPacket(PacketType.PONG);
        queue.offer(pak);
    }

        
    class Writer extends Thread {

        BufferedOutputStream bos;
        ObjectOutputStream osw;
        
        public Writer() {}
        
        public Writer(Socket connection) {
            initConnection(connection);
        }
        
        public Writer initConnection(Socket connection) {
            try {
                bos = new BufferedOutputStream(connection.getOutputStream());
                return this;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return this;
            }
        }

        public void run() {
            try {
                osw = new ObjectOutputStream(bos);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            NetworkPacket send = null;
            
            while(true) {
                try {
                    send = queue.take();
                    osw.writeObject(send);
                    osw.flush(); 
                } catch (SocketException e) {
                    // TODO: warn there might have been a disconnect (could only be temporary)
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    class Reader extends Thread{

        private BufferedInputStream is;
        private ObjectInputStream isr;

        public Reader() {}
        
        public Reader(Socket connection) {
            initConnection(connection);
        }
        
        public Reader initConnection(Socket connection) {
            try {
                is = new BufferedInputStream(connection.getInputStream());
                return this;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return this;
            }
        }

        public void run() {
            try {
                isr = new ObjectInputStream(is);    // this blocks waiting for a flush from server
            
                NetworkPacket packet;

                while(true) {
                    packet = (NetworkPacket) isr.readObject();
                    
                    PacketType type = packet.getType();
                    switch(type) {
                    case AUDIO_SAMPLE:
                        nes.getSoundDevice().outputSample(packet.getAudioSample());
                        break;
                    case AUDIO_FLUSH:
                        nes.getSoundDevice().flushFrame(false);
                        break;
                    case VIDEO:
                        nes.setFrameTime(packet.getFrametime());
                        nes.getGUI().setFrame(packet.getVideoBitmap(), packet.getVideoBgColor());
                        break;
                    case CONTROLLER:
                        ControllerInterfaceHost controller = nes.getController2();
                        if(controller != null) {
                            controller.setControllerbyte(packet.getControllerByte());
                        }
                        break;
                    case PAUSE:
                        // TODO: emulation is paused for some reason, stop emulation and show a message
                        break;
                    case RESUME:
                        // TODO: resume a pause emulation session
                        break;
                    case TITLE:
                        nes.setCurrentRomName(packet.getTitle());
                        break;
                    case PING:
                        sendPong();
                        break;
                    default:
                        break;
                    }
                }
            } catch (IOException e) {
                // FIXME: ver se funciona
                return;
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
