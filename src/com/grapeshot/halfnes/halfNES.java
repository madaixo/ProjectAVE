package com.grapeshot.halfnes;

//HalfNES, Copyright Andrew Hoffman, October 2010
import java.io.*;
import javax.swing.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class halfNES {

    // private static final long serialVersionUID = -7269569171056445433L;

    public static void main(String[] args) throws IOException {
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception e){
            // ignore
        }
        NES thing = new NES();
        
        // use Apache Commons CLI to parse command line options
        Options options = new Options();
        options.addOption("H", "host", false, "The emulator runs in host mode for network multiplayer");
        options.addOption("C", "client", true, "The emulator runs in client mode for network multiplayer. Receives the IP address of the host as argument.");
        // TODO: use the Option class to specify options, it allows for finer grained control such as saying host takes an optional argument (the port).
        
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLineParser parser = new GnuParser();
        try {
            CommandLine cli = parser.parse(options, args);

            if(cli.hasOption("host")) {
                thing.setHostMode(true);
            } else if(cli.hasOption("client")) {
                String hostIP = cli.getOptionValue("client");
                thing.setClientMode(true, hostIP);
            } 
            
            thing.startGUI();
            
            String[] unknownArgs = cli.getArgs();
            if(unknownArgs.length < 1) {
                thing.run();
            } else {
                thing.run(unknownArgs[0]);  // assume the first unknown argument is the ROM path
            }
        }
        catch(ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            formatter.printHelp("halfnes", options);	// FIXME: the first argument is probably not right
        }

    }
}
