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
* File: gsn-tiny/src/tinygsn/gui/android/utils/DetailedDataFragment.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.gui.android.utils;

import tinygsn.gui.android.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * A dialog fragment to show the detailed data
 * , e.g. detailed data in viewData activity or the pulled data 
 * 
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class DetailedDataFragment extends SherlockDialogFragment {
	String text;
	
	public DetailedDataFragment(String text){
		this.text = text;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.text, container, false);
		View tv = v.findViewById(R.id.text);
		((TextView) tv).setText(text);
		return v;
	}
}
