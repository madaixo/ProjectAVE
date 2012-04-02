package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private int port;
    private ControllerImplHost controller;
    private ServerSocket socket1 = null;
    
    public Server(int port, ControllerImplHost controller) {
        this.port = port;
        this.controller = controller;
    }
    
    public Server(int port) {
        this.port = port;
    }
    
    public void setController(ControllerImplHost controller) {
        this.controller = controller;
    }
    
    public boolean canBind() {
        try {
            this.socket1 = new ServerSocket(this.port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    @Override
    public void run() {
        // open a socket to receive controller input and return computed state
        Socket connection = null;
        
        try {
            if(this.socket1 == null) {
                this.socket1 = new ServerSocket(this.port);
            }
            int character;
            
            while (true) {
                connection = socket1.accept();
                
                BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
                InputStreamReader isr = new InputStreamReader(is);
                StringBuffer process = new StringBuffer();
                
                while((character = isr.read()) != 13) { // ASCII character 13 = CR
                    process.append((char)character);
                }
                
                // TODO: 
                // - handle delays/packet loss
                // - decide if we really want to use TCP or if UDP is enough
                
                
                System.out.println(process);
                controller.setControllerbyte(Integer.parseInt(process.toString()));
                
                try {
                    Thread.sleep(10);
                }
                catch (Exception e) {
                    // TODO: handle it
                }
                
                String returnCode = "ACK " + process + (char) 13;
                BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
                OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
                osw.write(returnCode);
                osw.flush();
            }
        }
        catch (IOException e) {
            // TODO: handle it
        }
        
        try {
            connection.close();
        }
        catch (IOException e) {
            // TODO: handle it
        }
    }
}
