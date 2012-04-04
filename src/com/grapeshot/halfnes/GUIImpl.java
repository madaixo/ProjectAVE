package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.*;

public class GUIImpl extends JFrame implements GUIInterface {

    private Canvas canvas;
    private BufferStrategy buffer;
    private final NES nes;
    private static final long serialVersionUID = 6411494245530679723L;
    private FileDialog fileDialog;
    //now using the AWT file dialog because apparently Apple didn't bother
    //to build a decent jFileChooser for the Mac JVM.
    private final AL listener = new AL();
    private int screenScaleFactor;
    private long[] frametimes = new long[60];
    private int frametimeptr = 0;
    private boolean smoothScale, inFullScreen = false;
    private GraphicsDevice gd;
    final int NES_HEIGHT = 224, NES_WIDTH;
    private Renderer renderer;
    private ControllerInterface padController1, padController2;
    public int[] bitmap;
    public int color;
    public ImageClient mpImage;
    private final ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();

    private JMenu networkMenu = null;

    public GUIImpl(NES nes) {
        this.nes = nes;
        screenScaleFactor = nes.getPrefs().getInt("screenScaling", 2);
        /*if(nes.getHostMode()) {
            padController1 = new ControllerImpl(this, nes.getPrefs(), 0);
            padController2 = new ControllerImplHost(nes.getHostPort());
        } else if(nes.getClientMode()) {
            padController1 = new ControllerFake();
            padController2 = new ControllerImplClient(this, nes.getPrefs(), 1, nes.getHostAddress(), nes.getHostPort());
        } else {*/
            padController1 = new ControllerImpl(this, nes.getPrefs(), 0);
            padController2 = new ControllerImpl(this, nes.getPrefs(), 1);
        //}
        // FIXME: the implementation above might make it hard to graciously fallback in case of connection problems
        nes.setControllers(padController1, padController2);
        padController1.startEventQueue();
        padController2.startEventQueue();

        if (nes.getPrefs().getBoolean("TVEmulation", false)) {
            renderer = new NTSCRenderer();
            NES_WIDTH = 302;
        } else {
            renderer = new RGBRenderer();
            NES_WIDTH = 256;
        }
        smoothScale = nes.getPrefs().getBoolean("smoothScaling", false);
        fileDialog = new FileDialog(this);
        fileDialog.setMode(FileDialog.LOAD);
        fileDialog.setTitle("Select a ROM to load");
        //should open last folder used, and if that doesn't exist, the folder it's running in
        final String path = nes.getPrefs().get("filePath", System.getProperty("user.dir", ""));
        final File startDirectory = new File(path);
        if (startDirectory.isDirectory()) {
            fileDialog.setDirectory(path);
        }
        //and if the last path used doesn't exist don't set the directory at all
        //and hopefully the jFileChooser will open somewhere usable
        //on Windows it does - on Mac probably not.
        fileDialog.setFilenameFilter(new NESFileFilter());
    }

    // TODO: meh
    public void setBitmap(int[] bit, int color){
        this.bitmap = bit;
        this.color = color;
    }

    public ImageClient getSecondScreen(){
        return mpImage;
    }


    public synchronized void run() {
        //construct window
        this.setTitle("HalfNES " + NES.VERSION);
        this.setResizable(false);
        buildMenus();
        this.getRootPane().registerKeyboardAction(listener, "Escape",
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.setLocation(nes.getPrefs().getInt("windowX", 0), nes.getPrefs().getInt("windowY", 0));
        this.addWindowListener(listener);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        screenScaleFactor = nes.getPrefs().getInt("screenScaling", 2);
        // Create canvas for painting...
        canvas = new Canvas();
        canvas.setSize(NES_WIDTH * screenScaleFactor, NES_HEIGHT * screenScaleFactor);
        canvas.setEnabled(false); //otherwise it steals input events.

        // Add canvas to game window...
        this.add(canvas);
        this.pack();
        this.setVisible(true);
        // Create BackBuffer...
        canvas.createBufferStrategy(2);
        buffer = canvas.getBufferStrategy();
        //now add the drag and drop handler.
        TransferHandler handler = new TransferHandler() {

            @Override
            public boolean canImport(final TransferHandler.TransferSupport support) {
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }

                return true;
            }

            @Override
            public boolean importData(final TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                Transferable t = support.getTransferable();
                try {
                    //holy typecasting batman (this interface predates generics)
                    File toload = (File) ((java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor)).get(0);
                    nes.loadROM(toload.getCanonicalPath());
                } catch (UnsupportedFlavorException e) {
                    return false;
                } catch (IOException e) {
                    return false;
                }
                return true;
            }
        };
        this.setTransferHandler(handler);
    }

    public void buildMenus() {
        JMenuBar menus = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem item;
        file.add(item = new JMenuItem("Open ROM..."));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        file.addSeparator();

        file.add(item = new JMenuItem("Preferences..."));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        file.addSeparator();

        file.add(item = new JMenuItem("Toggle Fullscreen"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        item.addActionListener(listener);
        menus.add(file);

        file.add(item = new JMenuItem("Quit"));
        item.addActionListener(listener);
        menus.add(file);

        JMenu nesmenu = new JMenu("NES");
        nesmenu.add(item = new JMenuItem("Reset"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        nesmenu.add(item = new JMenuItem("Hard Reset"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        nesmenu.add(item = new JMenuItem("Pause"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));

        nesmenu.add(item = new JMenuItem("Resume"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));

        nesmenu.add(item = new JMenuItem("Fast Forward"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        nesmenu.add(item = new JMenuItem("Frame Advance"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        nesmenu.addSeparator();

        nesmenu.add(item = new JMenuItem("ROM Info"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        menus.add(nesmenu);
        
        JMenu multiplayer = new JMenu("Network MP");
        // if you change the menu items don't forget to change refreshMultiplayerMenu()
        
        multiplayer.add(item = new JMenuItem("Disable"));
        item.addActionListener(listener);
        
        multiplayer.addSeparator();
        
        multiplayer.add(item = new JMenuItem("Run as Host"));
        item.addActionListener(listener);

        multiplayer.add(item = new JMenuItem("Connect to a Host"));
        item.addActionListener(listener);

        this.networkMenu = multiplayer;
        this.refreshMultiplayerMenu();
        menus.add(multiplayer);

        JMenu help = new JMenu("Help");
        help.add(item = new JMenuItem("About"));
        item.addActionListener(listener);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menus.add(help);
        this.setJMenuBar(menus);
    }

    public void loadROM() {
        renderer = (nes.getPrefs().getBoolean("TVEmulation", false)) ? new NTSCRenderer() : new RGBRenderer();
        boolean wasInFullScreen = false;
        if (inFullScreen) {
            wasInFullScreen = true;
            //load dialog won't show if we are in full screen, so this fixes for now.
            toggleFullScreen();
        }
        fileDialog.setVisible(true);
        if (fileDialog.getFile() != null) {
            nes.loadROM(fileDialog.getDirectory() + fileDialog.getFile());
            nes.getPrefs().put("filePath", fileDialog.getDirectory());
        }
        if (wasInFullScreen) {
            toggleFullScreen();
        }
    }

    public synchronized void toggleFullScreen() {
        if (inFullScreen) {
            this.dispose();
            gd.setFullScreenWindow(null);
            canvas.setSize(NES_HEIGHT * screenScaleFactor, NES_WIDTH * screenScaleFactor);
            this.setUndecorated(false);
            this.setVisible(true);
            inFullScreen = false;
            buildMenus();
            // nes.resume();
        } else {
            setMenuBar(null);
            gd = getGraphicsConfiguration().getDevice();
            if (!gd.isFullScreenSupported()) {
                //then fullscreen will give a window the size of the screen instead
                messageBox("Fullscreen is not supported by your OS or version of Java.");
            }
            this.dispose();
            this.setUndecorated(true);

            gd.setFullScreenWindow(this);
            this.setVisible(true);

            inFullScreen = true;
        }
    }

    public void messageBox(final String message) {
        JOptionPane.showMessageDialog(this, message);
    }
    int bgcolor;
    BufferedImage frame;
    double fps;
    int frameskip = 0;

    @Override
    public final synchronized void setFrame(final int[] nextframe, final int bgcolor) {
        //todo: stop running video filters while paused!
        //also move video filters into a worker thread because they
        //don't really depend on emulation state at all. Yes this is going to
        //cause more lag but it will hopefully get back up to playable speed with NTSC filter
        /*
        // TODO: needs some tweaking in order to show the framerate on both server and client
        frametimes[frametimeptr] = nes.getFrameTime();
        ++frametimeptr;
        frametimeptr %= frametimes.length;

        if (frametimeptr == 0) {
            long averageframes = 0;
            for (long l : frametimes) {
                averageframes += l;
            }
            averageframes /= frametimes.length;
            fps = 1E9 / averageframes;
            this.setTitle(String.format("HalfNES %s - %s, %2.2f fps"
                    + ((frameskip > 0) ? " frameskip " + frameskip : ""),
                    NES.VERSION,
                    nes.getCurrentRomName(),
                    fps));
        }
        */
//        if (nes.framecount % (frameskip + 1) == 0) {
        frame = renderer.render(nextframe, bgcolor);
        render();
//        }
    }

    @Override
    public final synchronized void render() {
        final Graphics graphics = buffer.getDrawGraphics();
        if (smoothScale) {
            ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        if (inFullScreen) {
            graphics.setColor(Color.BLACK);
            DisplayMode dm = gd.getDisplayMode();
            int scrnheight = dm.getHeight();
            int scrnwidth = dm.getWidth();
            //don't ask why this needs to be done every frame,
            //but it does b/c the canvas keeps resizing itself
            canvas.setSize(scrnwidth, scrnheight);
            graphics.fillRect(0, 0, scrnwidth, scrnheight);
            if (nes.getPrefs().getBoolean("maintainAspect", false)) {
                int scalefactor = getmaxscale(scrnwidth, scrnheight);
                int height = NES_HEIGHT * scalefactor;
                int width = (int) (256 * scalefactor * 1.1666667);
                graphics.drawImage(frame, ((scrnwidth / 2) - (width / 2)),
                        ((scrnheight / 2) - (height / 2)),
                        width,
                        height,
                        null);
            } else {
                graphics.drawImage(frame, 0, 0,
                        scrnwidth,
                        scrnheight,
                        null);
            }
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawString(this.getTitle(), 16, 16);

        } else {
            graphics.drawImage(frame, 0, 0, NES_WIDTH * screenScaleFactor, NES_HEIGHT * screenScaleFactor, null);
        }

        graphics.dispose();
        buffer.show();

    }

    public void showoptdlg() {
        final PreferencesDialog dialog = new PreferencesDialog(this, nes.getPrefs(), NES.defaultPort);
        dialog.setVisible(true);
    }

    public void savewindowposition() {
        nes.getPrefs().putInt("windowX", this.getX());
        nes.getPrefs().putInt("windowY", this.getY());
    }

    private int getmaxscale(final int width, final int height) {
        return Math.min(height / NES_HEIGHT, width / NES_WIDTH);
    }
    
    public GUIImpl getThis(){
    	return this;
    }
    
    public void refreshMultiplayerMenu() {
        JMenuItem disableItem = this.networkMenu.getItem(0);
        JMenuItem hostItem = this.networkMenu.getItem(2);   // pos = 1 is the separator
        JMenuItem clientItem = this.networkMenu.getItem(3);
        
        if(!nes.isNetworkActive()) {
            disableItem.setEnabled(false);
            hostItem.setEnabled(true);
            clientItem.setEnabled(true);
        } else {
            disableItem.setEnabled(true);
            hostItem.setEnabled(false);
            clientItem.setEnabled(false);
        }
    }

    public class AL implements ActionListener, WindowListener {

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            // placeholder for more robust handler
            if (arg0.getActionCommand().equals("Quit")) {
                nes.quit();
            } else if (arg0.getActionCommand().equals("Reset")) {
                nes.reset();
            } else if (arg0.getActionCommand().equals("Hard Reset")) {
                nes.reloadROM();
            } else if (arg0.getActionCommand().equals("Pause")) {
                nes.pause();
            } else if (arg0.getActionCommand().equals("Resume")) {
                nes.resume();
            } else if (arg0.getActionCommand().equals("Preferences...")) {
                showoptdlg();
            } else if (arg0.getActionCommand().equals("Fast Forward")) {
                nes.toggleFrameLimiter();
            } else if (arg0.getActionCommand().equals("About")) {
                messageBox("HalfNES " + NES.VERSION + " by Andrew Hoffman \n"
                        + "\n"
                        + "Get the latest version and report any bugs at http://code.google.com/p/halfnes \n"
                        + "\n"
                        + "This program is free software licensed under the GPL version 3, and comes with \n"
                        + "NO WARRANTY of any kind. (but if something's broken, please let me know). \n"
                        + "See the license.txt file for details.");
            } else if (arg0.getActionCommand().equals("ROM Info")) {
                String info = nes.getrominfo();
                if (info != null) {
                    messageBox(info);
                }
            } else if (arg0.getActionCommand().equals("Open ROM...")) {
                loadROM();
            } else if (arg0.getActionCommand().equals("Toggle Fullscreen")) {
                toggleFullScreen();
            } else if (arg0.getActionCommand().equals("Frame Advance")) {
                nes.frameAdvance();
            } else if (arg0.getActionCommand().equals("Escape")) {
                if (inFullScreen) {
                    toggleFullScreen();
                } else {
                    nes.quit();
                }
            } else if(arg0.getActionCommand().equals("Disable")) {
                
            	nes.networkDisable();
            	
                padController1.stopEventQueue();
                padController2.stopEventQueue();
                
                padController1 = new ControllerImpl(getThis(), nes.getPrefs(), 0);
                padController2 = new ControllerImpl(getThis(), nes.getPrefs(), 1);
            
                nes.setControllers(padController1, padController2);
                padController1.startEventQueue();
                padController2.startEventQueue();
                
                refreshMultiplayerMenu();
            } else if(arg0.getActionCommand().equals("Run as Host")) {
                        
                // FIXME: this should be moved to NES.class
                /*Server server = new Server(currentPort);
                if(!server.canBind()) {
                	JOptionPane.showMessageDialog(getThis(), "Unable to use port "+currentPort, "Run as Host", JOptionPane.ERROR_MESSAGE);
                    return;
                }*/
            	
            	String port = String.valueOf(nes.getPrefs().getInt("HostPort", NES.defaultPort));
            	
            	do{
            		port = JOptionPane.showInputDialog(getThis(), "Select a port", port);
            		
            		if(port != null){
            			// TODO: check if valid port
            			int intport = Integer.parseInt(port);
            			
            			if(!nes.setHostMode(intport)){
                        	JOptionPane.showMessageDialog(getThis(), "Unable to use port "+port, "Run as Host", JOptionPane.ERROR_MESSAGE);
                        	continue;
                        }
            			
            			padController1.stopEventQueue();
                        padController2.stopEventQueue();

                        //nes.setHostMode(true, currentPort);

                        padController1 = new ControllerImpl(getThis(), nes.getPrefs(), 0);
                        padController2 = new ControllerImplHost(nes.getServer());
                        mpImage = new ImageClient(getThis());

                        nes.setControllers(padController1, padController2);
                        padController1.startEventQueue();
                        padController2.startEventQueue();
            			
                        break;
            		}
            	}while(port != null);
                
                refreshMultiplayerMenu();
            } else if(arg0.getActionCommand().equals("Connect to a Host")) {
                
                String hostAddress = null;
                String currentAddress = "127.0.0.1:"+NES.defaultPort;
                String host;
            	int port;
                
                do {
                    hostAddress = (String) JOptionPane.showInputDialog(getThis(), "Enter the host's address:", "Connect to a Host", JOptionPane.QUESTION_MESSAGE, null, null, currentAddress);
                    
                    if(hostAddress != null) {
                        
                        int portStartIndex = hostAddress.lastIndexOf(":"); 

                        if(portStartIndex != -1) {
                            host = hostAddress.substring(0, portStartIndex);
                            port = Integer.parseInt(hostAddress.substring(portStartIndex+1));
                        } else {
                            host = hostAddress;   
                            port = NES.defaultPort;
                        }
                        
                        if(!nes.setClientMode(host, port)){
                            JOptionPane.showMessageDialog(getThis(), "Unable to connect to "+hostAddress, "Connect to a Host", JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                        
                        /*nes.setClientMode(true, hostAddress);
                        
                        // FIXME: this should be moved to NES.class
                        Client client = new Client(nes.getHostAddress(), nes.getHostPort());
                        if(!client.canConnect()) {
                            JOptionPane.showMessageDialog(getThis(), "Unable to connect to "+hostAddress, "Connect to a Host", JOptionPane.ERROR_MESSAGE);
                            nes.setClientMode(false);
                            continue;
                        }*/
                        
                        /* Configure controllers */
                        
                        padController1.stopEventQueue();
                        padController2.stopEventQueue();
        
                        padController1 = new ControllerFake();
                        padController2 = new ControllerImplClient(getThis(), nes.getPrefs(), 1, nes.getClient());
                        thread.execute(new ImageServer(getThis()));

                        nes.setControllers(padController1, padController2);
                        padController1.startEventQueue();
                        padController2.startEventQueue();
                    }
                    break;
                } while(true);
               
                refreshMultiplayerMenu();
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
        }

        @Override
        public void windowClosing(WindowEvent e) {
            savewindowposition();
            padController1.stopEventQueue();
            padController2.stopEventQueue();
            nes.quit();

        }

        @Override
        public void windowClosed(WindowEvent e) {
            //we don't care about these events
        }

        @Override
        public void windowIconified(WindowEvent e) {
            //but java wants us to implement something for all of them
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            //so we can use the interface.
        }

        @Override
        public void windowActivated(WindowEvent e) {
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
        }
    }
}
