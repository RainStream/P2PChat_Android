package com.gupar.p2pchat.imservice.manager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import de.greenrobot.event.EventBus;
import android.annotation.SuppressLint;

import com.gupar.p2pchat.imservice.event.MediaEvent;
import com.gupar.p2pchat.imservice.event.UserEvent;
import com.gupar.p2pchat.imservice.packager.cByteBuffer;
import com.gupar.p2pchat.imservice.packager.cPacketizer;
import com.gupar.p2pchat.utils.ErrorCode;
import com.gupar.p2pchat.utils.Logger;


public class IMPacketDispatcher {
	private static Logger logger = Logger.getLogger(IMPacketDispatcher.class);
	private static final cByteBuffer mBuffer = new cByteBuffer(1024 * 16);
	private static int m_State = 1;/** State of the protocol. 1 = status, 2 = login, 3 = work */
	
	IMPacketDispatcher(){
		logger.d("packet#creating IMPacketDispatcher");
	}
	
	public static void clearBuffers(){
		mBuffer.ClearAll();		
		m_State = 1;
	}
	
	public static boolean msgPacketDispatcher(byte []bytebuf) throws UnsupportedEncodingException{
		if(!mBuffer.Write(bytebuf, bytebuf.length))
			return false;
		
		for(;;){
			int PacketLen = mBuffer.ReadVarInt();
			if (PacketLen == 0){
				// Not enough data
				mBuffer.ResetRead();
				break;
			}
			if (!mBuffer.CanReadBytes(PacketLen))
			{
				// The full packet hasn't been received yet
				mBuffer.ResetRead();
				break;
			}
			
			byte []datas = new byte[PacketLen];
			mBuffer.ReadBuf(datas, PacketLen);
			mBuffer.CommitRead();
			if (!HandPacket(datas))
				return false;
		}
		return true;
	}
	
	static void SendPacket(cPacketizer pkg){
		IMSocketManager.instance().sendRequest(pkg);
	}
	
	static boolean HandPacket(byte[] msg){
		cByteBuffer bb = new cByteBuffer(msg.length);
		bb.Write(msg, msg.length);
	
		int type = bb.ReadVarInt();
		switch (m_State) {
		case 1: {
			// Status
			switch (type) {
			case 0x00:
				HandlePacketStatusRequest(bb);
				return true;
			}
			break;
		}
		case 2: {
			// Login
			switch (type) {
			case 0x00:
				HandlePacketLoginDisconnect(bb);
				return true;
			case 0x01:
				HandlePacketLoginSuccess(bb);
				return true;
			}
			break;
		}

		case 3: {
			// Work
			switch (type) {
			case 0x00: HandlePacketKeepAlive(bb); return true;
			case 0x01: HandlePacketUserInfo(bb); return true;			
			case 0x11: HandlePacketMedia(bb); return true;
			case 0x12: HandlePacketMediaMsg(bb); return true;
			case 0x40: HandlePacketErrorCode(bb); return true;
			}
			break;
		}
		default:
			break;
		}
		return false;
	}
	
	
	@SuppressLint("NewApi")
	static void HandlePacketStatusRequest(cByteBuffer a_ByteBuffer) {
		int state = a_ByteBuffer.ReadBEInt32();
		String strIP = a_ByteBuffer.ReadVarUTF8String();
		if (state > 3 || state < 1) {
			return;
		}
		
		m_State = state;
		
		IMLoginManager.instance().loginAllow();

	}

	static void HandlePacketLoginDisconnect(cByteBuffer a_ByteBuffer) {
		m_State = 1;
		
		IMLoginManager.instance().onLoginFailed();		
		int a_Reason = a_ByteBuffer.ReadBEInt32();		
		HandleErrorCode(a_Reason);
		
	}

	static void HandlePacketLoginSuccess(cByteBuffer a_ByteBuffer) {
		m_State = 3;	
		
		IMLoginManager.instance().onLoginOk();
	}

	static void HandlePacketKeepAlive(cByteBuffer a_ByteBuffer) {
		int KeepAliveID = a_ByteBuffer.ReadVarInt();
		
		cPacketizer pkg = new cPacketizer(0x00);
		pkg.WriteVarInt32(KeepAliveID);
		SendPacket(pkg);
	}
	
	static void HandlePacketUserInfo(cByteBuffer a_ByteBuffer) {
		int peer_id = a_ByteBuffer.ReadVarInt();
		String username = a_ByteBuffer.ReadVarUTF8String();
		int state = a_ByteBuffer.ReadVarInt();	
		
		if(state>3){
			//用户离线
			IMLoginManager.instance().UserOffline(peer_id,username);
		}
		else{
			//用户上线
			IMLoginManager.instance().UserOnline(peer_id,username);
		}
		
		UserEvent.Event ev = UserEvent.Event.USER_UPDATE;
		UserEvent event = new UserEvent(ev);
		EventBus.getDefault().postSticky(event);
	}
	
	static void HandlePacketMedia(cByteBuffer a_ByteBuffer){		
		int peer_id = a_ByteBuffer.ReadVarInt();
		int type = a_ByteBuffer.ReadVarInt();
		
		MediaEvent.Event ev = MediaEvent.Event.NONE;
		
		if(type == 1){
			//接收音视频请求
			ev = MediaEvent.Event.RESPONCE_CONNECT_TO_PEER;
		}
		else if(type == -1){
			//拒绝音视频请求
			ev = MediaEvent.Event.REJECT_CONNECT_TO_PEER;
		}
		else if(type == 0){
			ev = MediaEvent.Event.RECV__CONNECT_TO_PEER;
		}
		else{
			return;
		}
		
		MediaEvent event = new MediaEvent(ev);
		event.setPeerId(peer_id);
		EventBus.getDefault().postSticky(event);
		
	}

	static void HandlePacketMediaMsg(cByteBuffer a_ByteBuffer){
		int peer_id = a_ByteBuffer.ReadVarInt();
		
		String msg = a_ByteBuffer.ReadVarUTF8String();		
		MediaEvent event = new MediaEvent(MediaEvent.Event.RECV_CONNECT_MSG);
		event.setPeerId(peer_id);
		event.setMsg(msg);		
		EventBus.getDefault().postSticky(event);
	}
	
	static void HandlePacketErrorCode(cByteBuffer a_ByteBuffer){
		int a_Reason = a_ByteBuffer.ReadBEInt32();		
		HandleErrorCode(a_Reason);
	}
	
	static String HandleErrorCode(int a_Code){
		String strReason = "";
		switch (a_Code)
		{
		case  ErrorCode.ERROR_CODE_ACCOUNT_NOT_MATCH:
			strReason = "用户名密码不匹配！";
			break;
		case ErrorCode.ERROR_CODE_ACCOUNT_NOT_EXIST:
			strReason = "用户名不存在！";
			break;
		case ErrorCode.ERROR_CODE_PACKET_ERROR:
			strReason = "协议错误！";
			break;
		case ErrorCode.ERROR_CODE_PACKET_UNKONWN:
			strReason = "未知协议！";
			break;
		case ErrorCode.ERROR_CODE_SERVER_BUSY:
			strReason = "服务器繁忙！";
			break;
		case ErrorCode.ERROR_CODE_CLIENT_TIMEOUT:
			strReason = "哦！你操作超时，稍后再试！";
			break;
		case  ErrorCode.ERROR_CODE_ACCOUNT_ALREADY_LOGIN:
			strReason = "相同账号已经登陆！";
			break;
		case ErrorCode.ERROR_CODE_ACCOUNT_OTHER_LOGIN:
			strReason = "相同账号在异地登陆！";
			break;
		case ErrorCode.ERROR_CODE_SERVER_SHUTDOWN:
			strReason = "服务器已经关闭！";
			break;
		case ErrorCode.ERROR_CODE_BAD_APP:
			strReason = "非法客户端！";
			break;
		case  ErrorCode.ERROR_CODE_INVALID_ENCKEYLENGTH:
			strReason = "无效的数据长度！";
			break;
		case  ErrorCode.ERROR_CODE_INVALID_ENCNONCELENGTH:
			strReason = "无效的数据长度！h";
			break;
		case  ErrorCode.ERROR_CODE_HACKED_CLIENT:
			strReason = "黑客非法客户端！";
			break;
		default:
			break;
		}
		
		return strReason;
	}

	static public byte[] intToByteArray(int i) {
		byte[] result = new byte[4];
		result[3] = (byte) ((i >> 24) & 0xFF);
		result[2] = (byte) ((i >> 16) & 0xFF);
		result[1] = (byte) ((i >> 8) & 0xFF);
		result[0] = (byte) (i & 0xFF);
		return result;
	}

	static int ReadVarInt32(ByteBuffer msg) {
		int Value = 0;
		int Shift = 0;
		byte b = 0;
		do {
			b = msg.get();
			Value = Value | (((int) (b & 0x7f)) << Shift);
			Shift += 7;
		} while ((b & 0x80) != 0);
		return Value;
	}

	static String ReadVarUTF8String(ByteBuffer msg) {
		String str = null;
		int nLen = ReadVarInt32(msg);
		byte[] buf = new byte[nLen];
		msg.get(buf);
		try {
			str = new String(buf, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}
}
