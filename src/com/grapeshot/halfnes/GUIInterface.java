/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

/**
 *
 * @author Andrew
 */
public interface GUIInterface extends Runnable{
    public void setFrame(int[] frame, int bgcolor);
    //Frame is now a 256x240 array with NES color numbers from 0-3F
    //plus the state of the 3 color emphasis bits in bits 7,8,9
    public void messageBox(String message);
    public void run();
    public void render();
    
}
