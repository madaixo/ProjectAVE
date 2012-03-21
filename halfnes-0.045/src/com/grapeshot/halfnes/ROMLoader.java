package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.mappers.Mapper;

public class ROMLoader {
    //this is the oldest code in the project... I'm honestly ashamed
    //at how it's structured but for now it works.
    //TODO: fix this up

    public String name;
    public int prgsize;
    public int chrsize;
    public Mapper.MirrorType scrolltype;
    public int mappertype;
    public int prgoff;
    public int chroff;
    public boolean savesram = false;
    private int[] therom;

    public ROMLoader(String filename) {
        therom = FileUtils.readfromfile(filename);
        name = filename;
    }

    public int[] ReadHeader(int len) {
        // iNES header is 16 bytes, nsf header is 128
        return load(len, -16);
    }

    public void parseInesheader() throws BadMapperException {
        int[] inesheader = ReadHeader(16);
        // decode iNES 1.0 headers
        // 1st 4 bytes : $4E $45 $53 $1A
        if (inesheader[0] != 0x4E || inesheader[1] != 0x45
                || inesheader[2] != 0x53 || inesheader[3] != 0x1A) {
            // not a valid file
            if (inesheader[0] == 'U') {
                throw new BadMapperException("This is a UNIF file with the wrong extension");
            }
            throw new BadMapperException("iNES Header Invalid");

        }
        prgsize = 16384 * inesheader[4];
        chrsize = 8192 * inesheader[5];
        scrolltype = utils.getbit(inesheader[6], 3) ? Mapper.MirrorType.FOUR_SCREEN_MIRROR : (utils.getbit(inesheader[6], 0) ? Mapper.MirrorType.V_MIRROR : Mapper.MirrorType.H_MIRROR);
        savesram = utils.getbit(inesheader[6], 1);
        mappertype = (inesheader[6] >> 4);
        if (inesheader[11] + inesheader[12] + inesheader[13] + inesheader[14]
                + inesheader[15] == 0) {// fix for DiskDude
            mappertype += ((inesheader[7] >> 4) << 4);
        }

        // calc offsets; header not incl. here
        prgoff = 0;
        chroff = 0 + prgsize;
    }

    public int[] load(int size, int offset){
        int[] bindata = new int[size];
        System.arraycopy(therom, offset + 16, bindata, 0, size);
        return bindata;
    }
}
