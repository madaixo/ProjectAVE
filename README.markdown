 HalfNES with Network Multiplayer
==================================

This project aims to implement network based multiplayer in the HalfNES project. The work being done as part of a project for our university.

The first import was based on revision 154 of the official SVN repository, located at: http://code.google.com/p/halfnes


HalfNES' official Readme file
-----------------------------

HalfNES by Andrew Hoffman

Released as free software under GPL V3 (See license.txt for details). 

Current Features:

-   Joystick support through both DirectInput and xInput (thanks Zlika) 
-   Cross-Platform
-   Supports Mapper 0, 1, 2, 3, 4, 5, 7, 9, 11, 21, 22, 23, 24, 25, 26, 34, 66, 68, 69, 71, 78, 87, 118, 226 
-   Accurate sound core
-   Fast display code
-   Battery save support (No savestates! Come on. You can live without them.)
-   Remappable controls!
-   Full screen mode 

Default Controls (See Preferences dialog to remap them, need to reload 
the ROM for changes to apply): 

Controller 1:

-   D-Pad: Arrow Keys
-   B Button: Z
-   A Button: X
-   Select: Right Shift
-   Start: Enter 

Controller 2:

-   D-Pad: WASD
-   B Button: F
-   A Button: G
-   Select: R
-   Start: T 

Note on joystick support: 

HalfNES must be launched using the "run.bat" file for joysticks to work!
The lib folder must also be in the same location as halfnes.jar is.
The first detected gamepad will be used as Controller 1, and the second 
will be Controller 2. Currently the buttons used are not configurable. 

Please keep in mind that this is an alpha release. At this point in 
development, almost all US released games will start, but certain games 
still have graphics corruption or freezing problems. Please report any 
issues you encounter with the emulator or with games on the Google Code 
page (http://code.google.com/p/halfnes/). 

Do NOT ask me where to find ROM files of commercial games. Some public 
domain homebrew ROMs are available at www.pdroms.de for testing 
purposes. 

A 2 ghz Athlon 64 or better is currently required to run all games full 
speed. (The NTSC filter requires MUCH more processing power, however.)
Saved games are placed in the folder that the ROM file is in for 
now. 

If you are having problems getting the emulator to run, make sure to 
update your Java Runtime to the latest version. Go to 
http://java.com/en/download/manual.jsp and get the correct version for 
your OS. 

Special Thanks to the NESDev wiki and forum community for the invaluable 
NES hardware reference that made this project possible. 


