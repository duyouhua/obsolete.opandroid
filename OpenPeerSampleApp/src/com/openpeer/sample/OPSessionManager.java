package com.openpeer.sample;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.text.TextUtils;
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
import com.openpeer.sample.conversation.CallActivity;
import com.openpeer.sample.conversation.CallStatus;
import com.openpeer.sample.push.PushRegistrationManager;
import com.openpeer.sample.push.PushResult;
import com.openpeer.sample.push.PushToken;
import com.openpeer.sample.push.UAPushProviderImpl;
import com.openpeer.sdk.app.OPDataManager;
import com.openpeer.sdk.app.OPHelper;
import com.openpeer.sdk.model.OPSession;
import com.openpeer.sdk.model.OPUser;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OPSessionManager {
    static final String TAG = OPSessionManager.class.getSimpleName();
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
            if (session.isForThread(thread)) {
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
     * @param ids user ids
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
                if (message.getFrom().isSelf()) {
                    Log.e("test", "Weird! received message from myself!" + message.getMessageId() + " messageId " + messageID + " type "
                            + message.getMessageType());
                    return;
                }
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
                final OPMessage message = conversationThread.getMessage(messageID);

                if (TextUtils.isEmpty(message.getMessageId())) {
                    Log.e("test", "weird! message id is empty " + message);
                    message.setMessageId(messageID);
                }

                PushRegistrationManager.getInstance().getDeviceToken(contact.getPeerURI(), new Callback<PushToken>() {

                    @Override
                    public void success(PushToken token, Response response) {
                        Log.e("test", "onConversationThreadPushMessage push message " + message);
                        new UAPushProviderImpl().pushMessage(message, token, new Callback<PushResult>() {
                            @Override
                            public void success(PushResult pushResult, Response response) {

                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Log.e(TAG, "eror pushing message " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "eror retrieving device token " + error.getMessage());
                    }
                });
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

                        OPUser user = getPeerUserForCall(call);
                        call.setPeerUser(user);
                        Log.d("test", "found user for incoming call " + user);

                        mCalls.put(caller.getPeerURI(), call);
                        CallActivity.launchForIncomingCall(OPApplication.getInstance(), caller.getPeerURI());
                        break;
                    case CallState_Closed:
                        onCallEnd(call);
                        break;

                }
            }
        };
        CallbackHandler.getInstance().registerConversationThreadDelegate(threadDelegate);
        CallbackHandler.getInstance().registerCallDelegate(null, callDelegate);
    }

    public OPCall getOngoingCallForPeer(String peerUri) {
        return mCalls.get(peerUri);
    }

    public void onCallEnd(OPCall mCall) {
        String peerUri = mCall.getPeer().getPeerURI();
        mCalls.remove(peerUri);
        mCallStates.remove(peerUri);
        OPNotificationBuilder.cancelNotificationForCall(mCall);
    }

    public void hangupCall(OPCall mCall, CallClosedReasons callclosedreasonUser) {
        mCall.hangup(CallClosedReasons.CallClosedReason_User);
        onCallEnd(mCall);
    }

    public OPUser getPeerUserForCall(OPCall call) {
        OPContact contact = call.getPeer();

        OPUser user = new OPUser(contact, call.getConversationThread().getIdentityContactList(contact));
        user = OPDataManager.getDatastoreDelegate().saveUser(user);
        return user;
    }

    Hashtable<String, CallStatus> mCallStates = new Hashtable<String, CallStatus>();

    public CallStatus getMediaStateForCall(String peerUri) {
        CallStatus state = null;
        if (mCallStates == null) {
            mCallStates = new Hashtable<String, CallStatus>();

        } else {
            state = mCallStates.get(peerUri);
        }
        if (state == null) {
            state = new CallStatus();
            mCallStates.put(peerUri, state);
        }
        return state;

    }
}