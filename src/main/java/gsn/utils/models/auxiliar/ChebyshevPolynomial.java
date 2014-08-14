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
* File: src/gsn/utils/models/auxiliar/ChebyshevPolynomial.java
*
* @author Alexandru Arion
* @author Sofiane Sarni
*
*/

package gsn.utils.models.auxiliar;

import java.util.Vector;

public class ChebyshevPolynomial {

	int degree;
	Vector<Polynomial> polis;
	double[] coefs;

	public ChebyshevPolynomial(int degree, double[] coefs)
	{
		this.degree = degree;
		this.polis = new Vector<Polynomial>();
		//generate polis up to degree
		//degree 0
		this.polis.add(new Polynomial(new double[]{1}));
		//degree 1
		if(degree >= 1)
			this.polis.add(new Polynomial(new double[]{0, 1}));
		//other degrees
		for(int i = 2; i <= degree; i++)
		{
			this.polis.add(new Polynomial(GetNextPoly(i)));
		}
		this.coefs = coefs;
	}

	public double Calculate(double x)
	{
		double retval = 0.0;

		for(int i = 1; i < this.polis.size(); i++)
		{
			retval += this.polis.elementAt(i).Compute(x) * this.coefs[i];
		}

		retval += this.coefs[0] / 2;

		return retval;
	}

	public double Calculate(double[] cfs, double x)
	{
		double retval = 0.0;

		double term = 1;
		for(int i = 0; i < cfs.length; i++)
		{
			retval += term * cfs[i];
			term *= x;
		}

		return retval;
	}

	public double[] GetFullCoeficients()
	{
		double[] retval = new double[this.coefs.length];

		for(int i = 0; i < retval.length; i++)
		{
			retval[i] = 0.0;
			for(int j = i; j < this.polis.size(); j++)
			{
				retval[i] += this.polis.get(j).coefs[i];
			}
		}

		return retval;
	}

	private double[] GetNextPoly(int i)
	{
		double[] retval = new double[i + 1];
		double[] coef_1 = this.polis.elementAt(i - 1).getCoefs();
		double[] coef_2 = this.polis.elementAt(i - 2).getCoefs();

		for(int j = 0; j < i + 1; j++)
		{
			retval[j] = 0;
		}

		for(int j = 0; j < i - 1; j++)
		{
			retval[j] += -coef_2[j];
		}

		for(int j = 1; j < i + 1; j++)
		{
			retval[j] += 2 * coef_1[j - 1];
		}

		return retval;
	}
}
