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
* File: src/gsn/wrappers/tinyos/SensorScope2Packet.java
*
* @author Sofiane Sarni
*
*/

package gsn.wrappers.tinyos;

import java.util.Arrays;

public class SensorScope2Packet {
    public byte[] bytes;
    long timestamp;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensorScope2Packet that = (SensorScope2Packet) o;

        if (timestamp != that.timestamp) return false;
        if (!Arrays.equals(bytes, that.bytes)) return false;

        return true;
    }

    public int hashCode() {
        int result = bytes != null ? Arrays.hashCode(bytes) : 0;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    public String toString() {

        StringBuilder hex_sb = new StringBuilder();

        hex_sb.append(timestamp).append(" : ");

        for (int i = 0; i < bytes.length; i++) {
            hex_sb.append(String.format("%02x", bytes[i])).append(" ");
        }

        return hex_sb.toString() + " (" + String.format("%2d", bytes.length) + ")";
    }

    public SensorScope2Packet(long timestamp, byte[] bytes) {
        this.timestamp = timestamp;
        this.bytes = bytes;
    }
}
