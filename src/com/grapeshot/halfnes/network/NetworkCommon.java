package com.grapeshot.halfnes.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class NetworkCommon {
    
    private static long idCounter = 0;
        
    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(int[].class);
        kryo.register(ControllerMessage.class);
        kryo.register(TitleMessage.class);
        kryo.register(VideoMessage.class);
        kryo.register(AudioMessage.class);
    }
    
    public synchronized static long getNextId() {
        return idCounter++;
    }
    
    
    // Message classes
    static public class ControllerMessage {
        public int controllerByte;
        public long id;
    }
    
    static public class TitleMessage {
        public String title;
        public long id;
    }
    
    static public class VideoMessage {
        public int[] bitmap;
        public int background;
        public long frametime;
        public long id;
    }
    
    static public class AudioMessage {
        public int[] buffer;
        public long id;
    }
    
    
}
