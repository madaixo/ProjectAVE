package com.grapeshot.halfnes.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import com.grapeshot.halfnes.NES;

public class Server extends NetworkPeer implements Runnable {

    private ServerSocket socket1 = null;
    
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

        if(this.socket1 == null) {
        	try {
				this.socket1 = new ServerSocket(this.port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
         

        while(true) {
        	try {
				connection = socket1.accept();
			} catch (IOException e1) {
				return;
			}
        	
        	connectionClosedSignal = new CountDownLatch(1);
        	
        	reader = new Reader();
        	writer = new Writer();
        	
            reader.initConnection(connection).start();
            writer.initConnection(connection).start();


            try {
                    connectionClosedSignal.await();
            } catch (InterruptedException e) {
            	// TODO Auto-generated catch block
                e.printStackTrace();
            }
                
            writer.interrupt();

            try {
            	connection.close();
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
            }
        }
    }
}
