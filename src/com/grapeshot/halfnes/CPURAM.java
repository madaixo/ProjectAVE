/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.mappers.Mapper;
import java.util.Arrays;

/**
 *
 * @author Andrew Hoffman
 */
public class CPURAM {

    private boolean hasprgram = true;
    private final int[] wram = new int[2048];
    Mapper mapper;
    public APU apu;
    PPU ppu; //need these to call their write handlers from here.

    public CPURAM(final Mapper mappy) {
        mapper = mappy;
        // init memory
        Arrays.fill(wram, 0xff);
    }

    public final int read(final int addr) {
        if (addr > 0x4018) {
            return mapper.cartRead(addr);
        } else if (addr <= 0x1fff) {
            return wram[addr & 0x7FF];
        } else if (addr <= 0x3fff) {
            // 8 byte ppu regs; mirrored lots
            return ppu.read(addr & 7);
        } else if (0x4000 <= addr && addr <= 0x4018) {
            return apu.read(addr - 0x4000);
        } else {
            return addr >> 8; //open bus
        }
    }

    public final void write(final int addr, final int data) {
        if (addr > 0x4018) {
            mapper.cartWrite(addr, data);
        } else if (addr <= 0x1fff) {
            wram[addr & 0x7FF] = data;
        } else if (addr <= 0x3fff) {
            // 8 byte ppu regs; mirrored lots
            ppu.write(addr & 7, data);
        } else if (0x4000 <= addr && addr <= 0x4018) {
            apu.write(addr - 0x4000, data);
        }
    }

    public void setPrgRAMEnable(boolean b) {
        hasprgram = b;
    }

    public void setAPU(APU apu) {
        this.apu = apu;
    }

    public void setPPU(PPU ppu) {
        this.ppu = ppu;
    }
}
