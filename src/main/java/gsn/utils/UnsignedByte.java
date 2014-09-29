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
* File: src/gsn/utils/UnsignedByte.java
*
* @author Sofiane Sarni
*
*/

package gsn.utils;

public class UnsignedByte {
    private byte byteValue;
    private int intValue;

    public UnsignedByte() {
        byteValue = 0;
        intValue = 0;
    }

    public UnsignedByte(byte b) {
        byteValue = b;
        intValue = (int) b & 0xff;
    }

    public UnsignedByte(int i) {
        i = i & 0xff;
        byteValue = (byte) i;
        intValue = i;
    }

    public UnsignedByte setValue(byte b) {
        byteValue = b;
        intValue = (int) b & 0xff;
        return this;
    }

    public UnsignedByte setValue(int i) {
        i = i & 0xff;
        byteValue = (byte) i;
        intValue = i;
        return this;
    }

    public int getInt() {
        return intValue;
    }

    public byte getByte() {
        return byteValue;
    }

    public String toString() {
        return "(byte:" + getByte() + ", int:" + getInt() + ")";
    }

    public static byte[] UnsignedByteArray2ByteArray(UnsignedByte[] uba) {
        int length = uba.length;
        byte[] ba = new byte[length];
        for (int i = 0; i < length; i++)
            ba[i] = uba[i].getByte();
        return ba;
    }

    public static UnsignedByte[] ByteArray2UnsignedByteArray(byte[] ba) {
        int length = ba.length;
        UnsignedByte[] uba = new UnsignedByte[length];
        for (int i = 0; i < length; i++)
            uba[i].setValue(ba[i]);
        return uba;
    }
}