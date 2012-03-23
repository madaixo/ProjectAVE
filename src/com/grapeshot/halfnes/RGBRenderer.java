/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes;

import java.awt.image.BufferedImage;

/**
 *
 * @author Andrew
 */
public class RGBRenderer extends Renderer {
    private final static int[][] nescolor = GetNESColors();

    public BufferedImage render(int[] nespixels, int bgcolor) {
        //and now replace the nes color numbers with rgb colors (respecting color emph bits)
        for (int i = 0; i < nespixels.length; ++i) {
            nespixels[i] = nescolor[(nespixels[i] & 0x1c0) >> 6][nespixels[i] & 0x3f];
        }
        return getImageFromArray(nespixels, 256 * 8, 256, 224);
    }

    private static int[][] GetNESColors() {
        //just or's all the colors with opaque alpha and does the color emphasis calcs
        int[] colorarray = {0x757575, 0x271B8F, 0x0000AB,
            0x47009F, 0x8F0077, 0xAB0013, 0xA70000, 0x7F0B00, 0x432F00,
            0x004700, 0x005100, 0x003F17, 0x1B3F5F, 0x000000, 0x000000,
            0x000000, 0xBCBCBC, 0x0073EF, 0x233BEF, 0x8300F3, 0xBF00BF,
            0xE7005B, 0xDB2B00, 0xCB4F0F, 0x8B7300, 0x009700, 0x00AB00,
            0x00933B, 0x00838B, 0x000000, 0x000000, 0x000000, 0xFFFFFF,
            0x3FBFFF, 0x5F97FF, 0xA78BFD, 0xF77BFF, 0xFF77B7, 0xFF7763,
            0xFF9B3B, 0xF3BF3F, 0x83D313, 0x4FDF4B, 0x58F898, 0x00EBDB,
            0x000000, 0x000000, 0x000000, 0xFFFFFF, 0xABE7FF, 0xC7D7FF,
            0xD7CBFF, 0xFFC7FF, 0xFFC7DB, 0xFFBFB3, 0xFFDBAB, 0xFFE7A3,
            0xE3FFA3, 0xABF3BF, 0xB3FFCF, 0x9FFFF3, 0x000000, 0x000000,
            0x000000};
        for (int i = 0; i < colorarray.length; ++i) {
            colorarray[i] |= 0xff000000;
        }
        int[][] colors = new int[8][colorarray.length];
        for (int j = 0; j < colorarray.length; ++j) {
            colors[0][j] = colorarray[j];
            //emphasize red
            colors[1][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16)) << 16))
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8) * 0.7) << 8)
                    + (int) ((colorarray[j] & 0x000000ff) * 0.7);
            //emphasize green
            colors[2][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16) * 0.7)) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8)) << 8)
                    + (int) ((colorarray[j] & 0x000000ff) * 0.7);
            //emphasize yellow
            colors[3][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16))) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8)) << 8)
                    + (int) ((colorarray[j] & 0x000000ff) * 0.7);
            //emphasize blue
            colors[4][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16) * 0.7)) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8) * 0.7) << 8)
                    + (int) ((colorarray[j] & 0x000000ff));
            //emphasize purple
            colors[5][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16))) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8) * 0.7) << 8)
                    + (int) ((colorarray[j] & 0x000000ff));
            //emphasize cyan?
            colors[6][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16) * 0.7)) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8)) << 8)
                    + (int) ((colorarray[j] & 0x000000ff));
            //de-emph all 3 colors
            colors[7][j] = (0xff << 24)
                    + ((int) ((((colorarray[j] & 0x00ff0000) >> 16) * 0.7)) << 16)
                    + ((int) (((colorarray[j] & 0x0000ff00) >> 8) * 0.7) << 8)
                    + (int) ((colorarray[j] & 0x000000ff) * 0.7);

        }
        return colors;
    }
}
