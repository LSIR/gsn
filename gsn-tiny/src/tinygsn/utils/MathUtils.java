/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/utils/MathUtils.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.utils;

import java.util.Random;

public class MathUtils {
	public static double INFINITIVE = 10e6;
	public static double DEFAULT_STEP = 1;

	public static double getNextRandomValue(double v) {
		return getNextRandomValue(v, -INFINITIVE, INFINITIVE, DEFAULT_STEP);
	}

	public static double getNextRandomValue(double v, double lBound, double uBound) {
		return getNextRandomValue(v, lBound, uBound, DEFAULT_STEP);
	}

	public static double getNextRandomValue(double v, double lBound,
			double uBound, double step) {
		double nv = v;
		Random rand = new Random();
		int numSteps = rand.nextInt(10) + 1;
		step = step * rand.nextDouble();

		while (numSteps > 0) {
			double upDown = 1;
			if (rand.nextDouble() < 0.5)
				upDown = -1;
			nv = nv + step * upDown;
			numSteps--;
		}

		if (nv < lBound)
			nv = uBound - (nv % (uBound - lBound));
		if (nv > uBound)
			nv = lBound + (nv % (uBound - lBound));

		return nv;
	}

	public static void main(String[] args) {
		double v = 5.5;
		for (int i = 1; i < 40; i++) {
			v = getNextRandomValue(v, -2, 8);
			System.out.println(v);
		}

	}
}
