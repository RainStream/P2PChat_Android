package com.gupar.p2pchat.imservice.event;


/**
 * @author : ycj on 15-11-17.
 * @email : gupar@qq.com.
 * 
 */
public class MediaEvent {
	private Event event;
	private String msg;
	private int peer_id;
	private int code;
	
	public MediaEvent(Event event) {
		this.event = event;
	}

	public enum Event {
		NONE,
		RECV__CONNECT_TO_PEER,//收到对方音视频请求
		REQUEST_CONNECT_TO_PEER,//请求音视频连接
		RESPONCE_CONNECT_TO_PEER,//响应音视频连接
		REJECT_CONNECT_TO_PEER,//拒绝音视频连接
		DIS_CONNECT_TO_PEER,//中断音视频连接
		RECV_CONNECT_MSG,//音视频通讯协议
		PUSH_CONNECT_MSG,//推送音视频消息
		PUSH_DEVICE_STSTUS,//推送设备状态
		PUSH_MONITOR_STATUS,//推送监控状态
		PUSH_REQUEST_IMAGE,//推送拉取图像信号
		OTher
	}
	
	public int getPeerId() {
		return this.peer_id;
	}
	
	public void setPeerId(int id) {
		this.peer_id = id;
	}

	public Event getEvent() {
		return event;
	}
	
	public String getMsg() {
		return this.msg;
	}
	
	public int getCode() {
		return this.code;
	}

	public void setEvent(Event event) {
		this.event = event;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
}
