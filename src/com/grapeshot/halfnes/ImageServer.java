package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ImageServer implements Runnable{
	
    private GUIInterface gui;
    
    public ImageServer(GUIImpl gui) {
        this.gui = gui;
    }
    
    @Override
    public void run() {
        // open a socket to receive controller input and return computed state
        
        ServerSocket socket1;
        Socket connection = null;
        
        try {
            socket1 = new ServerSocket(18452); //TODO: fixed ports are ugly
            int character;
            
            while (true) {
                connection = socket1.accept();
                
                int[] map;
                int bgcolor;
                System.out.println("TESTE");
                BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
                ObjectInputStream res = new ObjectInputStream(bis);
                map = (int [])res.readObject();
                bgcolor = res.readInt();
                //res.flush();
                gui.setFrame(map, bgcolor);
                
                }  
            
        }
        catch (IOException e) {
            // FIXME: handle it
        } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
            connection.close();
        }
        catch (IOException e) {
            // FIXME: handle it
        }
    }

}
