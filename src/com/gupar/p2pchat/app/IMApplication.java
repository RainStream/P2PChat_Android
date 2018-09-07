package com.gupar.p2pchat.app;

import android.app.Application;
import android.content.Intent;

import com.gupar.p2pchat.imservice.service.IMService;
import com.gupar.p2pchat.utils.Logger;

public class IMApplication extends Application {

	private Logger logger = Logger.getLogger(IMApplication.class);
	private static IMApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();
		logger.i("Application starts");
		instance = this;
		startIMService();
		// ImageLoaderUtil.initImageLoaderConfig(getApplicationContext());
	}

	public static IMApplication getInstance() {
		return instance;
	}

	private void startIMService() {
		logger.i("start IMService");
		Intent intent = new Intent();
		intent.setClass(this, IMService.class);
		startService(intent);
	}

	public static boolean gifRunning = true;// gif是否运行


}
