/*******************************************************************************
 * Copyright 2011, 2012 Torsten Hildebrandt and BIBA - Bremer Institut für Produktion und Logistik GmbH
 *
 * This file is part of jasima, v1.0.
 *
 * jasima is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jasima is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jasima.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package jasima.core.random.continuous;

import java.util.Arrays;

/**
 * A DblStream infinitely returning the numbers of getValues() in exactly this
 * order. After the last value being returned the sequence starts again with the
 * first number.
 * 
 * @author Torsten Hildebrandt
 */
public class DblConst extends DblStream {

	private double[] values;

	private int next;

	public DblConst() {
		this(null);
	}

	public DblConst(double... vs) {
		super();
		setValues(vs);
	}

	public double[] getValues() {
		return values;
	}

	public void setValues(double... vs) {
		this.values = vs;
	}

	@Override
	public double nextDbl() {
		double v = values[next];
		// wrap around
		if (++next == values.length)
			next = 0;
		return v;
	}

	@Override
	public String toString() {
		return "DblConst" + Arrays.toString(values);
	}

	@Override
	public DblConst clone() throws CloneNotSupportedException {
		DblConst c = (DblConst) super.clone();

		if (values != null)
			c.values = Arrays.copyOf(values, values.length);

		return c;
	}

}