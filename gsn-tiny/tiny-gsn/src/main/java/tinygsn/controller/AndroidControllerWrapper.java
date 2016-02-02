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
* File: gsn-tiny/src/tinygsn/controller/AndroidControllerListVSNew.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.controller;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

import tinygsn.beans.StaticData;
import tinygsn.beans.Subscription;
import tinygsn.gui.android.utils.SensorRow;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.services.AbstractScheduler;
import tinygsn.storage.db.SqliteStorageManager;


public class AndroidControllerWrapper extends AbstractController {


	private ArrayList<AbstractWrapper> wrapperList = new ArrayList<AbstractWrapper>();
	
	public AndroidControllerWrapper() {	}
	
	public ArrayList<SensorRow> loadListWrapper() {
		if (wrapperList.size()==0){
			Properties p = AbstractWrapper.getWrapperList(StaticData.globalContext);
			for (String s : p.stringPropertyNames()){
				try {
					wrapperList.add(StaticData.getWrapperByName(p.getProperty(s)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Collections.sort(wrapperList,new Comparator<AbstractWrapper>(){
				@Override
				public int compare(AbstractWrapper lhs, AbstractWrapper rhs) {
					return lhs.getWrapperName().compareTo(rhs.getWrapperName());
				}});
			
			for (String s : StaticData.getLocalWrapperNames()){
				try {
					wrapperList.add(StaticData.getWrapperByName(s));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		ArrayList<SensorRow> r = new ArrayList<>();
		for(AbstractWrapper w : wrapperList){
			r.add(new SensorRow(w.getWrapperName(), w.getConfig().isRunning(), "duty cycle: "+w.getDcDuration() +"s every "+w.getDcInterval()+"s."));
		}
		return r;
	}
	
	public ArrayList<SensorRow> loadListScheduler(){
		ArrayList<SensorRow> r = new ArrayList<SensorRow>();
		for (String s : AbstractScheduler.SCHEDULER_LIST){
			try {
				AbstractScheduler w = (AbstractScheduler) StaticData.getWrapperByName(s);
				StringBuilder sb = new StringBuilder("running every ");
				sb.append(w.getDcInterval()).append("s. \n managing:\n");
				for (String m:w.getManagedSensors()){
					sb.append(m).append("\n");
				}
				r.add(new SensorRow(w.getWrapperName(), w.getConfig().isRunning(), sb.toString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return r;
	}
	
	public ArrayList<String> loadListManaged(){
		ArrayList<String> managed = new ArrayList<String>();
		for (String s : AbstractScheduler.SCHEDULER_LIST){
			try {
				AbstractScheduler w = (AbstractScheduler) StaticData.getWrapperByName(s);
				if(w.getConfig().isRunning()){
					for (String m:w.getManagedSensors()){
						managed.add(m);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return managed;
	}
	
}