package com.grapeshot.halfnes;

//HalfNES, Copyright Andrew Hoffman, October 2010
import java.io.*;
import javax.swing.*;

public class halfNES {

    public static void main(String[] args) throws IOException {
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception e){
            // ignore
        }
        NES thing = new NES();
        
        if (args == null || args.length < 1 || args[0] == null) {
            thing.run();
        } else {
            thing.run(args[0]);
        }
    }
}
