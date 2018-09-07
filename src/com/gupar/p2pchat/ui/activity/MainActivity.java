package com.gupar.p2pchat.ui.activity;

import java.util.ArrayList;

import com.gupar.p2pchat.imservice.event.MediaEvent;
import com.gupar.p2pchat.imservice.event.UserEvent;
import com.gupar.p2pchat.imservice.manager.IMLoginManager.UserInfo;
import com.gupar.p2pchat.imservice.service.IMService;
import com.gupar.p2pchat.imservice.service.support.IMServiceConnector;
import com.gupar.p2pchat.utils.Logger;

import com.gupar.p2pchat.R;

import de.greenrobot.event.EventBus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Logger logger = Logger.getLogger(MainActivity.class);

	public static MainActivity mainActivity;
	private IMService imService = null;
	private TextView userName;
	private ListView listview;
	public UserAdapter adapter; 
	private long exitTime = 0;
	private boolean bCalling = false;
	
	private IMServiceConnector imServiceConnector = new IMServiceConnector() {
		@Override
		public void onServiceDisconnected() {
		}

		@Override
		public void onIMServiceConnected() {

			logger.d("call#onIMServiceConnected");

			try {
				if (imService == null) {
					imService = imServiceConnector.getIMService();
					String name = imService.getLoginManager().getLoginUser();
					userName.setText("当前用户："+name);
					
					listview.setAdapter(adapter);
				}

			} catch (Exception e) {
				// 任何未知的异常
				logger.w("loadIdentity failed");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// app 自动更新
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			mainActivity = this;	
			bCalling = false;
			
			userName = (TextView)findViewById(R.id.textLoginName);
			
			listview = (ListView) findViewById(R.id.listView);
			adapter = new UserAdapter(this);
			
			imServiceConnector.connect(this);
			EventBus.getDefault().register(this);
		} catch (Exception e) {

			logger.e("ERROR", "ERROR IN CODE: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public void ConnectToPeer(int peer_id,boolean initiator)
	{
		Intent intent = new Intent(MainActivity.this, CallActivity.class);
		intent.putExtra("initiator", initiator);
		intent.putExtra("peer_id", peer_id);
		startActivityForResult(intent,0);
		bCalling = true;
	}
	
	@Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        if (requestCode == 0 ) {  
        	bCalling = false;
        } 
    }  
	
	@SuppressWarnings("unused")
	private class UserAdapter extends BaseAdapter {

		private Context context;
		private LayoutInflater inflater;	

		public UserAdapter(Context context) {
			super();
			this.context = context;
			inflater = LayoutInflater.from(context);			
		}

		@Override
		public int getCount() {
			return imService.getLoginManager().getUsers().size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View view, ViewGroup arg2) {
			if (view == null) {
				view = inflater.inflate(R.layout.listview_item, null);
			}
			
			ArrayList<UserInfo> userlist = imService.getLoginManager().getUsers();

			final TextView edit = (TextView) view.findViewById(R.id.edit);
			final UserInfo info = userlist.get(position);
			String name = "用户ID: " + info.peer_id + "  用户名： " + info.Name;
			edit.setText(name); // 在重构adapter的时候不至于数据错乱
			Button call = (Button) view.findViewById(R.id.call);
//			edit.setOnFocusChangeListener(new OnFocusChangeListener() {
//				@Override
//				public void onFocusChange(View v, boolean hasFocus) {
//					ArrayList<UserInfo> userlist = imService.getLoginManager().getUsers();
//					if (userlist.size() > 0) {
//						userlist.set(position, info);
//					}
//				}
//			});
			call.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0){
					//开始进行通话
					ConnectToPeer(info.peer_id,true);
				}
			});
			return view;
		}
    }  
	

	/**
	 * ----------------------------event 事件驱动----------------------------
	 */
	public void onEventMainThread(MediaEvent event) {
		int peer_id = event.getPeerId();
		String msg = event.getMsg();
		switch (event.getEvent()) {
		case RECV__CONNECT_TO_PEER://收到对方音视频请求
			if(!bCalling){
				ConnectToPeer(peer_id,false);
				imService.getCallManager().AllowVideo(peer_id);
			}
			else{
				//忙碌的话拒绝对方请求
				imService.getCallManager().RejectVideo(peer_id);
			}
			break;
		case REQUEST_CONNECT_TO_PEER://请求音视频连接
			imService.getCallManager().RequestVideo(peer_id);
			break;
		case PUSH_CONNECT_MSG://推送音视频消息
			imService.getCallManager().sendToPeer(peer_id,msg);
			break;
		case DIS_CONNECT_TO_PEER:
			imService.getCallManager().StopVideo(peer_id);
			break;
		default:
			break;
		}
	}
	
	public void onEventMainThread(UserEvent event) {
		switch(event.getEvent()){
		case NONE:
			break;
		case USER_UPDATE:
			adapter.notifyDataSetChanged();
			break;
		default:
			break;
		}
	}
	

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		if ((System.currentTimeMillis() - exitTime) > 2000) {
			Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
			exitTime = System.currentTimeMillis();
		}else {
			super.finish();
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		imService.stopSelf();
		imServiceConnector.disconnect(this);
		EventBus.getDefault().unregister(this);
	}

}
