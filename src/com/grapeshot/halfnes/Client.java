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
    
    public Client(String hostAddress, int hostPort) {
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
    }
    
    public void sendControllerbyte(int byteVal) {
        this.openConnection();        
        this.send("" + byteVal);        
        this.closeConnection();
    }
    
    private void send(String msg) {
        StringBuffer instr = new StringBuffer();
        
        try {
            BufferedOutputStream bos = new BufferedOutputStream(this.connection.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos, "US-ASCII");
            String process = "" + msg + (char) 13;  // using ASCII char 13 (aka CR) as terminating char
            osw.write(process);
            osw.flush();

            BufferedInputStream bis = new BufferedInputStream(this.connection.getInputStream());
            InputStreamReader isr = new InputStreamReader(bis, "US-ASCII");
            int c;
            while((c = isr.read()) != 13) {
                instr.append((char) c);
            }

            System.out.println(instr);
        }
        catch (IOException f) {
            System.out.println("IOException: " + f);
        }
        catch (Exception g) {
            System.out.println("Exception: " + g);
        }
    }
    
    public boolean canConnect() {
        try {
            this.address = InetAddress.getByName(this.hostAddress);
            this.connection = new Socket(this.address, this.hostPort);
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
