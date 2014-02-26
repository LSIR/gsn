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
* File: src/gsn/beans/windowing/WindowingUtil.java
*
* @author gsn_devs
* @author Timotee Maret
*
*/

package gsn.beans.windowing;

public class WindowingUtil {

    public static long GCD(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return GCDHelper(a, b);
    }

    private static long GCDHelper(long a, long b) {
        if (b == 0) {
            return a;
        }
        return GCDHelper(b, a % b);
    }
}
