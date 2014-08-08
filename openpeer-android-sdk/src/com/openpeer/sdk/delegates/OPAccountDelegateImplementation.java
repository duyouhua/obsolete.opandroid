/**
 * Copyright (c) 2014, SMB Phone Inc. / Hookflash Inc.
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p/>
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package com.openpeer.sdk.delegates;

import android.util.Log;

import com.openpeer.javaapi.AccountStates;
import com.openpeer.javaapi.OPAccount;
import com.openpeer.javaapi.OPAccountDelegate;
import com.openpeer.sdk.app.LoginManager;
import com.openpeer.sdk.app.LoginUIListener;
import com.openpeer.sdk.app.OPDataManager;
import com.openpeer.sdk.app.OPSdkConfig;

public  class OPAccountDelegateImplementation extends OPAccountDelegate {
	LoginUIListener mListener;
	private static OPAccountDelegateImplementation instance;
	public static OPAccountDelegateImplementation getInstance(){
		if(instance==null){
			instance = new OPAccountDelegateImplementation();			
		}
		return instance;
	}

	private OPAccountDelegateImplementation(){}
	public void bind(LoginUIListener listener) {
		this.mListener = listener;
	}
	public void unbind() {
		this.mListener = null;
	}

	@Override
	public void onAccountStateChanged(OPAccount account, AccountStates state) {
		Log.d("state", "Account state " + state);
		switch (state) {
		case AccountState_WaitingForAssociationToIdentity:
			break;
		case AccountState_WaitingForBrowserWindowToBeLoaded:
			mListener.getAccountWebview().post(new Runnable() {
				public void run() {
					mListener.getAccountWebview().loadUrl(OPSdkConfig.getInstance().getNamespaceGrantServiceUrl());
				}
			});
			break;
		case AccountState_WaitingForBrowserWindowToBeMadeVisible:
			mListener.getAccountWebview().post(new Runnable() {
				public void run() {
					mListener.onAccountLoginWebViewMadeVisible();
				}
			});
			account.notifyBrowserWindowVisible();
			break;
		case AccountState_WaitingForBrowserWindowToClose:
			mListener.getAccountWebview().post(new Runnable() {
				public void run() {
					mListener.onAccountLoginWebViewMadeClose();
				}
			});
			account.notifyBrowserWindowClosed();
			break;
		case AccountState_Ready:
			Log.w("login", "Account READY !!!!!!!!!!!!");
			OPDataManager.getInstance().setAccountReady(true);

			LoginManager.getInstance().onAccountStateReady(account);
			break;
		case AccountState_Shutdown:
			OPDataManager.getInstance().setAccountReady(false);
			if (mListener != null) {
				mListener.getAccountWebview().post(new Runnable() {
					public void run() {
						mListener.onLoginError();
					}
				});
			}
			break;
			default:
				break;
		}
	
	}

	@Override
	public void onAccountAssociatedIdentitiesChanged(OPAccount account) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAccountPendingMessageForInnerBrowserWindowFrame(OPAccount account) {
		// TODO Auto-generated method stub
		// LoginManager.pendingMessageForNamespaceGrantInnerFrame();
		String msg = account.getNextMessageForInnerBrowerWindowFrame();
		Log.d("output", "pendingMessageForNamespaceGrantInnerFrame " + msg);
		passMessageToJS(msg);
	}

	void passMessageToJS(final String msg) {
		mListener.getAccountWebview().post(new Runnable() {
			public void run() {
				String cmd = String.format("javascript:sendBundleToJS(\'%s\')", msg);
				Log.w("JNI", "Pass to JS: " + cmd);
				mListener.getAccountWebview().loadUrl(cmd);
			}
		});
	}
}