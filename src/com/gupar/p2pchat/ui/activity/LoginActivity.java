package com.gupar.p2pchat.ui.activity;

import com.gupar.p2pchat.R;
import com.gupar.p2pchat.imservice.event.LoginEvent;
import com.gupar.p2pchat.imservice.event.SocketEvent;
import com.gupar.p2pchat.imservice.service.IMService;
import com.gupar.p2pchat.imservice.service.support.IMServiceConnector;
import com.gupar.p2pchat.utils.Constants;
import com.gupar.p2pchat.utils.IMUIHelper;
import com.gupar.p2pchat.utils.Logger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

/**
 * @YM 1. 链接成功之后，直接判断是否loginSp是否可以直接登陆 true: 1.可以登陆，从DB中获取历史的状态
 *     2.建立长连接，请求最新的数据状态 【网络断开没有这个状态】 3.完成
 *     <p/>
 *     false:1. 不能直接登陆，跳转到登陆页面 2. 请求消息服务器地址，链接，验证，触发loginSuccess 3. 保存登陆状态
 */
public class LoginActivity extends Activity {
	private Logger logger = Logger.getLogger(LoginActivity.class);
	private EditText mEditServer;
	private EditText mEditUserName;
	private EditText mEditPassWord;
	private View loginPage;
	private View splashPage;
	private View mLoginStatusView;
	private InputMethodManager intputManager;
	private IMService imService;
	private boolean loginSuccess = false;
	private String mServer,mUserName, mPassword;
	private boolean isLogining = false;

	private IMServiceConnector imServiceConnector = new IMServiceConnector() {
		@Override
		public void onServiceDisconnected() {
		}

		@Override
		public void onIMServiceConnected() {
			logger.d("login#onIMServiceConnected");
			imService = imServiceConnector.getIMService();
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler uiHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if(!loginSuccess){
				showLoginPage();
			}			
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		EventBus.getDefault().register(this);
		mEditServer = (EditText) findViewById(R.id.server);
		mEditUserName = (EditText) findViewById(R.id.name);
		mEditPassWord = (EditText) findViewById(R.id.password);
		intputManager = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
		mLoginStatusView = findViewById(R.id.login_status);
		splashPage = findViewById(R.id.splash_page);
		loginPage = findViewById(R.id.login_page);
		logger.d("login#onCreate");
		imServiceConnector.connect(LoginActivity.this);
		initListener();
		uiHandler.sendEmptyMessageDelayed(0, 1000);
		mEditServer.setText(Constants.SERVER_IP);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		if (MainActivity.mainActivity == null && !loginSuccess) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		imServiceConnector.disconnect(LoginActivity.this);
		EventBus.getDefault().unregister(this);
	}

	private void showLoginPage() {
		splashPage.setVisibility(View.GONE);
		loginPage.setVisibility(View.VISIBLE);
	}

	public void attemptLogin() {
		mServer = mEditServer.getText().toString();
		mUserName = mEditUserName.getText().toString();
		mPassword = mEditPassWord.getText().toString();
		if (TextUtils.isEmpty(mPassword)) {
			Toast.makeText(this, getString(R.string.error_pwd_required),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (TextUtils.isEmpty(mUserName)) {
			Toast.makeText(this, getString(R.string.error_name_required),
					Toast.LENGTH_SHORT).show();
			return;
		}

		if (imService != null) {
			mUserName = mUserName.trim();
			mPassword = mPassword.trim();
			if(!isLogining){
				imService.getLoginManager().login(mServer,mUserName, mPassword);
				findViewById(R.id.sign_in_button).setEnabled(false);
			}
			showProgress(true);
		}
	}

	private void showProgress(final boolean show) {
		if (show) {
			mLoginStatusView.setVisibility(View.VISIBLE);
		} else { 
			mLoginStatusView.setVisibility(View.GONE);
		}
	}

	private void initListener() {
		// TODO Auto-generated method stub
		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						intputManager.hideSoftInputFromWindow(
								mEditPassWord.getWindowToken(), 0);
						attemptLogin();
					}
				});
	}

	// 为什么会有两个这个
	// 可能是 兼容性的问题 导致两种方法onBackPressed
	@Override
	public void onBackPressed() {
		logger.d("login#onBackPressed");
		// imLoginMgr.cancel();
		// TODO Auto-generated method stub
		super.onBackPressed();
	}

	/**
	 * ----------------------------event 事件驱动----------------------------
	 */
	public void onEventMainThread(LoginEvent event) {
		switch (event) {
		case LOGINING:
			isLogining = true;
			break;
		case LOCAL_LOGIN_SUCCESS:
		case LOGIN_OK:
			isLogining = false;
			onLoginSuccess();
			break;
		case LOGIN_AUTH_FAILED:
		case LOGIN_INNER_FAILED:
			isLogining = false;
			if (!loginSuccess)
				onLoginFailure(event);
			break;
		default:
			break;
		}
	}

	public void onEventMainThread(SocketEvent event) {
		switch (event) {
		case CONNECT_MSG_SERVER_FAILED:
			if (!loginSuccess)
				onSocketFailure(event);
			break;
		default:
			break;
		}
	}

	private void onLoginSuccess() {
		logger.i("login#onLoginSuccess");
		loginSuccess = true;
		showProgress(false);
		Intent intent = new Intent(LoginActivity.this, MainActivity.class);
		startActivity(intent);
		LoginActivity.this.finish();
	}

	private void onLoginFailure(LoginEvent event) {
		findViewById(R.id.sign_in_button).setEnabled(true);
		logger.e("login#onLoginError -> errorCode:%s", event.name());
		showLoginPage();
		String errorTip = getString(IMUIHelper.getLoginErrorTip(event));
		logger.d("login#errorTip:%s", errorTip);
		mLoginStatusView.setVisibility(View.GONE);
		Toast.makeText(this, errorTip, Toast.LENGTH_SHORT).show();
	}

	private void onSocketFailure(SocketEvent event) {
		findViewById(R.id.sign_in_button).setEnabled(true);
		logger.e("login#onLoginError -> errorCode:%s,", event.name());
		showLoginPage();
		String errorTip = getString(IMUIHelper.getSocketErrorTip(event));
		logger.d("login#errorTip:%s", errorTip);
		mLoginStatusView.setVisibility(View.GONE);
		Toast.makeText(this, errorTip, Toast.LENGTH_SHORT).show();
	}
}