package tinygsn.controller;

import tinygsn.beans.StreamElement;
import tinygsn.storage.StorageManager;
import android.app.Activity;

public abstract class AbstractController {

  public abstract void startLoadVSList();
//
//  public abstract void tinygsnStop();
//
	public abstract void consume(StreamElement streamElement);
//
	public abstract StorageManager getStorageManager();
	
	public abstract Activity getActivity();
}