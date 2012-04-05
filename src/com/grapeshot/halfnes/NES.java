package com.grapeshot.halfnes;

import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.mappers.Mapper;
import com.grapeshot.halfnes.network.Client;
import com.grapeshot.halfnes.network.Server;

import java.util.prefs.Preferences;

/**
 *
 * @author Andrew Hoffman
 */
public class NES {

    private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private Mapper mapper;
    private APU apu;
    private CPU cpu;
    private CPURAM cpuram;
    private PPU ppu;
    private ControllerInterface controller1, controller2;
    final public static String VERSION = "0.045";
    private boolean runEmulation, dontSleep = false;
    public long frameStartTime, framecount, frameDoneTime;
    private boolean frameLimiterOn = true;
    private String curRomPath, curRomName;
    private GUIInterface gui;
    private FrameLimiterInterface limiter = new FrameLimiterImpl(this);

    private Server server;
    private Client client;
    private AudioOutInterface soundDevice = new SwingAudioImpl(this, prefs.getInt("sampleRate", 44100));
    private Thread serverThread = null;

    private boolean hostMode = false, clientMode = false;
    private String hostAddress;
    private int hostPort;
    public static final int defaultPort = 18451;  // FIXME: define the default port number in a better place

    public NES() {
        // nothing to do, GUI init moved to startGUI()
    }

    public void startGUI() {
        gui = new GUIImpl(this);
        try{
            java.awt.EventQueue.invokeAndWait(gui);
        }catch(InterruptedException e){
            System.err.println("Could not initialize GUI. Exiting.");
            System.exit(-1);
        }catch(java.lang.reflect.InvocationTargetException f){
            System.err.println(f.getCause().toString());
            //not sure how this could happen (thrown if run method causes exception)
            System.exit(-1);
        }
    }

    public void run(final String romtoload) {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        //set thread priority higher than the interface thread
        curRomPath = romtoload;
        loadROM(romtoload);
        run();
    }

    public void run() {
        while (true) {
            if (runEmulation) {
                frameStartTime = System.nanoTime();
                runframe();
                if (frameLimiterOn && !dontSleep) {
                    limiter.sleep();
                }
                frameDoneTime = System.nanoTime() - frameStartTime;
            } else {
                limiter.sleepFixed();
                if (ppu != null && framecount > 1) {
                    java.awt.EventQueue.invokeLater(render);
                }
            }
        }
    }
    Runnable render = new Runnable() {

        public void run() {
            gui.render();
        }
    };

    public synchronized void runframe() {
        final int scanlinectrfire = 256;
        //the main method sequencing everything that has to happen in the nes each frame
        //loops unrolled a bit to avoid some conditionals every cycle
        //vblank
        //start by setting nmi
        if ((utils.getbit(ppu.ppuregs[0], 7) && framecount > 1)) {
            //^hack, without this Lolo 2 and/or Vegavox NSFs don't work
            cpu.runcycle(241, 9000);
            cpu.nmi();
            // do the nmi but let cpu run ONE extra instruction first
            // (fixes Adventures of Lolo 2, Milon's Secret Castle, Solomon's Key)
        }
        for (int scanline = 241; scanline < 261; ++scanline) {
            //most of vblank period
            cpu.cycle(scanline, scanlinectrfire);
            mapper.notifyscanline(scanline);
            cpu.cycle(scanline, 341);
        }
        //scanline 261 
        //turn off vblank flag
        ppu.ppuregs[2] &= 0x80;
        cpu.cycle(261, 30);
        // turn off sprite 0, sprite overflow flags
        ppu.ppuregs[2] &= 0x9F;
        cpu.cycle(261, scanlinectrfire);
        mapper.notifyscanline(261);
        cpu.cycle(261, (((framecount & 1) == 1) && utils.getbit(ppu.ppuregs[1], 3)) ? 340 : 341);
        //odd frames are shorter by one PPU pixel if rendering is on.

        dontSleep = apu.bufferHasLessThan(1000);
        //if the audio buffer is completely drained, don't sleep for this frame
        //this is to prevent the emulator from getting stuck sleeping too much
        //on a slow system or when the audio buffer runs dry.

        apu.finishframe();
        cpu.modcycles();
        //active drawing time
        for (int scanline = 0; scanline < 240; ++scanline) {
            if (!ppu.drawLine(scanline)) { //returns true if sprite 0 hits
                cpu.cycle(scanline, scanlinectrfire);
                mapper.notifyscanline(scanline);
            } else {
                //it is de sprite zero line
                final int sprite0x = ppu.getspritehit();
                if (sprite0x < scanlinectrfire) {
                    cpu.cycle(scanline, sprite0x);
                    ppu.ppuregs[2] |= 0x40; //sprite 0 hit
                    cpu.cycle(scanline, scanlinectrfire);
                    mapper.notifyscanline(scanline);
                } else {
                    cpu.cycle(scanline, scanlinectrfire);
                    mapper.notifyscanline(scanline);
                    cpu.cycle(scanline, sprite0x);
                    ppu.ppuregs[2] |= 0x40; //sprite 0 hit
                }
            }
            //and finish out the scanline
            cpu.cycle(scanline, 341);
        }
        //scanline 240: dummy fetches
        cpu.cycle(240, scanlinectrfire);
        mapper.notifyscanline(240);
        cpu.cycle(240, 341);
        //set the vblank flag
        ppu.ppuregs[2] |= 0x80;
        //render the frame
        if(hostMode){
            // NB TODO: change this to use the new Server 
            this.server.sendVideoFrame(ppu.getBitmap(), ppu.bgcolor, this.getFrameTime());
            // gui.setBitmap(ppu.getBitmap(), ppu.bgcolor);
            // gui.getSecondScreen().sendNewFrame();
        }
        ppu.renderFrame(gui);
        if ((framecount & 2047) == 0) {
            //save sram every 30 seconds or so
            saveSRAM(true);
        }
        ++framecount;
    }

    public void setControllers(ControllerInterface controller1, ControllerInterface controller2) {
        this.controller1 = controller1;
        this.controller2 = controller2;
    }

    public void toggleFrameLimiter() {
        if (frameLimiterOn) {
            frameLimiterOn = false;
        } else {
            frameLimiterOn = true;
        }
    }

    public synchronized void loadROM(final String filename) {
        runEmulation = false;
        if (!FileUtils.exists(filename) || !FileUtils.getExtension(filename).equalsIgnoreCase(".nes")) {
            gui.messageBox("Could not load file:\nFile " + filename + "\ndoes not exist or is not a valid NES game.");
            return;
        }
        Mapper newmapper;
        try {
            if (FileUtils.getExtension(filename).equalsIgnoreCase(".nes")) {
                final ROMLoader loader = new ROMLoader(filename);
                loader.parseInesheader();
                newmapper = Mapper.getCorrectMapper(loader.mappertype);
                newmapper.setLoader(loader);
                newmapper.loadrom();
            } else {
                throw new BadMapperException("ROM is not a valid NES game");
            }
        } catch (BadMapperException e) {
            gui.messageBox("Error Loading File: ROM is"
                    + " corrupted or uses an unsupported mapper.\n" + e.getMessage());
            return;
        }
        if (apu != null) {
            //if rom already running save its sram before closing
            apu.destroy();
            saveSRAM(false);
            //also get rid of mapper etc.
            mapper.destroy();
            cpu = null;
            cpuram = null;
            ppu = null;
        }
        mapper = newmapper;
        //now some annoying getting of all the references where they belong
        cpuram = mapper.getCPURAM();
        cpu = mapper.cpu;
        ppu = mapper.ppu;
        apu = new APU(this, cpu, cpuram, soundDevice);
        cpuram.setAPU(apu);
        cpuram.setPPU(ppu);
        curRomPath = filename;
        curRomName = FileUtils.getFilenamefromPath(filename);

        framecount = 0;
        //if savestate exists, load it
        if (mapper.hasSRAM()) {
            loadSRAM();
        }
        //and start emulation
        cpu.init();
        runEmulation = true;

        if(this.hostMode)
            this.server.sendTitle(curRomName);
    }

    private void saveSRAM(final boolean async) {
        if (mapper != null && mapper.hasSRAM() && mapper.supportsSaves()) {
            if (async) {
                FileUtils.asyncwritetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            } else {
                FileUtils.writetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            }
        }
    }

    private void loadSRAM() {
        final String name = FileUtils.stripExtension(curRomPath) + ".sav";
        if (FileUtils.exists(name) && mapper.supportsSaves()) {
            mapper.setPRGRAM(FileUtils.readfromfile(name));
        }

    }

    public void quit() {
        //save SRAM and quit
        if (cpu != null && curRomPath != null) {
            runEmulation = false;
            saveSRAM(false);
        }
        System.exit(0);
    }

    public synchronized void reset() {
        if (cpu != null) {
            cpu.reset();
            runEmulation = true;
            apu.pause();
            apu.resume();
        }
        //reset frame counter as well because PPU is reset
        //on Famicom, PPU is not reset when Reset is pressed
        //but some NES games expect it to be and you get garbage.
        framecount = 0;
    }

    public synchronized void reloadROM() {
        loadROM(curRomPath);
    }

    public synchronized void pause() {
        if (apu != null) {
            apu.pause();
        }
        runEmulation = false;
    }

    public long getFrameTime() {
        return frameDoneTime;
    }

    public void setFrameTime(long frametime) {
        frameDoneTime = frametime;
    }

    public String getrominfo() {
        if (mapper != null) {
            return mapper.getrominfo();
        }
        return null;
    }

    public Preferences getPrefs() {
        return prefs;
    }

    public synchronized void frameAdvance() {
        runEmulation = false;
        if (cpu != null) {
            runframe();
        }
    }

    public synchronized void resume() {
        if (apu != null) {
            apu.resume();
        }
        if (cpu != null) {
            runEmulation = true;
        }
    }

    public String getCurrentRomName() {
        return curRomName;
    }

    public void setCurrentRomName(String romName) {
        curRomName = romName;
    }

    public boolean isFrameLimiterOn() {
        return frameLimiterOn;
    }

    public void messageBox(final String string) {
        gui.messageBox(string);
    }

    public ControllerInterface getcontroller1() {
        return controller1;
    }

    public ControllerInterface getcontroller2() {
        return controller2;
    }



    public void setHostMode(boolean hostMode) {
        this.hostMode = hostMode;
        this.hostPort = defaultPort;
    }

    public void setHostMode(boolean hostMode, int hostPort) {
        this.hostMode = hostMode;
        this.setHostPort(hostPort);
    }

    public boolean setHostMode(int port){

        this.server = new Server(port, this);
        if(!server.canBind()){
            this.server = null;
            //this.hostMode = false;
        }else{
            this.hostMode = true;
            soundDevice.setServer(this.server);
        }

        serverThread = new Thread(this.server);
        serverThread.start();

        return this.hostMode == true;
    }

    public Server getServer(){
        return this.server;
    }

    public void networkDisable(){

        //TODO terminate server or client

        if(this.hostMode) {
            this.server.closeSocket();
            this.serverThread.interrupt();
            this.serverThread = null;
        }

        soundDevice.setServer(null);

        this.server = null;
        this.client = null;

        this.hostMode = false;
        this.clientMode = false;
    }

    public boolean getHostMode() {
        return this.hostMode;
    }

    public void setHostPort(int port) {
        this.hostPort = port;
        // TODO: enforce a port number between 10000 and 65535. Possibly check if port is bindable.
    }

    public int getHostPort() {
        return this.hostPort;
    }

    public void setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
    }

    public void setClientMode(boolean clientMode, String hostAddress) {
        this.clientMode = clientMode;
        this.setHostAddress(hostAddress);
    }

    public boolean setClientMode(String hostAddress, int hostPort){

        this.client = new Client(hostAddress, hostPort, this);
        if(!client.openConnection()) {
            this.client = null;
            //this.clientMode = false;
        }else
            this.clientMode = true;

        return this.clientMode == true;
    }

    public Client getClient(){
        return this.client;
    }

    public boolean getClientMode() {
        return this.clientMode;
    }

    public void setHostAddress(String hostAddress) {
        int portStartIndex = hostAddress.lastIndexOf(":"); 
        if(portStartIndex != -1) {
            this.hostAddress = hostAddress.substring(0, portStartIndex);
            this.setHostPort(Integer.parseInt(hostAddress.substring(portStartIndex+1)));
        } else {
            this.hostAddress = hostAddress;   
            this.hostPort = defaultPort;
        }
    }

    public String getHostAddress() {
        return this.hostAddress;
    }

    public boolean isNetworkActive() {
        return (this.hostMode || this.clientMode);
    }

    public AudioOutInterface getSoundDevice() {
        return this.soundDevice;
    }

    public GUIInterface getGUI() {
        return this.gui;
    }

    public ControllerInterfaceHost getController2() {
        if(this.controller2 instanceof ControllerImplHost) {
            return (ControllerInterfaceHost) this.controller2;
        } else {
            return null;
        }
    }
}