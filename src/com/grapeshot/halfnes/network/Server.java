package com.grapeshot.halfnes.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import com.grapeshot.halfnes.NES;

public class Server extends NetworkPeer implements Runnable {

    private ServerSocket socket1 = null;
    private CountDownLatch connectionClosedSignal = new CountDownLatch(1);
    
    private int port = 0;
    
    public Server(int port, NES nes) {
        super();
        this.port = port;
        this.nes = nes;
    }

    public boolean canBind() {
        try {
            this.socket1 = new ServerSocket(this.port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public void closeSocket(){
        if(this.socket1 != null) {
            try {
                this.socket1.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void run() {
        Socket connection = null;

        try {
            if(this.socket1 == null) {
                this.socket1 = new ServerSocket(this.port);
            }

            while(true) {
                connection = socket1.accept();
                reader.initConnection(connection).start();
                writer.initConnection(connection).start();

                try {
                    connectionClosedSignal.await();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                writer.interrupt();
                reader.interrupt();
            }
        }
        catch (IOException e) {
            // TODO: handle it
            e.printStackTrace();
        }
    }
}
