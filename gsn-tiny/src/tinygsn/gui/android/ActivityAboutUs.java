package tinygsn.gui.android;

import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * About the development team
 * 
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class ActivityAboutUs extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Sherlock___Theme_DarkActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        ((TextView)findViewById(R.id.text)).setText("About us: " 
        + "\n\n\tThis application is developed in a semester project of my Master study at EPFL."
        		+ "\n\t\t\t* Developer: Do Ngoc Hoan \n\t\t\t\t(hoan.do@epfl.ch)"
        		+ "\n\t\t\t* Supervisor: Sofiane Sarni \n\t\t\t\t(sofiane.sarni@epfl.ch)"
        		+ "\n\n\tSource code of this project is publicly available at https://code.google.com/p/tinygsn/");
        
    }
    
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			break;
		}
		return true;
	}
}
