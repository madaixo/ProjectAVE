package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private ControllerImplHost controller;
    
    public Server(ControllerImplHost controller) {
        this.controller = controller;
    }
    
    @Override
    public void run() {
        // open a socket to receive controller input and return computed state
        
        ServerSocket socket1;
        Socket connection = null;
        
        try {
            socket1 = new ServerSocket(controller.getPort());
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
                    // FIXME: handle it
                }
                
                // TODO: send back new state (or just the delta)
                String returnCode = "ACK " + process + (char) 13;
                BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
                OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
                osw.write(returnCode);
                osw.flush();
            }
        }
        catch (IOException e) {
            // FIXME: handle it
        }
        
        try {
            connection.close();
        }
        catch (IOException e) {
            // FIXME: handle it
        }
    }
}
