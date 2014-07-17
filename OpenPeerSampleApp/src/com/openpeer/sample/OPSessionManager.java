package com.openpeer.sample;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.util.Log;

import com.openpeer.delegates.CallbackHandler;
import com.openpeer.javaapi.CallClosedReasons;
import com.openpeer.javaapi.CallStates;
import com.openpeer.javaapi.ContactStates;
import com.openpeer.javaapi.MessageDeliveryStates;
import com.openpeer.javaapi.OPCall;
import com.openpeer.javaapi.OPCallDelegate;
import com.openpeer.javaapi.OPContact;
import com.openpeer.javaapi.OPConversationThread;
import com.openpeer.javaapi.OPConversationThreadDelegate;
import com.openpeer.javaapi.OPMessage;
import com.openpeer.sample.conversation.ConversationActivity;
import com.openpeer.sdk.app.OPDataManager;
import com.openpeer.sdk.app.OPHelper;
import com.openpeer.sdk.model.OPSession;
import com.openpeer.sdk.model.OPUser;

public class OPSessionManager {
	List<OPSession> mSessions;

	private static OPSessionManager instance;

	public static OPSessionManager getInstance() {
		if (instance == null) {
			instance = new OPSessionManager();
			instance.mSessions = new ArrayList<OPSession>();
		}
		return instance;
	}

	public OPSession addSession(OPSession session) {
		Log.d("test", "add session for thread " + session.getThread().getThreadID() + " window " + session.getCurrentWindowId());
		mSessions.add(session);
		return session;
	}

	/**
	 * Look up existing session for thread. Uses thread id to look up
	 * 
	 * @param thread
	 * @return
	 */
	public OPSession getSessionOfThread(OPConversationThread thread) {
		for (OPSession session : mSessions) {
			// TODO: use windowId to search when specified
			if (session.getThread() != null && thread.getThreadID().equals(session.getThread().getThreadID())) {
				Log.d("test", "found session for thread " + thread.getThreadID() + " sessions " + mSessions.size());
				return session;
			}
		}
		// No existing session for the thread
		return new OPSession(thread);
	}

	/**
	 * Look up the session "for" users. This call use calculated window id to find the session.
	 * 
	 * @param userIDs
	 * @return
	 */
	public OPSession getSessionForUsers(long[] userIDs) {
		for (OPSession session : mSessions) {
			if (session.isForUsers(userIDs)) {
				return session;
			}
		}
		return null;
	}


	/**
	 * Find existing session "including" the users
	 * 
	 * @param ids
	 *            user ids
	 * @return
	 */
	private OPSession getSessionWithUsers(long[] ids) {
		// TODO: implement proper look up
		return getSessionForUsers(ids);
	}

	OPCall mActiveCall;
	// <callId,call>
	Hashtable<String, OPCall> mCalls;

	private OPCallDelegate mBackgroundCallHandler;

	public void onEnteringBackground() {
	}

	public void onEnteringForeground() {
	}

	public void onCallStateChanged(OPCall call, CallStates state) {
		if (OPHelper.getInstance().isAppInBackground()) {
			mBackgroundCallHandler.onCallStateChanged(call, state);
		}
	}

	/**
	 * Application should provide a background call handler to show an notification for incoming call, and all other fany stuff
	 * 
	 * @param delegate
	 */
	public void setBackgroundCallDelegate(OPCallDelegate delegate) {
		mBackgroundCallHandler = delegate;
	}

	public OPCall placeCall(long[] userIDs, boolean audio, boolean video) {
		// long windowId = OPChatWindow.getWindowId(userIDs);
		OPSession session = OPSessionManager.getInstance().getSessionWithUsers(userIDs);
		List<OPUser> users = null;
		if (session == null) {
			// this is user intiiated session
			users = OPDataManager.getDatastoreDelegate().getUsers(userIDs);
			session = new OPSession(users);
			addSession(session);
		} else {
			users = session.getParticipants();
		}

		OPCall call = session.placeCall(users.get(0), audio, video);
		mCalls.put(call.getPeer().getPeerURI(), call);
		return call;
	}

	void init() {
		mCalls = new Hashtable<String, OPCall>();
		OPConversationThreadDelegate threadDelegate = new OPConversationThreadDelegate() {

			@Override
			public void onConversationThreadNew(OPConversationThread conversationThread) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConversationThreadContactsChanged(OPConversationThread conversationThread) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConversationThreadContactStateChanged(OPConversationThread conversationThread, OPContact contact,
					ContactStates state) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConversationThreadMessage(OPConversationThread conversationThread, String messageID) {
				OPMessage message = conversationThread.getMessage(messageID);
				OPSession session = getSessionOfThread(conversationThread);
				session.onMessageReceived(message);
				if (OPApplication.getInstance().isInBackground()) {
					OPNotificationBuilder.showNotificationForMessage(session, message);
				}
			}

			@Override
			public void onConversationThreadMessageDeliveryStateChanged(OPConversationThread conversationThread, String messageID,
					MessageDeliveryStates state) {

			}

			@Override
			public void onConversationThreadPushMessage(OPConversationThread conversationThread, String messageID, OPContact contact) {

			}
		};
		OPCallDelegate callDelegate = new OPCallDelegate() {

			@Override
			public void onCallStateChanged(OPCall call, CallStates state) {
				switch (state) {
				case CallState_Incoming:
					OPContact caller = call.getCaller();
					Log.d("test",
							"OPSessionManager onCallStateChanged " + call.getNativeClsPtr() + " caller " + caller.getNativeClassPointer());
					OPCall currentCall = getOngoingCallForPeer(caller.getPeerURI());
					if (currentCall != null) {
						// TODO: auto answer and swap calls
						Log.d("test", "found existing call.");
						// return;
					}

					mCalls.put(caller.getPeerURI(), call);
					ConversationActivity.launchForIncomingCall(OPApplication.getInstance(), caller.getPeerURI());
					break;
				case CallState_Closed:
					onCallEnd(call);
					break;

				}
			}
		};
		CallbackHandler.getInstance().registerConversationThreadDelegate(threadDelegate);
		if (AppConfig.FEATURE_CALL) {
			CallbackHandler.getInstance().registerCallDelegate(null, callDelegate);
		}
	}

	public OPCall getOngoingCallForPeer(String peerUri) {
		return mCalls.get(peerUri);
	}

	public void onCallEnd(OPCall mCall) {
		mCalls.remove(mCall.getPeer().getPeerURI());
	}

	public void hangupCall(OPCall mCall, CallClosedReasons callclosedreasonUser) {
		mCall.hangup(CallClosedReasons.CallClosedReason_User);
		onCallEnd(mCall);
	}

	public OPUser getPeerUserForCall(OPCall call) {
		OPContact contact = call.getPeer();
		return new OPUser(contact, call.getConversationThread().getIdentityContactList(contact));
	}
}
