package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.mappers.Mapper;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class PPU {

    public Mapper mapper;
    private int scanline, oamaddr, sprite0x, readbuffer = 0;
    private int loopyV = 0x0;//ppu memory pointer
    private int loopyT = 0x0;//temp pointer
    private int loopyX = 0;//fine x scroll
    private final int[] OAM = new int[256], spriteshiftregH = new int[8],
            spriteshiftregL = new int[8], spriteXlatch = new int[8],
            spritepals = new int[8], bitmap = new int[240 * 256];
    private final boolean[] spritebgflags = new boolean[8];
    private boolean sprite0hit = false, even = true, bgpattern = true, sprpattern = false;
    public final int[] ppuregs = new int[0x8], pal = new int[0x20];
    private DebugUI debuggui;
    private int vraminc = 1;
    private final static boolean PPUDEBUG = false;
    private BufferedImage newBuff;

    public PPU(final Mapper mapper) {
        this.mapper = mapper;
        Arrays.fill(OAM, 0xff);
        Arrays.fill(ppuregs, 0x00);
        Arrays.fill(pal, 32);
        if (PPUDEBUG) {
            newBuff = new BufferedImage(512, 480, BufferedImage.TYPE_INT_BGR);
            debuggui = new DebugUI();
            debuggui.run();
        }
    }

    public final int read(final int regnum) {
        switch (regnum) {
            case 2:
                even = true;
                final int tmp = ppuregs[2];
                ppuregs[2] &= 0x7f;//turn off vblank flag
                return tmp;
            case 4:
                // reading this is NOT reliable but some games do it anyways
                return OAM[oamaddr];
            case 7:
                // PPUDATA
                // correct behavior. read is delayed by one
                // -unless- is a read from sprite pallettes
                if ((loopyV & 0x3fff) < 0x3f00) {
                    final int temp = readbuffer;
                    readbuffer = mapper.ppuRead(loopyV & 0x3fff);
                    loopyV += vraminc;
                    return temp;
                } else {
                    readbuffer = mapper.ppuRead((loopyV & 0x3fff) - 0x1000);
                    final int temp = mapper.ppuRead(loopyV);
                    loopyV += vraminc;
                    return temp;
                }

            // and don't increment on read
            default:
                return 0x20; // open bus
        }
    }

    public final void write(final int regnum, final int data) {
        //System.err.println("PPU write - wrote " + data + " to reg " + regnum);
        //debugdraw();
        switch (regnum) {
            case 0:
                ppuregs[0] = data;
                vraminc = (utils.getbit(data, 2) ? 32 : 1);
                //set 2 bits of vram address (nametable select)
                loopyT &= ~0xc00;
                loopyT += (data & 3) << 10;
                break;
            case 1:
                ppuregs[1] = data;
            case 3:
                // PPUOAMADDR (2003)
                // most games just write zero and use the dma
                oamaddr = data & 0xff;
                break;
            case 4:
                // PPUOAMDATA(2004)
                OAM[oamaddr++] = data;
                oamaddr &= 0xff;
                // games don't write this directly anyway
                break;

            // PPUSCROLL(2005)
            case 5:
                if (even) {
                    // horizontal scroll
                    loopyT &= ~0x1f;
                    loopyX = data & 7;
                    loopyT += data >> 3;

                    even = false;
                } else {
                    // vertical scroll
                    loopyT &= ~0x7000;
                    loopyT |= ((data & 7) << 12);
                    loopyT &= ~0x3e0;
                    loopyT |= (data & 0xf8) << 2;
                    even = true;

                }
                break;

            case 6:
                // PPUADDR (2006)
                if (even) {
                    // high byte
                    loopyT &= 0xc0ff;
                    loopyT += ((data & 0x3f) << 8);
                    loopyT &= 0x3fff;
                    even = false;
                } else {
                    loopyT &= 0xff00;
                    loopyT += data;
                    loopyV = loopyT;
                    even = true;

                }
                break;
            case 7:
                // PPUDATA
                if (renderingisoff()) {
                    // if rendering is off its safe to write
                    mapper.ppuWrite((loopyV & 0x3fff), data);
                    loopyV += vraminc;
                } else {
                    //System.err.println("dropped write");
                    // write anyway though until i figure out which wrong thing to do
                    //also since the ppu doesn't get pixel level timing right now,
                    //i can't detect hblank.
                    mapper.ppuWrite((loopyV & 0x3fff), data);
                    loopyV += vraminc;
                }
            // increments on write but NOT on read
            default:
                break;
        }
    }

    public final boolean renderingisoff() {
        // tells when it's ok to write to the ppu
        return (scanline >= 240) //|| (pixel > 256)
                || (!utils.getbit(ppuregs[1], 3));
    }

    public final boolean mmc3CounterClocking() {
        return (bgpattern != sprpattern) && !renderingisoff();
    }
    int bgcolor;

    public final boolean drawLine(final int scanline) {
        //System.err.print("SCANLINE " + scanline);
        //this contains probably more magic numbers than the rest of the program combined.
        //TODO: define some static bitmasks to manipulate the address through, instead
        bgpattern = utils.getbit(ppuregs[0], 4);
        sprpattern = utils.getbit(ppuregs[0], 3);
        final int bufferoffset = scanline * 256;
        //TODO: Simplify Logic
        bgcolor = pal[0] + 256; //plus 256 is to give indication it IS the bgcolor
        //because bg color is special
        if (utils.getbit(ppuregs[1], 3)) {
            //System.err.println(" BG ON!");
            // if bg is on, draw tiles.
            if (scanline == 0) {
                //update whole scroll
                loopyV = loopyT;
            } else {
                //update horizontal scroll bits only
                loopyV &= ~0x41f;
                loopyV |= loopyT & 0x41f;
            }
            //draw background
            int ntoffset = (loopyV & 0xc00) | 0x2000;
            int attroffset = (loopyV & 0xc00) | 0x2000 + 0x3c0;
            boolean horizWrap = false;
            for (int tilenum = 0; tilenum < 33; ++tilenum) {
                //for each tile in row
                if ((tilenum * 8 + (((loopyV & 0x1f) << 3) + loopyX)) > 255 && !horizWrap) {
                    //if scrolling off the side of the nametable, bump address to next nametable
                    ntoffset ^= 0x400;
                    ntoffset -= 32;
                    attroffset ^= 0x400;
                    horizWrap = true;
                }
                //get palette number from attribute table byte
                final int tileaddr = mapper.ppuRead(ntoffset + (loopyV & 0x3ff) + tilenum) * 16 + (bgpattern ? 0x1000 : 0);
                final int palettenum = getattrtbl(attroffset, (ntoffset + loopyV + tilenum) & 0x1f, (((ntoffset + loopyV + tilenum) & 0x3e0) >> 5));
                final int[] tile = getTile(tileaddr, palettenum * 4, (loopyV & 0x7000) >> 12);
                //now put inna buffer
                final int xpos = tilenum * 8 - loopyX;//not quite right yet
                for (int pxl = 0; pxl < 8; ++pxl) {
                    if ((pxl + xpos) < 256 && (pxl + xpos) >= 0) { //it's not off the screen
                        bitmap[pxl + xpos + bufferoffset] = tile[pxl];
                    }
                }
            }
            //increment loopy_v to next row of tiles
            int newfinescroll = loopyV & 0x7000;
            newfinescroll += 0x1000;
            loopyV &= ~0x7000;
            if (newfinescroll > 0x7000) {
                //reset the fine scroll bits and increment tile address to next row
                loopyV += 32;
            } else {
                //increment the fine scroll
                loopyV += newfinescroll;
            }
            if (((loopyV >> 5) & 0x1f) == 30) {
                //if incrementing loopy_v to the next row pushes us into the next
                //nametable, zero the "row" bits and go to next nametable
                loopyV &= ~0x3e0;
                loopyV ^= 0x800;
                ntoffset += 0x440;
                attroffset += 0x7c0;
            }
            //hide leftmost 8 pixels if that flag is on
            if (!utils.getbit(ppuregs[1], 1)) {
                for (int i = 0; i < 8; ++i) {
                    bitmap[i + bufferoffset] = bgcolor;
                }
            }
        } else {
            //System.err.println(" BG off");
            //if rendering is off draw either the background color OR
            //if the PPU address points to the palette, draw that color instead.
            bgcolor = ((loopyV > 0x3f00 && loopyV < 0x3fff) ? mapper.ppuRead(loopyV) : pal[0]);
            Arrays.fill(bitmap, bufferoffset, bufferoffset + 256, bgcolor);
        }
        //draw sprites on top of whatever we had
        drawSprites(scanline);
        //deal with the grayscale flag
        if (utils.getbit(ppuregs[1], 0)) {
            for (int i = bufferoffset; i < (bufferoffset + 256); ++i) {
                bitmap[i] &= 0x30;
            }
        }
        final int emph = (ppuregs[1] & 0xe0) << 1;
        for (int i = bufferoffset; i < (bufferoffset + 256); ++i) {
            bitmap[i] = bitmap[i] & 0x3f | emph;
        }
        if (sprite0hit) {
            sprite0hit = false;
            return true;
        } else {
            return false;
        }
    }
    int off, y, index, sprpxl;

    private void drawSprites(final int scanline) {
        if (!utils.getbit(ppuregs[1], 4)) {
            return; //return immediately if sprites are disabled
        }
        final int bufferoffset = 256 * scanline;
        bgpattern = utils.getbit(ppuregs[0], 4);
        sprpattern = utils.getbit(ppuregs[0], 3);
        int ypos, offset, tilefetched;
        int found = 0;
        final boolean spritesize = utils.getbit(ppuregs[0], 5);
        boolean sprite0here = false;
        //primary evaluation
        for (int spritestart = 0; spritestart < 255; spritestart += 4) {
            //for each sprite, first we cull the non-visible ones
            ypos = OAM[spritestart] + 1;
            offset = scanline - ypos;
            if (ypos > scanline || offset > (spritesize ? 15 : 7)) {
                //sprite is out of range vertically
                continue;
            }
            //if we're here it's a valid renderable sprite
            if (spritestart == 0) {
                sprite0here = true;
                //actually which sprite is flagged for sprite 0 depends on the starting
                //oam address which is, on the real thing, not necessarily zero.
            }
            if (found >= 8) {
                //if more than 8 sprites, set overflow bit and STOP looking
                //todo: add "no sprite limit" option back
                ppuregs[2] |= 0x20;
                break; //also the real PPU does strange stuff on sprite overflow.
            } else {
                //set up ye sprite for rendering
                final int oamextra = OAM[spritestart + 2];
                //bg flag
                spritebgflags[found] = utils.getbit(oamextra, 5);
                //x value
                spriteXlatch[found] = OAM[spritestart + 3];
                spritepals[found] = ((oamextra & 3) + 4) * 4;
                if (utils.getbit(oamextra, 7)) {
                    //if sprite is flipped vertically, reverse the offset
                    offset = (spritesize ? 15 : 7) - offset;
                }
                //now correction for the fact that 8x16 tiles are 2 separate tiles
                if (offset > 7) {
                    offset += 8;
                }
                //get tile address (8x16 sprites can use both pattern tbl pages but only the even tiles)
                final int tilenum = OAM[spritestart + 1];
                if (spritesize) {
                    tilefetched = ((tilenum & 1) * 0x1000)
                            + (tilenum & 0xfe) * 16;
                } else {
                    tilefetched = tilenum * 16
                            + ((sprpattern) ? 0x1000 : 0);
                }
                tilefetched += offset;
                //now load up the shift registers for said sprite
                final boolean hflip = utils.getbit(oamextra, 6);
                if (!hflip) {
                    spriteshiftregL[found] = utils.reverseByte(mapper.ppuRead(tilefetched));
                    spriteshiftregH[found] = utils.reverseByte(mapper.ppuRead(tilefetched + 8));
                } else {
                    spriteshiftregL[found] = mapper.ppuRead(tilefetched);
                    spriteshiftregH[found] = mapper.ppuRead(tilefetched + 8);
                }
                ++found;
            }
        }
        if (found == 0) {
            //no sprites to draw on line.
            return;
        }
        for (int i = found; i < 8; ++i) {
            //fill unused sprite registers with zeros
            spriteshiftregL[found] = 0;
            spriteshiftregH[found] = 0;
        }

        //rendering. this is slow b/c it's iterating through all pxels on line.
        //profiler doesn't see how slow it is though. fix that!
        int startdraw = utils.getbit(ppuregs[1], 2) ? 0 : 8;//sprite left 8 pixels clip


        for (int x = 0; x < 256; ++x) {
            sprpxl = 0;
            index = 7;
            //per pixel in de line that could have a sprite
            for (y = found - 1; y >= 0; --y) {
                off = x - spriteXlatch[y];
                if (off >= 0 && off <= 8) {
                    if ((spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1) != 0) {
                        index = y;
                        sprpxl = 2 * (spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1);
                    }
                    spriteshiftregH[y] >>= 1;
                    spriteshiftregL[y] >>= 1;
                }
            }
            if (sprpxl == 0 || x < startdraw) {
                //no opaque sprite pixel here
                continue;
            }

            if (sprite0here && (index == 0) && bitmap[bufferoffset + x] != bgcolor && x < 255) {
                //sprite 0 hit!
                sprite0hit = true;
                sprite0x = x;
            }
            //now, FINALLY, drawing.
            if (!spritebgflags[index] || (bitmap[bufferoffset + x] == bgcolor)) {
                bitmap[bufferoffset + x] = pal[spritepals[index] + sprpxl];
            }
        }
    }

    private int getattrtbl(final int ntstart, final int tilex, final int tiley) {
        final int base = ntstart + (tilex >> 2) + 8 * (tiley >> 2);
        if ((tilex & 2) == 0) {
            if ((tiley & 2) == 0) {
                return mapper.ppuRead(base) & 3;
            } else {
                return (mapper.ppuRead(base) >> 4) & 3;
            }
        } else {
            if ((tiley & 2) == 0) {
                return (mapper.ppuRead(base) >> 2) & 3;
            } else {
                return (mapper.ppuRead(base) >> 6) & 3;
            }
        }
    }

    public final void debugdraw() {
        //old code, left for dumping out VRAM to debug window.
        //SLOW.
        final boolean tilemode = true;
        if (tilemode) {
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 30; ++j) {
                    newBuff.setRGB(i * 8, j * 8, 8, 8, oldgettile(mapper.ppuRead(0x2000 + i + 32 * j) * 16 + (bgpattern ? 0x1000 : 0)), 0, 8);
                }
            }
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 30; ++j) {
                    newBuff.setRGB(i * 8 + 255, j * 8, 8, 8, oldgettile(mapper.ppuRead(0x2400 + i + 32 * j) * 16 + (bgpattern ? 0x1000 : 0)), 0, 8);
                }
            }
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 30; ++j) {
                    newBuff.setRGB(i * 8, j * 8 + 239, 8, 8, oldgettile(mapper.ppuRead(0x2800 + i + 32 * j) * 16 + (bgpattern ? 0x1000 : 0)), 0, 8);
                }
            }
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 30; ++j) {
                    newBuff.setRGB(i * 8 + 255, j * 8 + 239, 8, 8, oldgettile(mapper.ppuRead(0x2C00 + i + 32 * j) * 16 + (bgpattern ? 0x1000 : 0)), 0, 8);
                }
            }
        } else {
            //draw the tileset instead
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 32; ++j) {
                    newBuff.setRGB(i * 8, j * 8, 8, 8, oldgettile((i + 16 * j) * 16), 0, 8);
                }
            }
        }
        //draw the palettes on the bottom.
//        for (int i = 0; i < 32; ++i) {
//            for (int j = 0; j < 16; ++j) {
//                for (int k = 0; k < 16; ++k) {
//                    newBuff.setRGB(j + i * 16, k + 256, nescolor[0][pal[i]]);
//                }
//            }
//        }
        debuggui.setFrame(newBuff);
        //debugbuff.clear();
    }

    public final int[] oldgettile(final int patterntblptr) {
        // this'll be really really slow
        //for debug only
        int[] dat = new int[64];
        for (int i = 0; i < 8; ++i) {
            //per line of tile ( 1 byte)
            for (int j = 0; j < 8; ++j) {
                //per pixel(1 bit)
                dat[8 * i + j] = ((utils.getbit(mapper.ppuRead(i + patterntblptr), 7 - j)) ? 0x555555 : 0) + ((utils.getbit(mapper.ppuRead(i + patterntblptr + 8), 7 - j)) ? 0xaaaaaa : 0);
            }
        }
        return dat;
    }

    public final void renderFrame(GUIInterface gui) {
        if (PPUDEBUG) {
            debugdraw();
        }
        gui.setFrame(bitmap, bgcolor);

    }
    private int[] tiledata = new int[8];
    private int[] tilepal = new int[4];

    public final int[] getTile(final int tileptr, final int paletteindex, final int off) {
        //returns an 8 pixel line of tile data fron given PPU ram location
        //with given offset and given palette. (color expressed as NES color number)
        tilepal[0] = bgcolor;
        System.arraycopy(pal, paletteindex + 1, tilepal, 1, 3);
        // per line of tile ( 1 byte)
        int linelowbits = mapper.ppuRead(off + tileptr);
        int linehighbits = mapper.ppuRead(off + tileptr + 8);
        for (int j = 7; j >= 0; --j) {
            // per pixel(1 bit)
            tiledata[j] = tilepal[((linehighbits & 1) << 1) + (linelowbits & 1)];
            linehighbits >>= 1;
            linelowbits >>= 1;
        }
        return tiledata;
    }

    public final int getspritehit() {
        return (sprite0x < 255) ? sprite0x : -1;
    }
    
    public int[] getBitmap(){
        return bitmap;
        
    }
}
