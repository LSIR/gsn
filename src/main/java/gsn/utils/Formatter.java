/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/utils/Formatter.java
*
* @author Sofiane Sarni
*
*/

package gsn.utils;

import java.util.Iterator;
import java.util.List;

public class Formatter {

    public static String listArray(int[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(int[] a) {
        return listArray(a, 0, a.length-1);
    }

    public static String listArray(int[] a, int len) {
        return listArray(a, 0, len-1);
    }

    public static String listArray(int[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len-1, hexFormat);
    }

    public static String listArray(int[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listArray(byte[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(byte[] a, int len) {
        return listArray(a, 0, len-1);
    }

    public static String listArray(byte[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len-1, hexFormat);
    }

    public static String listArray(byte[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i] & 0xff)).append(" ");
            dec_sb_2.append(a[i] & 0xff).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listArray(UnsignedByte[] a, int from, int to) {
        return listArray(a, from, to, false);
    }

    public static String listArray(UnsignedByte[] a, int len) {
        return listArray(a, 0, len-1);
    }

    public static String listArray(UnsignedByte[] a, int len, boolean hexFormat) {
        return listArray(a, 0, len-1, hexFormat);
    }

    public static String listArray(UnsignedByte[] a, int from, int to, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i = from; (i <= to && i < a.length); i++) {
            hex_sb_2.append(String.format("%02x", a[i].getByte())).append(" ");
            dec_sb_2.append(a[i].getInt()).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");
        dec_sb_2.append("(").append(String.format("%2d", to - from + 1)).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

    public static String listUnsignedByteList(List<UnsignedByte> a) {
        return listUnsignedByteList(a, false);
    }

    public static String listUnsignedByteList(List<UnsignedByte> a, boolean hexFormat) {
        StringBuilder hex_sb_2 = new StringBuilder();
        StringBuilder dec_sb_2 = new StringBuilder();
        for (int i=0;i<a.size();i++) {
            hex_sb_2.append(String.format("%02x", a.get(i).getByte())).append(" ");
            dec_sb_2.append(a.get(i).getInt()).append(" ");
        }

        hex_sb_2.append("(").append(String.format("%2d", a.size())).append(")");
        dec_sb_2.append("(").append(String.format("%2d", a.size())).append(")");

        if (hexFormat)
            return hex_sb_2.toString();
        else
            return dec_sb_2.toString();
    }

}
