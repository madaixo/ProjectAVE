package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Server implements Runnable {

    private int port;
    private ControllerImplHost controller;
    private ServerSocket socket1 = null;
    private CountDownLatch connectionClosedSignal = new CountDownLatch(1);
    private BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    
    //Test
    BufferedOutputStream bos;
    OutputStreamWriter osw;
    
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
    
    public void sendSoundSample(int sample){
    	String process = "" + sample + (char) 13;
    	queue.offer(process);
    }
    
    public void sendSoundFlush(){
    	String process = "" + 0 + (char) 13;
    	queue.offer(process);
    }
    
    @Override
    public void run() {
        // open a socket to receive controller input and return computed state
    	Socket connection = null;
    	
        try {
            if(this.socket1 == null) {
                this.socket1 = new ServerSocket(this.port);
            }
            
            while(true){
            	
            	connection = socket1.accept();
            	
            	Reader reader = new Reader(connection);
            	reader.start();
            	Writer writer = new Writer(connection);
            	writer.start();
            	
            	try {
					connectionClosedSignal.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
            	writer.interrupt();
            }
        }
        catch (IOException e) {
            // TODO: handle it
        	e.printStackTrace();
        }
    }
    
    class Writer extends Thread{
    	
    	BufferedOutputStream bos;
        OutputStreamWriter osw;
        
        public Writer(Socket connection){
        	try {
				bos = new BufferedOutputStream(connection.getOutputStream());
				osw = new OutputStreamWriter(bos, "US-ASCII");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        public void run(){
        	
        	String send = null;
			while(true){
				try {
					send = queue.take();
					osw.write(send);
					osw.flush();
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
    	private InputStreamReader isr;
    	
    	public Reader(Socket connection){

    		try {
				is = new BufferedInputStream(connection.getInputStream());
				isr = new InputStreamReader(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	public void run(){
    		
    		try {
    			int character;
        	
    			while(true){
        		
    				StringBuffer process = new StringBuffer();
    				
					while((character = isr.read()) != 13) { // ASCII character 13 = CR
						if(character == -1){
							connectionClosedSignal.countDown();
							return;
						}
					    process.append((char)character);
					}
                
					System.out.println(process);
					controller.setControllerbyte(Integer.parseInt(process.toString()));
        		
    			}
    		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
}
