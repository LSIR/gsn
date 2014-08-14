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
* File: src/gsn/utils/models/auxiliar/Polynomial.java
*
* @author Alexandru Arion
* @author Sofiane Sarni
*
*/

package gsn.utils.models.auxiliar;

public class Polynomial {
	double[] coefs;
	int degree;

	public Polynomial(double[] coefs)
	{
		this.degree = coefs.length - 1;
		this.coefs = coefs;
	}

	public int getDegree()
	{
		return this.degree;
	}

	public double Compute(double x)
	{
		double retval = 0.0;

		double term = 1;
		for(int i = 0; i <= this.degree; i++)
		{
			retval += term * this.coefs[i];
			term *= x;
		}

		return retval;
	}

	public double[] getCoefs()
	{
		return coefs;
	}
}
