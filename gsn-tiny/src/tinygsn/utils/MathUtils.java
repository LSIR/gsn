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
