package com.gupar.p2pchat.imservice.manager;

import java.util.ArrayList;

import com.gupar.p2pchat.DB.entity.UserEntity;
import com.gupar.p2pchat.imservice.event.LoginEvent;
import com.gupar.p2pchat.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * 很多情况下都是一种权衡 登陆控制
 * 
 * @yingmu
 */
public class IMLoginManager extends IMManager {
	private Logger logger = Logger.getLogger(IMLoginManager.class);
	/** 单例模式 */
	private static IMLoginManager inst = new IMLoginManager();

	public static IMLoginManager instance() {
		return inst;
	}

	public IMLoginManager() {
		logger.d("login#creating IMLoginManager");
	}

	IMSocketManager imSocketManager = IMSocketManager.instance();
	/** 登陆参数 以便重试 */
	private String loginUserName;
	private String loginPwd;
	private String loginServer;
	private int loginId;
	private UserEntity loginInfo;
	/** loginManger 自身的状态 todo 状态太多就采用enum的方式 */
	private boolean identityChanged = false;
	private boolean isKickout = false;
	private boolean isPcOnline = false;
	// 以前是否登陆过，用户重新登陆的判断
	private boolean everLogined = false;
	// 本地包含登陆信息了[可以理解为支持离线登陆了]
	private boolean isLocalLogin = false;
	private LoginEvent loginStatus = LoginEvent.NONE;

	private ArrayList<UserInfo> userlist = new ArrayList<UserInfo>();;
	
	/**
	 * -------------------------------功能方法--------------------------------------
	 */
	@Override
	public void doOnStart() {
	}
	
	public String getLoginUser(){
		return loginUserName;
	}
	
	public class UserInfo{
		public int  peer_id = -1;
		public String  Name = new String();
		
		public UserInfo(int peer_id,String name){
			this.peer_id = peer_id;
			this.Name = name;
		}
	}

	@Override
	public void reset() {
		loginUserName = null;
		loginPwd = null;
		loginId = -1;
		loginInfo = null;
		identityChanged = false;
		isKickout = false;
		isPcOnline = false;
		everLogined = false;
		loginStatus = LoginEvent.NONE;
		isLocalLogin = false;
		userlist = new ArrayList<UserInfo>();
	}

	public void UserOnline(int peer_id, String name) {
		UserInfo info = new UserInfo(peer_id,name);
		
		for(int i=0;i<userlist.size();i++){  
            if(userlist.get(i).peer_id == peer_id){  
            	userlist.remove(i);
            	break;
            }  
        }
		
		userlist.add(info);
	}

	public void UserOffline(int peer_id, String name) {
		for(int i=0;i<userlist.size();i++){  
            if(userlist.get(i).peer_id == peer_id){  
            	userlist.remove(i);
            	break;
            }  
        } 
	}
	
	public ArrayList<UserInfo> getUsers(){
		return userlist;
	}

	/**
	 * 实现自身的事件驱动
	 * 
	 * @param event
	 */
	public void triggerEvent(LoginEvent event) {
		loginStatus = event;
		EventBus.getDefault().postSticky(event);
	}

	/**
	 * if not login, do nothing send logOuting message, so reconnect won't react
	 * abnormally but when reconnect start to work again?use isEverLogined close
	 * the socket send logOuteOk message mainactivity jumps to login page
	 * 
	 */
	public void logOut() {
		logger.d("login#logOut");
		logger.d("login#stop reconnecting");
		// everlogined is enough to stop reconnecting
		everLogined = false;
		isLocalLogin = false;
		imSocketManager.disconnectMsgServer();
		// reqLoginOut();
	}

	/**
	 * 重新请求登陆 IMReconnectManager 1. 检测当前的状态 2. 请求msg server的地址 3. 建立链接 4. 验证登陆信息
	 * 
	 * @return
	 */
	public void relogin() {
//		if (loginSp.getLoginIdentity() == null
//				|| loginSp.getLoginIdentity().getLoginName().equals("")
//				|| loginSp.getLoginIdentity().getPwd().equals("")) {
//			logger.d("reconnect#login#userName or loginPwd is null!!");
//			everLogined = false;
//			loginStatus = LoginEvent.LOGINING;
//			triggerEvent(LoginEvent.LOGINING);
//		} else {
//			logger.d("reconnect#login#relogin");
//			login(loginSp.getLoginIdentity());
//		}
	}

//	// 自动登陆流程
//	public void login(LoginSp.SpLoginIdentity identity) {
//		if (identity == null) {
//			triggerEvent(LoginEvent.LOGIN_AUTH_FAILED);
//			return;
//		}
//		loginUserName = identity.getLoginName();
//		loginPwd = identity.getPwd();
//		identityChanged = false;
//		imSocketManager.reqConnectServer();
//	}

	public void login(String server,String userName, String password) {
		logger.i("login#login -> userName:%s", userName);
		
		loginUserName = userName;
		loginPwd = password;
		identityChanged = true;
		imSocketManager.reqConnectServer(server);
	}

	public void loginAllow() {
		imSocketManager.LoginAllow(loginUserName, loginPwd);
	}

	public void onLoginOk() {
		logger.i("login#onLoginOk");
		everLogined = true;
		isKickout = false;
		triggerEvent(LoginEvent.LOGIN_OK);

		if (identityChanged) {
			// LoginSp.instance().setLoginInfo(loginUserName,loginPwd,loginId);
			identityChanged = false;
		}
	}

	public void onLoginFailed() {
		logger.i("login#onLoginFailed");
		imSocketManager.disconnectMsgServer();
		triggerEvent(LoginEvent.LOGIN_AUTH_FAILED);
	}

	/** ------------------状态的 set get------------------------------ */
	public int getLoginId() {
		return loginId;
	}

	public void setLoginId(int loginId) {
		logger.d("login#setLoginId -> loginId:%d", loginId);
		this.loginId = loginId;
	}

	public boolean isEverLogined() {
		return everLogined;
	}

	public void setEverLogined(boolean everLogined) {
		this.everLogined = everLogined;
	}

	public UserEntity getLoginInfo() {
		return loginInfo;
	}

	public void setLoginInfo(UserEntity loginInfo) {
		this.loginInfo = loginInfo;
	}

	public LoginEvent getLoginStatus() {
		return loginStatus;
	}

	public boolean isKickout() {
		return isKickout;
	}

	public void setKickout(boolean isKickout) {
		this.isKickout = isKickout;
	}

	public boolean isPcOnline() {
		return isPcOnline;
	}

	public void setPcOnline(boolean isPcOnline) {
		this.isPcOnline = isPcOnline;
	}
}