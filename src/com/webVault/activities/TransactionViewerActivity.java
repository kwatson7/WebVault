package com.webVault.activities;

import java.io.IOException;
import java.util.List;

import com.parse.ParseException;
import com.parse.ParseQuery;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.TwoObjects;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.webVault.AccountData;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.TimeoutException;
import com.webVault.R;
import com.webVault.TransactionAdapter;
import com.webVault.Utils;
import com.webVault.activities.LoginActivity.OnLogin;
import com.webVault.serverobjects.Transaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class TransactionViewerActivity extends CustomActivity{

	// graphics objects
	private ListView listView;

	// private variables
	AccountHolder accountData = null;
	TransactionViewerActivity act = this;

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		
		ParseQuery.clearAllCachedResults();
		
		initializeLayout();	
	}

	@Override
	protected void initializeLayout() {
		setContentView(R.layout.transaction_viewer);

		// grab graphics pointers
		listView = (ListView) findViewById(R.id.listView);
	}

	@Override
	protected void onResume(){
		super.onResume();

		if(loadAccountDataLogin())
			afterResumeToDo();
	}
	
	private void afterResumeToDo(){
		if (accountData == null)
			return;
		Transaction.queryTransactionsInBackground(
				act,
				null,
				true,
				0,
				100,
				accountData.getPublicKeyAsBase64(),
				queryTransactionCallback);
	}
	
	/**
	 * When querying the transaction, perform this when finished. Will load list adapter with new transaction
	 */
	private static FinishedCallback<TransactionViewerActivity, TwoObjects<ParseException, List<Transaction>>> queryTransactionCallback = 
		new FinishedCallback<TransactionViewerActivity, TwoObjects<ParseException,List<Transaction>>>() {

			@Override
			public void onFinish(TransactionViewerActivity activity,
					TwoObjects<ParseException, List<Transaction>> result) {
				// if we get an exception, just leave activity
				if (result.mObject1 != null){
					Log.e(Utils.LOG_TAG, result.mObject1.getMessage());
					Utils.showCustomToast(activity, result.mObject1.getMessage(), true, 1);
					activity.finish();
					return;
				}
				TransactionAdapter adapter = new TransactionAdapter(activity, result.mObject2, activity.accountData);
				activity.listView.setAdapter(adapter);
				
			}
		};

	/**
	 * Load the account data into this activity. If it fails, it will launch a login screen
	 * @return true if we already have the data, false otherwise
	 */
	private boolean loadAccountDataLogin(){
		accountData = null;
		try {
			accountData = AccountData.getAccountData();
			return true;
		} catch (TimeoutException e) {
			LoginActivity.launchActivity(this);
			return false;
		}
	}

	/**
	 * get account data or finish if we can't get it
	 * @param password, can be null if no password is available, will just show toast and quit if we can't access password
	 */
	private void getAccountData(String password){

		accountData = null;
		try {
			accountData = AccountData.getAccountData();
		} catch (TimeoutException e) {
			try {
				if (password == null){
					Utils.showCustomToast(ctx, "Cannot access account", true, 1);
					finish();
					return;
				}
				accountData = AccountData.loadData(this, password);
			} catch (IOException e1) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
				Utils.showCustomToast(ctx, e1.getMessage(), true, 1);
				finish();
			} catch (IncorrectPasswordException e1) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
				Utils.showCustomToast(ctx, e1.getMessage(), true, 1);
				finish();
			} catch (EncryptionException e1) {
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e1));
				Utils.showCustomToast(ctx, e1.getMessage(), true, 1);
				finish();
			}
		}
	}


	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		getAccountData(null);

		// we logged in
		LoginActivity.parseActivityResult(requestCode, resultCode, intent, new OnLogin() {

			@Override
			public void onLoginFailed() {
				Utils.showCustomToast(ctx, "Login failed... Quitting", true, 1);
				finish();
				return;
			}

			@Override
			public void onLoginCanceled() {
				Utils.showCustomToast(ctx, "Cancelled login... Quitting", true, 1);
				finish();
				return;
			}

			@Override
			public void onSuccessfulLoginKey(char[] key) {
				throw new UnsupportedOperationException("Not supported yet.");	
			}

			@Override
			public void onSuccessfulLoginPassword(String password) {
				getAccountData(password);
				afterResumeToDo();
			}

			@Override
			public void onLoginError() {
				Utils.showCustomToast(ctx, "Login error... Quitting", true, 1);
				finish();
				return;
			}
		});
	}


	@Override
	protected void additionalConfigurationStoring() {

	}

	@Override
	protected void onDestroyOverride() {

	}
}
