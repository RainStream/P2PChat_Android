package com.gupar.p2pchat.imservice.service;

import com.gupar.p2pchat.DB.sp.ConfigurationSp;
import com.gupar.p2pchat.imservice.manager.IMCallManager;
import com.gupar.p2pchat.imservice.manager.IMLoginManager;
import com.gupar.p2pchat.imservice.manager.IMSocketManager;
import com.gupar.p2pchat.utils.Logger;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

/**
 * IMService 负责所有IMManager的初始化与reset 并且Manager的状态的改变 也会影响到IMService的操作 备注:
 * 有些服务应该在LOGIN_OK 之后进行 todo IMManager reflect or just like
 * ctx.getSystemService()
 */
public class IMService extends Service {
	private Logger logger = Logger.getLogger(IMService.class);

	/** binder */
	private IMServiceBinder binder = new IMServiceBinder();
	private IntentBroadCast intentBroadCast;

	public class IMServiceBinder extends Binder {
		public IMService getService() {
			return IMService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		logger.i("IMService onBind");
		return binder;
	}

	// 所有的管理类
	private IMSocketManager socketMgr = IMSocketManager.instance();
	private IMLoginManager loginMgr = IMLoginManager.instance();
	private IMCallManager callMgr = IMCallManager.instance();
	private ConfigurationSp configSp;

	@Override
	public void onCreate() {
		logger.i("IMService onCreate");
		super.onCreate();
		startForeground((int) System.currentTimeMillis(), new Notification());
		// 注册网络监听
		intentBroadCast = new IntentBroadCast();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(intentBroadCast, filter);
	}

	@Override
	public void onDestroy() {
		logger.i("IMService onDestroy");
		// todo 在onCreate中使用startForeground
		// 在这个地方是否执行 stopForeground呐
		handleLoginout();
		if (intentBroadCast != null) {
			unregisterReceiver(intentBroadCast);
		}
		super.onDestroy();
	}

	// 负责初始化 每个manager
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.i("IMService onStartCommand");
		// 应用开启初始化 下面这几个怎么释放 todo
		Context ctx = getApplicationContext();
		// // 放在这里还有些问题 todo
		socketMgr.onStartIMManager(ctx);
		loginMgr.onStartIMManager(ctx);
		callMgr.onStartIMManager(ctx);
		return START_STICKY;
	}

	private void handleLoginout() {
		logger.d("imservice#handleLoginout");

		// login需要监听socket的变化,在这个地方不能释放，设计上的不合理?
		socketMgr.reset();
		loginMgr.reset();
		callMgr.reset();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		logger.d("imservice#onTaskRemoved");
		// super.onTaskRemoved(rootIntent);
		this.stopSelf();
	}

	/** -----------------get/set 的实体定义--------------------- */

	public IMSocketManager getSocketManager() {
		return socketMgr;
	}

	public IMLoginManager getLoginManager() {
		return loginMgr;
	}

	public IMCallManager getCallManager() {
		return callMgr;
	}

	public ConfigurationSp getConfigSp() {
		return configSp;
	}
	
	class IntentBroadCast extends BroadcastReceiver {
		State wifiState = null;
		State mobileState = null;

		@Override
		public void onReceive(Context context, Intent arg1) {
			// TODO Auto-generated method stub
			// 获取手机的连接服务管理器，这里是连接管理器类
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			wifiState = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
			mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
					.getState();
			if (wifiState != null && mobileState != null
					&& State.CONNECTED != wifiState
					&& State.CONNECTED == mobileState) {
				Toast.makeText(context, "手机网络连接成功！", Toast.LENGTH_SHORT).show();
				getLoginManager().relogin();
			} else if (wifiState != null && mobileState != null
					&& State.CONNECTED == wifiState
					&& State.CONNECTED != mobileState) {
				Toast.makeText(context, "wifi连接成功！", Toast.LENGTH_SHORT).show();
				getLoginManager().relogin();
			} else if (wifiState != null && mobileState != null
					&& State.CONNECTED != wifiState
					&& State.CONNECTED != mobileState) {
				Toast.makeText(context, "手机没有任何网络...", Toast.LENGTH_SHORT).show();
			}
		}

	}

}


