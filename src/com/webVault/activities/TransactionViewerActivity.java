package com.webVault.activities;

import java.io.IOException;
import java.util.List;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.tools.CustomActivity;
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
		Transaction.queryTransactionsInBackground(0, 10, accountData.getPublicKeyAsBase64(), queryTransactionCallback);
	}
	/**
	 * When querying the transaction, perform this when finished. Will load list adapter with new transaction
	 */
	private FindCallback queryTransactionCallback = new FindCallback(){

		@Override
		public void done(List<ParseObject> objects, ParseException e) {
			
			// if we get an exception, just leave activity
			if (e != null){
				Log.e(Utils.LOG_TAG, e.getMessage());
				Utils.showCustomToast(TransactionViewerActivity.this, e.getMessage(), true, 1);
				finish();
				return;
			}
			
			List<Transaction> list = Transaction.convertList(objects);
			TransactionAdapter adapter = new TransactionAdapter(act, list, accountData);
			listView.setAdapter(adapter);
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
