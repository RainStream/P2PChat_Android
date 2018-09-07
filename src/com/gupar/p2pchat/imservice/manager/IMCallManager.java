package com.gupar.p2pchat.imservice.manager;

import com.gupar.p2pchat.utils.Logger;

public class IMCallManager extends IMManager {
	private Logger logger = Logger.getLogger(IMLoginManager.class);

	private static IMCallManager inst = new IMCallManager();

	public static IMCallManager instance() {
		return inst;
	}

	public IMCallManager() {
		logger.d("call#creating IMCallManager");
	}

	IMSocketManager imSocketManager = IMSocketManager.instance();

	@Override
	public void doOnStart() {
		

	}

	@Override
	public void reset() {
		
	}

	public void RequestVideo(int peer_id) {
		imSocketManager.RequestVideo(peer_id);
	}
	public void  AllowVideo(int peer_id) {
		imSocketManager.AllowVideo(peer_id);
	}
	
	public void RejectVideo(int peer_id) {
		imSocketManager.RejectVideo(peer_id);
	}
	
	public void StopVideo(int peer_id) {
		imSocketManager.StopVideo(peer_id);
	}

	public void sendToPeer(int peer_id,String msg) {
		imSocketManager.sendToPeer(peer_id,msg);
	}

}
