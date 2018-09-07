package com.gupar.p2pchat.imservice.manager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import com.gupar.p2pchat.imservice.event.SocketEvent;
import com.gupar.p2pchat.imservice.network.MsgServerHandler;
import com.gupar.p2pchat.imservice.network.SocketThread;
import com.gupar.p2pchat.imservice.packager.cPacketizer;
import com.gupar.p2pchat.utils.Constants;
import com.gupar.p2pchat.utils.Logger;

import android.annotation.SuppressLint;
import de.greenrobot.event.EventBus;

/**
 * @author : yingmu on 14-12-30.
 * @email : yingmu@mogujie.com.
 * 
 *        业务层面: 长连接建立成功之后，就要发送登陆信息，否则15s之内就会断开 所以connMsg 与 login是强耦合的关系
 */
public class IMSocketManager extends IMManager {

	private Logger logger = Logger.getLogger(IMSocketManager.class);
	private static IMSocketManager inst = new IMSocketManager();
	private String loginServer;

	public static IMSocketManager instance() {
		return inst;
	}

	public IMSocketManager() {
		logger.d("login#creating IMSocketManager");
	}

	/** 底层socket */
	private SocketThread msgServerThread;

	/** 自身状态 */
	private SocketEvent socketStatus = SocketEvent.NONE;

	/**
	 * 获取Msg地址，等待链接
	 */
	@Override
	public void doOnStart() {
		socketStatus = SocketEvent.NONE;
	}

	// todo check
	@Override
	public void reset() {
		/*
		 * disconnectMsgServer(); socketStatus = SocketEvent.NONE;
		 * currentMsgAddress = null;
		 */
	}

	public static String MD5(String str) {
		try {
			// 获得MD5摘要算法的 MessageDigest 对象
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			char[] charArray = str.toCharArray();
			byte[] byteArray = new byte[charArray.length];

			for (int i = 0; i < charArray.length; i++)
				byteArray[i] = (byte) charArray[i];
			byte[] md5Bytes = md5.digest(byteArray);
			StringBuffer hexValue = new StringBuffer();
			for (int i = 0; i < md5Bytes.length; i++) {
				int val = ((int) md5Bytes[i]) & 0xff;
				if (val < 16)
					hexValue.append("0");
				hexValue.append(Integer.toHexString(val));
			}

			return hexValue.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 实现自身的事件驱动
	 * 
	 * @param event
	 */
	public void triggerEvent(SocketEvent event) {
		// setSocketStatus(event);
		EventBus.getDefault().postSticky(event);
	}

	/**
	 * -------------------------------功能方法--------------------------------------
	 */

	/*
	 * public void sendRequest(GeneratedMessageLite requset,int sid,int cid){
	 * sendRequest(requset,sid,cid,null); }
	 *//**
	 * todo check exception
	 * */
	public void sendRequest(cPacketizer requset) {
		try {
			// 组装包头 header
			byte[] bytes = requset.getData();
			msgServerThread.sendRequest(bytes);
		} catch (Exception e) {
			logger.e("#sendRequest#channel is close!");
		}
	}

	public void packetDispatch(byte[] bytes)
			throws UnsupportedEncodingException {
		IMPacketDispatcher.msgPacketDispatcher(bytes);
	}

	public void RequestVideo(int peer_id) {
		cPacketizer pkg = new cPacketizer(0x11);// Packet type
		pkg.WriteVarInt32(peer_id);
		pkg.WriteVarInt32(0);// 请求视频
		sendRequest(pkg);
	}

	public void AllowVideo(int peer_id) {
		cPacketizer pkg = new cPacketizer(0x11);// Packet type
		pkg.WriteVarInt32(peer_id);
		pkg.WriteVarInt32(1);// 同意视频
		sendRequest(pkg);
	}

	public void RejectVideo(int peer_id) {
		cPacketizer pkg = new cPacketizer(0x11);// Packet type
		pkg.WriteVarInt32(peer_id);
		pkg.WriteVarInt32(-1);// 拒绝视频
		sendRequest(pkg);
	}

	public void StopVideo(int peer_id) {
		// cPacketizer pkg = new cPacketizer(0x21);// Packet type
		// pkg.WriteVarInt32(0);
		// sendRequest(pkg);
	}

	public void sendToPeer(int peer_id, String msg) {
		cPacketizer pkg = new cPacketizer(0x12);// Packet type
		pkg.WriteVarInt32(peer_id);
		pkg.WriteString(msg);
		sendRequest(pkg);
	}

	/**
	 * 新版本流程如下 1.客户端通过域名获得login_server的地址 2.客户端通过login_server获得msg_serv的地址
	 * 3.客户端带着用户名密码对msg_serv进行登录 4.msg_serv转给db_proxy进行认证（do not care on client）
	 * 5.将认证结果返回给客户端
	 */
	public void reqConnectServer(String server) {
		loginServer = server;
		logger.d("socket#reqMsgServerAddrs.");
		connectMsgServer();
	}

	@SuppressLint("DefaultLocale")
	public void LoginAllow(String username, String password) {
		logger.d("socket#reqMsgServerAddrs.");

		password = MD5(password).toLowerCase();

		cPacketizer pkg = new cPacketizer(0x00);// Packet type
		pkg.WriteString(username);
		pkg.WriteString(password);
		sendRequest(pkg);
	}

	/**
	 * 与登陆login是强耦合的关系
	 */
	@SuppressLint("NewApi")
	private void connectMsgServer() {
		triggerEvent(SocketEvent.CONNECTING_MSG_SERVER);

		String priorIP = loginServer;
		if(priorIP.isEmpty()){
			priorIP = Constants.SERVER_IP;
		}

		int port = Constants.SERVER_PORT;
		logger.i("login#connectMsgServer -> (%s:%d)", priorIP, port);

		// check again,may be unimportance
		if (msgServerThread != null) {
			msgServerThread.close();
			msgServerThread = null;
		}

		msgServerThread = new SocketThread(priorIP, port,
				new MsgServerHandler());
		msgServerThread.start();
	}

	public void reconnectMsg() {
		synchronized (IMSocketManager.class) {
			connectMsgServer();
		}
	}

	/**
	 * 断开与msg的链接
	 */
	public void disconnectMsgServer() {
		logger.i("login#disconnectMsgServer");
		if (msgServerThread != null) {
			msgServerThread.close();
			msgServerThread = null;
			logger.i("login#do real disconnectMsgServer ok");
		}
	}

	/** 判断链接是否处于断开状态 */
	/*
	 * public boolean isSocketConnect(){ if(msgServerThread == null ||
	 * msgServerThread.isClose()){ return false; } return true; }
	 */
	public void onMsgServerConnected() {

		// 清空接收缓存
		IMPacketDispatcher.clearBuffers();

		logger.i("login#onMsgServerConnected");
		triggerEvent(SocketEvent.CONNECT_MSG_SERVER_SUCCESS);

		cPacketizer pkg = new cPacketizer(0x00);
		pkg.WriteString("p2pchat");
		pkg.WriteString(Constants.SERVER_IP);
		pkg.WriteBEInt32(Constants.SERVER_PORT);
		pkg.WriteBEInt32(2);
		sendRequest(pkg);
	}

	/**
	 * 1. kickout 被踢出会触发这个状态 -- 不需要重连 2. 心跳包没有收到 会触发这个状态 -- 链接断开，重连 3. 链接主动断开 --
	 * 重连 之前的长连接状态 connected
	 */
	// 先断开链接
	// only 2 threads(ui thread, network thread) would request sending packet
	// let the ui thread to close the connection
	// so if the ui thread has a sending task, no synchronization issue
	public void onMsgServerDisconn() {
		logger.w("login#onMsgServerDisconn");
		disconnectMsgServer();
		triggerEvent(SocketEvent.MSG_SERVER_DISCONNECTED);
	}

	/** 之前没有连接成功 */
	public void onConnectMsgServerFail() {
		triggerEvent(SocketEvent.CONNECT_MSG_SERVER_FAILED);
	}
}
