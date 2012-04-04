package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ImageClient {
	private String hostAddress = "127.0.0.1"; //TODO: yes yes i know...
    private int hostPort = 18452;
    private InetAddress address;
    private Socket connection;
    private GUIImpl gui;
    
    public ImageClient(GUIImpl gui){
    	this.gui = gui;
    	
    }
    /*
    public ImageClient(String hostAddress, int hostPort, GUIImpl gui ) {
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.gui = gui;
    }
    */
    public void sendNewFrame() {
       // StringBuffer instr = new StringBuffer();

        this.openConnection();
        
        try {
        	BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
            ObjectOutputStream res = new ObjectOutputStream(os);
            //System.out.println("host");
            res.writeObject(gui.bitmap);
            //res.flush();
            res.writeInt(gui.color);
            res.flush();
            // TODO: use the server's response as new state or new state delta (to be decided)

        }
        catch (IOException f) {
            System.out.println("IOException: " + f);
        }
        catch (Exception g) {
            System.out.println("Exception: " + g);
        }
        
        this.closeConnection();
    }
    
    public void openConnection() {
        try {
            this.address = InetAddress.getByName(this.hostAddress);
            this.connection = new Socket(this.address, this.hostPort);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void closeConnection() {
        try {
            this.connection.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
