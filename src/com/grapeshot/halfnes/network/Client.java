package com.grapeshot.halfnes.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.grapeshot.halfnes.NES;

public class Client extends NetworkPeer {

    private String hostAddress = null;
    private int port = 0;
    private InetAddress address = null;
    private Socket connection = null;
    
    public Client(String hostAddress, int hostPort, NES nes) {
        super();
        this.hostAddress = hostAddress;
        this.port = hostPort;
        this.nes = nes;
    }

    public boolean openConnection() {
        try {
            this.address = InetAddress.getByName(this.hostAddress);
            this.connection = new Socket(this.address, this.port);
            
            this.sendPing();
            
            reader.initConnection(connection).start();
            writer.initConnection(connection).start();

            return true;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        }
    }

    public void closeConnection() {
        try {
            this.connection.close();
            writer.interrupt();
            reader.interrupt();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
