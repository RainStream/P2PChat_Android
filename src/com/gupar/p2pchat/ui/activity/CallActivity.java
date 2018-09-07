/*
 * libjingle
 * Copyright 2015 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gupar.p2pchat.ui.activity;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.appspot.apprtc.AppRTCAudioManager;
import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.UnhandledExceptionHandler;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.VideoRendererGui.ScalingType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.gupar.p2pchat.R;
import com.gupar.p2pchat.imservice.event.MediaEvent;
import com.gupar.p2pchat.utils.Constants;

import de.greenrobot.event.EventBus;

/**
 * Activity for peer connection call setup, call waiting and call view.
 */
public class CallActivity extends Activity implements
		AppRTCClient.SignalingEvents,
		PeerConnectionClient.PeerConnectionEvents, CallFragment.OnCallEvents {

	public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
	public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
	public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
	public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
	public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
	public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
	public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
	public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
	public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
	public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
	public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
	public static final String EXTRA_NOAUDIOPROCESSING_ENABLED = "org.appspot.apprtc.NOAUDIOPROCESSING";
	public static final String EXTRA_CPUOVERUSE_DETECTION = "org.appspot.apprtc.CPUOVERUSE_DETECTION";
	public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
	public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
	public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
	private static final String TAG = "CallRTCClient";
	
	// List of mandatory application permissions.
	private static final String[] MANDATORY_PERMISSIONS = {
			"android.permission.MODIFY_AUDIO_SETTINGS",
			"android.permission.RECORD_AUDIO", "android.permission.INTERNET" };
	
	// Peer connection statistics callback period in ms.
	private static final int STAT_CALLBACK_PERIOD = 1000;
	// Local preview screen position before call is connected.
	private static final int LOCAL_X_CONNECTING = 0;
	private static final int LOCAL_Y_CONNECTING = 0;
	private static final int LOCAL_WIDTH_CONNECTING = 100;
	private static final int LOCAL_HEIGHT_CONNECTING = 100;
	// Local preview screen position after call is connected.
	private static final int LOCAL_X_CONNECTED = 72;
	private static final int LOCAL_Y_CONNECTED = 72;
	private static final int LOCAL_WIDTH_CONNECTED = 25;
	private static final int LOCAL_HEIGHT_CONNECTED = 25;
	// Remote video screen position
	private static final int REMOTE_X = 0;
	private static final int REMOTE_Y = 0;
	private static final int REMOTE_WIDTH = 100;
	private static final int REMOTE_HEIGHT = 100;

	private PeerConnectionClient peerConnectionClient = null;

	private SignalingParameters signalingParameters;
	private AppRTCAudioManager audioManager = null;
	private VideoRenderer.Callbacks localRender;
	private VideoRenderer.Callbacks remoteRender;
	private ScalingType scalingType;
	private Toast logToast;
	private boolean commandLineRun;
	private int runTimeMs;
	private boolean activityRunning;
	private boolean initiator;
	private int peer_id;
	private PeerConnectionParameters peerConnectionParameters;
	private boolean iceConnected;
	private boolean isError;
	private boolean callControlFragmentVisible = true;
	private long callStartedTimeMs = 0;
	// Controls
	private GLSurfaceView videoView;
	CallFragment callFragment;
	HudFragment hudFragment;

	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(
				this));

		EventBus.getDefault().register(this);
		
		// Set window styles for fullscreen-window size. Needs to be done before
		// adding content.
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().addFlags(
	        LayoutParams.FLAG_FULLSCREEN
	        | LayoutParams.FLAG_KEEP_SCREEN_ON
	        | LayoutParams.FLAG_DISMISS_KEYGUARD
	        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
	        | LayoutParams.FLAG_TURN_SCREEN_ON);
	    getWindow().getDecorView().setSystemUiVisibility(
	        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	        | View.SYSTEM_UI_FLAG_FULLSCREEN
	        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	    setContentView(R.layout.activity_call);		
		
		iceConnected = false;
		signalingParameters = null;
		scalingType = ScalingType.SCALE_ASPECT_FILL;

		// Create UI controls.
		videoView = (GLSurfaceView) findViewById(R.id.glview_call);
		callFragment = new CallFragment();
		hudFragment = new HudFragment();

		// Create video renderers.
		VideoRendererGui.setView(videoView, new Runnable() {
			@Override
			public void run() {
				createPeerConnectionFactory();
			}
		});
		remoteRender = VideoRendererGui.create(REMOTE_X, REMOTE_Y,
				REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
		localRender = VideoRendererGui.create(LOCAL_X_CONNECTING,
				LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
				LOCAL_HEIGHT_CONNECTING, scalingType, true);

	    // Show/hide call control fragment on view click.
	    videoView.setOnClickListener(new View.OnClickListener() {
	      @Override
	      public void onClick(View view) {
	        toggleCallControlFragmentVisibility();
	      }
	    });
	    
	    
	    // Check for mandatory permissions.
	    for (String permission : MANDATORY_PERMISSIONS) {
	      if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
	        logAndToast("Permission " + permission + " is not granted");
	        setResult(RESULT_CANCELED);
	        finish();
	        return;
	      }
	    }

		// Get Intent parameters.
		final Intent intent = getIntent();

		boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
		initiator = intent.getBooleanExtra("initiator", true);
		peer_id = intent.getIntExtra("peer_id", 0);
		
	    peerConnectionParameters = new PeerConnectionParameters(
	            intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
	            loopback,
	            intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0),
	            intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0),
	            intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
	            intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0),
	            intent.getStringExtra(EXTRA_VIDEOCODEC),
	            intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
	            intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
	            intent.getStringExtra(EXTRA_AUDIOCODEC),
	            intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
	            intent.getBooleanExtra(EXTRA_CPUOVERUSE_DETECTION, true));
		commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
		runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

		// Create connection client and connection parameters.
		startCall();

		// Send intent arguments to fragments.
		callFragment.setArguments(intent.getExtras());
		hudFragment.setArguments(intent.getExtras());
		// Activate call and HUD fragments and start the call.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.call_fragment_container, callFragment);
		ft.add(R.id.hud_fragment_container, hudFragment);
		ft.commit();

		// For command line execution run connection for <runTimeMs> and exit.
		if (commandLineRun && runTimeMs > 0) {
			videoView.postDelayed(new Runnable() {
				public void run() {
					disconnect();
				}
			}, runTimeMs);
		}
	}

	// Activity interfaces
	@Override
	public void onPause() {
		super.onPause();
		videoView.onPause();
		activityRunning = false;
		if (peerConnectionClient != null) {
			peerConnectionClient.stopVideoSource();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		videoView.onResume();
		activityRunning = true;
		if (peerConnectionClient != null) {
			peerConnectionClient.startVideoSource();
		}
	}

	@Override
	protected void onDestroy() {
		disconnect();
		super.onDestroy();

		EventBus.getDefault().unregister(this);

		if (logToast != null) {
			logToast.cancel();
		}
		activityRunning = false;
	}

	// CallFragment.OnCallEvents interface implementation.
	@Override
	public void onCallHangUp() {
		disconnect();
	}

	@Override
	public void onCameraSwitch() {
		if (peerConnectionClient != null) {
			peerConnectionClient.switchCamera();
		}
	}

	@Override
	public void onVideoScalingSwitch(ScalingType scalingType) {
		this.scalingType = scalingType;
		updateVideoView();
	}

	// Helper functions.
	@SuppressLint("NewApi")
	private void toggleCallControlFragmentVisibility() {
	    if (!iceConnected || !callFragment.isAdded()) {
	        return;
	      }
	      // Show/hide call control fragment
	      callControlFragmentVisible = !callControlFragmentVisible;
	      FragmentTransaction ft = getFragmentManager().beginTransaction();
	      if (callControlFragmentVisible) {
	        ft.show(callFragment);
	        ft.show(hudFragment);
	      } else {
	        ft.hide(callFragment);
	        ft.hide(hudFragment);
	      }
	      ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
	      ft.commit();
	}

	private void updateVideoView() {
		VideoRendererGui.update(remoteRender, REMOTE_X, REMOTE_Y, REMOTE_WIDTH,
				REMOTE_HEIGHT, scalingType, false);
		if (iceConnected) {
			VideoRendererGui.update(localRender, LOCAL_X_CONNECTED,
					LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED,
					LOCAL_HEIGHT_CONNECTED, ScalingType.SCALE_ASPECT_FIT, true);
		} else {
			VideoRendererGui.update(localRender, LOCAL_X_CONNECTING,
					LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING,
					LOCAL_HEIGHT_CONNECTING, scalingType, true);
		}
	}

	private void startCall() {

		callStartedTimeMs = System.currentTimeMillis();

		// Create and audio manager that will take care of audio routing,
		// audio modes, audio device enumeration etc.
		audioManager = AppRTCAudioManager.create(this, new Runnable() {
			// This method will be called each time the audio state (number and
			// type of devices) has been changed.
			@Override
			public void run() {
				onAudioManagerChangedState();
			}
		});
		// Store existing audio settings and change audio mode to
		// MODE_IN_COMMUNICATION for best possible VoIP performance.
		Log.d(TAG, "Initializing the audio manager...");
		audioManager.init();

		// Start peer connection.

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (initiator) {
					RequestVideo();
				}
				else{
					RespondVideo();
				}
			}
		});
	}

	// Should be called from UI thread
	private void callConnected() {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		Log.i(TAG, "Call connected: delay=" + delta + "ms");

		// Update video view.
		updateVideoView();
		// Enable statistics callback.
		peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
	}

	private void onAudioManagerChangedState() {
		// TODO(henrika): disable video if
		// AppRTCAudioManager.AudioDevice.EARPIECE
		// is active.
	}

	// Create peer connection factory when EGL context is ready.
	private void createPeerConnectionFactory() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (peerConnectionClient == null) {
					final long delta = System.currentTimeMillis()
							- callStartedTimeMs;
					Log.d(TAG, "Creating peer connection factory, delay="
							+ delta + "ms");
					peerConnectionClient = PeerConnectionClient.getInstance();
					peerConnectionClient.createPeerConnectionFactory(
							CallActivity.this,
							VideoRendererGui.getEGLContext(),
							peerConnectionParameters, CallActivity.this);
				}
				  if (signalingParameters != null) {
			          Log.w(TAG, "EGL context is ready after room connection.");
			          onConnectedToPeerInternal(signalingParameters);
			        }
			}
		});
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnect() {
		if (activityRunning) {
			SendMessage("BYE");
			activityRunning = false;
		}
		if (peerConnectionClient != null) {
			peerConnectionClient.close();
			peerConnectionClient = null;
		}
		if (audioManager != null) {
			audioManager.close();
			audioManager = null;
		}
		if (iceConnected && !isError) {
			setResult(RESULT_OK);
		} else {
			setResult(RESULT_CANCELED);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				StopVideo();
			}
		});

		finish();
	}

	private void disconnectWithErrorMessage(final String errorMessage) {
		if (commandLineRun || !activityRunning) {
			Log.e(TAG, "Critical error: " + errorMessage);
			disconnect();
		} else {
			new AlertDialog.Builder(this)
					.setTitle(getText(R.string.channel_error_title))
					.setMessage(errorMessage)
					.setCancelable(false)
					.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
									disconnect();
								}
							}).create().show();
		}
	}

	// Log |msg| and Toast about it.
	private void logAndToast(String msg) {
		Log.d(TAG, msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}

	private void reportError(final String description) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isError) {
					isError = true;
					disconnectWithErrorMessage(description);
				}
			}
		});
	}

	// -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
	// All callbacks are invoked from websocket signaling looper thread and
	// are routed to UI thread.
	private void onConnectedToPeerInternal(final SignalingParameters params) {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;

		signalingParameters = params;
		if (peerConnectionClient == null) {
			Log.w(TAG, "Room is connected, but EGL context is not ready yet.");
			return;
		}

		signalingParameters.iceServers.add(new IceServer(Constants.STUN_SERVER));

		logAndToast("Creating peer connection, delay=" + delta + "ms");
		peerConnectionClient.createPeerConnection(localRender, remoteRender,
				signalingParameters);
		
		if (signalingParameters.initiator) {
			logAndToast("Creating OFFER...");
			// Create offer. Offer SDP will be sent to answering client in
			// PeerConnectionEvents.onLocalDescription event.
			peerConnectionClient.createOffer();
		} else {
			if (params.offerSdp != null) {
				peerConnectionClient.setRemoteDescription(params.offerSdp);
				logAndToast("Creating ANSWER...");
				// Create answer. Answer SDP will be sent to offering client in
				// PeerConnectionEvents.onLocalDescription event.
				peerConnectionClient.createAnswer();
			}
			if (params.iceCandidates != null) {
				// Add remote ICE candidates from room.
				for (IceCandidate iceCandidate : params.iceCandidates) {
					peerConnectionClient.addRemoteIceCandidate(iceCandidate);
				}
			}
		}
	}

	@Override
	public void onRemoteDescription(final SessionDescription sdp) {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (peerConnectionClient == null) {
					Log.e(TAG,"Received remote SDP for non-initilized peer connection.");
					return;
				}
				logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
				peerConnectionClient.setRemoteDescription(sdp);
				if (!signalingParameters.initiator) {
					logAndToast("Creating ANSWER...");
					// Create answer. Answer SDP will be sent to offering client
					// in
					// PeerConnectionEvents.onLocalDescription event.
					peerConnectionClient.createAnswer();
				}
			}
		});
	}

	@Override
	public void onRemoteIceCandidate(final IceCandidate candidate) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (peerConnectionClient == null) {
					Log.e(TAG,
							"Received ICE candidate for non-initilized peer connection.");
					return;
				}
				peerConnectionClient.addRemoteIceCandidate(candidate);
			}
		});
	}

	@Override
	public void onChannelClose() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logAndToast("Remote end hung up; dropping PeerConnection");
				disconnect();
			}
		});
	}

	@Override
	public void onChannelError(final String description) {
		reportError(description);
	}

	// -----Implementation of
	// PeerConnectionClient.PeerConnectionEvents.---------
	// Send local peer connection SDP and ICE candidates to remote party.
	// All callbacks are invoked from peer connection client looper thread and
	// are routed to UI thread.
	@Override
	public void onLocalDescription(final SessionDescription sdp) {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
				if (signalingParameters.initiator) {
					JSONObject json = new JSONObject();
					jsonPut(json, "sdp", sdp.description);
					jsonPut(json, "type", "offer");
					SendMessage(json.toString());
				} else {
					JSONObject json = new JSONObject();
					jsonPut(json, "sdp", sdp.description);
					jsonPut(json, "type", "answer");
					SendMessage(json.toString());
				}
			}
		});
	}

	// Put a |key|->|value| mapping in |json|.
	private static void jsonPut(JSONObject json, String key, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onIceCandidate(final IceCandidate candidate) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				JSONObject json = new JSONObject();
				jsonPut(json, "type", "candidate");
				jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);// label
				jsonPut(json, "sdpMid", candidate.sdpMid);// id
				jsonPut(json, "candidate", candidate.sdp);
				SendMessage(json.toString());
			}
		});
	}

	@Override
	public void onIceConnected() {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logAndToast("ICE connected, delay=" + delta + "ms");
				iceConnected = true;
				callConnected();
			}
		});
	}

	@Override
	public void onIceDisconnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logAndToast("ICE disconnected");
				iceConnected = false;
				disconnect();
			}
		});
	}

	@Override
	public void onPeerConnectionClosed() {
	}

	@Override
	public void onPeerConnectionStatsReady(final StatsReport[] reports) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isError && iceConnected) {
					hudFragment.updateEncoderStatistics(reports);
				}
			}
		});
	}

	@Override
	public void onPeerConnectionError(final String description) {
		reportError(description);
	}

	private void RespondVideo() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				List<PeerConnection.IceServer> list = new ArrayList<PeerConnection.IceServer>();
				List<IceCandidate> listIce = new ArrayList<IceCandidate>();
				SignalingParameters param = new SignalingParameters(list,
						initiator, null, listIce);
				onConnectedToPeerInternal(param);
			}
		});
	}
	
	private void RejectedVideo(){
		logAndToast("对方未接听");
		disconnect();
	}

	/**
	 * ----------------------------event 事件驱动----------------------------
	 */
	public void onEventMainThread(MediaEvent event) {
		String msg = event.getMsg();
		switch (event.getEvent()) {
		case RESPONCE_CONNECT_TO_PEER:
			RespondVideo();
			break;
		case REJECT_CONNECT_TO_PEER:
			RejectedVideo();
			break;
		case RECV_CONNECT_MSG:
			onMessage(msg);
			break;
		default:
			break;
		}
	}

	private void RequestVideo() {
		MediaEvent event = new MediaEvent(MediaEvent.Event.REQUEST_CONNECT_TO_PEER);
		event.setPeerId(peer_id);
		EventBus.getDefault().postSticky(event);
	}

	private void StopVideo() {
		MediaEvent event = new MediaEvent(MediaEvent.Event.DIS_CONNECT_TO_PEER);
		event.setPeerId(peer_id);
		EventBus.getDefault().postSticky(event);
	}

	private void SendMessage(final String msg) {
		MediaEvent event = new MediaEvent(MediaEvent.Event.PUSH_CONNECT_MSG);
		event.setPeerId(peer_id);
		event.setMsg(msg);
		EventBus.getDefault().postSticky(event);
	}

	@SuppressLint("NewApi")
	private void onMessage(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (msg.equals("BYE")) {
						onChannelClose();
						return;
					}

					JSONObject json = new JSONObject(msg);
					String type = (String) json.get("type");
					if (type.equals("candidate")) {
						IceCandidate candidate = new IceCandidate(
								(String) json.get("sdpMid"),
								json.getInt("sdpMLineIndex"),
								(String) json.get("candidate"));
						onRemoteIceCandidate(candidate);
					} else if (type.equals("answer")) {
						if (signalingParameters.initiator) {
							SessionDescription sdp = new SessionDescription(
									SessionDescription.Type.fromCanonicalForm(type),
									json.getString("sdp"));
							onRemoteDescription(sdp);
						} else {
							reportError("Received answer for call initiator: "+ msg);
						}
					} else if (type.equals("offer")) {
						if (!signalingParameters.initiator) {
							SessionDescription sdp = new SessionDescription(
									SessionDescription.Type.fromCanonicalForm(type),
									json.getString("sdp"));
							onRemoteDescription(sdp);
						} else {
							reportError("Received offer for call receiver: "+ msg);
						}
					} else {
						reportError("Unexpected WebSocket message: " + msg);
					}
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
