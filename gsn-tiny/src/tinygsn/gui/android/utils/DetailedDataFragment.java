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
