package tinygsn.gui.android;

import org.kroz.activerecord.ActiveRecordBase;
import org.kroz.activerecord.ActiveRecordException;
import org.kroz.activerecord.Database;
import org.kroz.activerecord.DatabaseBuilder;
import tinygsn.gui.android.utils.SubscriptionRow;
import tinygsn.utils.Const;
import android.app.Application;
import android.util.Log;

public class TinyGSN extends Application {

	public ActiveRecordBase _db;

	public TinyGSN() {

	}

	@Override
	public void onCreate() {
		super.onCreate();

//		// --------- Prepare mDatabase connection ----------
//		DatabaseBuilder builder = new DatabaseBuilder(Const.DATABASE_NAME);
//		builder.addClass(SubscriptionRow.class);
//		Database.setBuilder(builder);
//		try {
//			_db = ActiveRecordBase.open(this, Const.DATABASE_NAME,
//					Const.DATABASE_VERSION);
//			Log.v("TinyGSN", "Open ok");
//		}
//		catch (ActiveRecordException e) {
//			e.printStackTrace();
//		}

		Log.v("TinyGSN", "TinyGSN is called!");

	}
}
