package com.grapeshot.halfnes.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.grapeshot.halfnes.AudioOutInterface;
import com.grapeshot.halfnes.ControllerInterfaceHost;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.network.NetworkPacket.PacketType;


public abstract class NetworkPeer {

    protected NES nes = null;
    // BlockingQueue<NetworkPacket> queue = new LinkedBlockingQueue<NetworkPacket>();
    LimitedSizeQueue<NetworkPacket> queue = new LimitedSizeQueue<NetworkPacket>(5);
    Reader reader = null;
    Writer writer = null;

    public NetworkPeer() {
        this.reader = new Reader();
        this.writer = new Writer();
    }


    // utility methods
    public void sendControllerByte(int value) {
        ControllerPacket pak = new ControllerPacket(value);
        // queue.offer(pak);
        queue.add(pak);
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
        TitlePacket pak = new TitlePacket(title);
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
                    send = (NetworkPacket) queue.get();
                    osw.writeObject(send);
                    osw.flush(); 
                } catch (SocketException e) {
                    // TODO: warn there might have been a disconnect (could only be temporary)
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
    
    class LimitedSizeQueue<ElementType> implements Queue<ElementType>
    {
        private int maxSize;
        private LinkedList<ElementType> storageArea;

        public LimitedSizeQueue(final int maxSize)
        {
            if (maxSize > 0)
            {
                this.maxSize = maxSize;
                storageArea = new LinkedList<ElementType>();
            }
            else
            {
                throw new IllegalArgumentException("blah blah blah");
            }
        }
        
        @Override
        public boolean addAll(Collection<? extends ElementType> c) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public void clear() {
            // TODO Auto-generated method stub
            
        }
        @Override
        public boolean contains(Object o) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public boolean isEmpty() {
            return this.storageArea.isEmpty();
        }
        @Override
        public Iterator<ElementType> iterator() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public boolean remove(Object o) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }
        @Override
        public int size() {
            return this.storageArea.size();
        }
        @Override
        public Object[] toArray() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <T> T[] toArray(T[] a) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public boolean add(ElementType arg0) {
            if (storageArea.size() < maxSize) {
                storageArea.addFirst(arg0);
            } else {
                storageArea.removeLast();
                storageArea.addFirst(arg0);
            }
            return true;
        }
        @Override
        public ElementType element() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public boolean offer(ElementType arg0) {
            if (storageArea.size() < maxSize) {
                storageArea.addFirst(arg0);
            } else {
                storageArea.removeLast();
                storageArea.addFirst(arg0);
            }
            return true;
        }
        @Override
        public ElementType peek() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public ElementType poll() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public ElementType remove() {
            return (ElementType) this.storageArea.remove();
        }
        
        public ElementType get() throws InterruptedException {
            while(this.storageArea.isEmpty()) { // simulate blocking get() call
                Thread.sleep(50);
            }
            return this.storageArea.removeLast();
        }
    }
}
