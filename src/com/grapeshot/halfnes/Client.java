package com.grapeshot.halfnes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    private String hostAddress;
    private int hostPort;
    private InetAddress address;
    private Socket connection;
    private AudioOutInterface soundDevice;
    
    BufferedOutputStream bos;
    OutputStreamWriter osw;
    
    public Client(String hostAddress, int hostPort, AudioOutInterface ai) {
    	this.soundDevice = ai;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
    }
    
    public void sendControllerbyte(int byteVal) {
        //this.openConnection();        
        this.send("" + byteVal);        
        //this.closeConnection();
    }
    
    private void send(String msg) {
        //StringBuffer instr = new StringBuffer();
        
        try {
            String process = "" + msg + (char) 13;  // using ASCII char 13 (aka CR) as terminating char
            System.out.println("Sending "+process);
            osw.write(process);
            osw.flush();

        }
        catch (IOException f) {
            System.out.println("IOException: " + f);
        }
        catch (Exception g) {
            System.out.println("Exception: " + g);
        }
    }
    
    /*public boolean canConnect() {
        try {
            this.address = InetAddress.getByName(this.hostAddress);
            this.connection = new Socket(this.address, this.hostPort);
            
            bos = new BufferedOutputStream(this.connection.getOutputStream());
            osw = new OutputStreamWriter(bos, "US-ASCII");
            
            this.send("" + 0);
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        
        try {
            this.connection.close();
        } catch (IOException e) {
            return false;
        }
        
        return true;
    }*/
    
    public boolean openConnection() {
        try {
            this.address = InetAddress.getByName(this.hostAddress);
            this.connection = new Socket(this.address, this.hostPort);
            
            bos = new BufferedOutputStream(this.connection.getOutputStream());
            osw = new OutputStreamWriter(bos, "US-ASCII");
            
            new Reader(connection).start();
            
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
        	osw.close();
        	bos.close();
            this.connection.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
    			int character, sample;
        	
    			while(true){
        		
    				StringBuffer process = new StringBuffer();
    				
					while((character = isr.read()) != 13) { // ASCII character 13 = CR
						if(character == -1){
							return;
						}

					    process.append((char)character);
					}
					sample = Integer.parseInt(process.toString());
					
					if(sample == 0)
						soundDevice.flushFrame(false);
					else
						soundDevice.outputSample(sample);
        		
    			}
    		} catch (IOException e) {
    			// FIXME: ver se funciona
				return;
			}
    	}
    }
}
