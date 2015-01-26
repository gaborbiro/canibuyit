package com.gb.canibuythat;


import android.app.Application;
import android.content.Context;


/**
 * Created by GABOR on 2015-jan.-24.
 */
public class App extends Application {

	private static Context	appContext;


	@Override
	public void onCreate() {
		super.onCreate();
		this.appContext = this;
	}


	public static Context getAppContext() {
		return appContext;
	}
}
