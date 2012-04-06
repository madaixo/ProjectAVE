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

import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkPacket.PacketType;

public abstract class NetworkPeer /* implements Runnable */ {
    
    protected NES nes = null;
    BlockingQueue<NetworkPacket> queue = new LinkedBlockingQueue<NetworkPacket>();
    //BlockingQueue<NetworkPacket> queue = new ArrayBlockingQueue<NetworkPacket>(10);
    Reader reader = null;
    Writer writer = null;

    public NetworkPeer() {
        this.reader = new Reader();
        this.writer = new Writer();
    }

    
    // utility methods
    public void sendControllerByte(int value) {
        ControllerPacket pak = new ControllerPacket(value);
        queue.offer(pak);
    }
    
    public void sendFrame(int[] audio, int[] bitmap, int bgcolor, long frametime){
    	FramePacket pak = new FramePacket(audio.clone(), bitmap.clone(), bgcolor, frametime);
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
        //NetworkPacket pak = new NetworkPacket(PacketType.TITLE);
        TitlePacket pak = new TitlePacket(title);
        //pak.setTitle(title);
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
                    //if(nes.getHostMode()) System.out.println(queue.size());
                    osw.writeObject(send);
                    osw.flush();
                    osw.reset();
                } catch (SocketException e) {
                    // TODO: warn there might have been a disconnect (could only be temporary)
                	System.out.println("erro?");
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
