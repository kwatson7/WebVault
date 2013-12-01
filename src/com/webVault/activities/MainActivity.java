package com.webVault.activities;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.webVault.AccountData;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.KeyNickname;
import com.webVault.AccountData.TimeoutException;
import com.webVault.R;
import com.webVault.ReceiverSpinner;
import com.webVault.Utils;
import com.webVault.Tasks.AccountValueTask;
import com.webVault.Tasks.AccountValueTask.Output;
import com.webVault.activities.LoginActivity.OnLogin;
import com.webVault.serverobjects.Asset;
import com.webVault.serverobjects.Transaction;
import com.webVault.serverobjects.Transaction.InvalidTransactionException;

public class MainActivity extends CustomActivity{

	// graphics objects
	private TextView balanceField;
	private ProgressBar fetchingData;
	private ReceiverSpinner receiverSpinner;
	private EditText message;
	private EditText amountToSend;

	// private constants
	private static final String FETCH_DATA_TAG = "fetchingData";

	// private variables
	AccountHolder accountData = null;
	Asset asset = null;

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// parse analytics
		ParseAnalytics.trackAppOpened(getIntent());

		initializeLayout();	
	}

	@Override
	protected void initializeLayout() {
		setContentView(R.layout.transaction);

		// grab graphics pointers
		balanceField = (TextView) findViewById(R.id.balanceField);
		fetchingData = (ProgressBar) findViewById(R.id.fetchingData);
		receiverSpinner = (ReceiverSpinner) findViewById(R.id.receiverSpinner);
		message = (EditText) findViewById(R.id.message);
		amountToSend = (EditText) findViewById(R.id.amountToSend);
	}

	@Override
	protected void onResume(){
		super.onResume();

		loadAccountDataLogin();

		// fetch the balance
		fetchBalance();
	}

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

	/**
	 * Show the dialog for adding a nickname with the corresponding key
	 * @param key The key the nickname will be attached to.
	 */
	private void showCreateNickNameDialog(final String key){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);  
		final EditText input = new EditText(this); 
		alert.setMessage("Name").
		setView(input).
		setPositiveButton("OK", new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int whichButton) {  
				String value = input.getText().toString();
				if (accountData == null)
					Utils.showCustomToast(ctx, "Account data timed out", true, 1);
				else{
					try {
						if (value == null || value.length() == 0)
							accountData.addKey(key, ctx);
						else
							accountData.addKey(key, value, ctx);

						// make default selection this person
						if(!receiverSpinner.setSelectedKey(accountData, key))
							Utils.showCustomToast(ctx, "User not set... unknown error", true, 1);
					} catch (IOException e) {
						Utils.showCustomToast(ctx, e.getMessage(), true, 1);
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					} catch (EncryptionException e) {
						Utils.showCustomToast(ctx, e.getMessage(), true, 1);
						Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					}
				}                 
			}  
		}).  
		setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				return;   
			}
		});
		alert.show();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		getAccountData(null);

		// we want to our QR code
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			switch (resultCode){
			case RESULT_OK:
				// the public key
				String scannedKey = scanResult.getContents();

				// make sure we have account data
				if (accountData == null){
					Utils.showCustomToast(ctx, "No Account Data available", true, 1);
					return;
				}

				// null key
				if (scannedKey== null){
					Utils.showCustomToast(ctx, "Not a valid key", true, 1);
					return;
				}

				// check if a valid key
				try {
					PublicPrivateEncryptor k = new PublicPrivateEncryptor(null, scannedKey);
				} catch (EncryptionException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					Utils.showCustomToast(ctx, "Not a valid key. Try again", true, 1);
					return;
				}

				// add the key to the list of keys and store to file
				try {
					accountData.addKey(scannedKey, ctx);
				} catch (IOException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					Utils.showCustomToast(ctx, e.getMessage(), true, 1);
				} catch (EncryptionException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					Utils.showCustomToast(ctx, e.getMessage(), true, 1);
				}
				showCreateNickNameDialog(scannedKey);

				break;
			case RESULT_CANCELED:
				break;
			}
			return;
		}

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
			}

			@Override
			public void onLoginError() {
				Utils.showCustomToast(ctx, "Login error... Quitting", true, 1);
				finish();
				return;

			}
		});
	}

	/**
	 * Send was clicked, so send the transaction
	 * @param view
	 */
	public void sendClicked(View view){
		sendAmount();
	}

	private void sendAmount(){
		if (asset == null){
			Utils.showCustomToast(ctx, "No asset selected to send", true, 1);
			return;
		}

		// parse recipient
		KeyNickname selection = receiverSpinner.getSelected();
		if (selection == null){
			Utils.showCustomToast(ctx, "Select a recipient first", true, 1);
			return;
		}
		String receiverKey = selection.publicKey;

		// parse amount to send
		double amount = 0;
		try{
			// remove asset type string first
			String str = amountToSend.getText().toString();
			str = str.replace(Html.fromHtml(asset.getSymbol()), "");
			amount = Double.parseDouble(str);
		}catch(NumberFormatException  exception){  
			Utils.showCustomToast(ctx, "Amount to send is not a number", true, 1);
			return;
		}
		final double amount2 = amount;

		// parse message
		String messageToSend = message.getText().toString();

		// test sending a transaction
		PublicPrivateEncryptor receiver;
		try {
			receiver = new PublicPrivateEncryptor(null, receiverKey);
			Transaction transaction = new Transaction(receiver, accountData.getUserKey(), asset.getId(), amount, messageToSend);
			transaction.saveInBackground(this, null, new FinishedCallback<MainActivity, ParseException>() {

				@Override
				public void onFinish(MainActivity activity,
						ParseException result) {
					Utils.showCustomToast(ctx, com.tools.Tools.pluralString(amount2, activity.asset.getName()) + " sent", true, 1);
					activity.clearFields();
					activity.fetchBalance();
				}
			});
		} catch (EncryptionException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			Utils.showCustomToast(ctx, e.getMessage(), true, 1);
		} catch (InvalidTransactionException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			Utils.showCustomToast(ctx, e.getMessage(), true, 1);
		} catch (IOException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			Utils.showCustomToast(ctx, e.getMessage(), true, 1);
		}
	}

	/**
	 * Clear input fields
	 */
	private void clearFields(){
		message.setText("");
		amountToSend.setText("");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * When we click to share our QR code for our public key
	 * @param view
	 */
	public void onSharePublicKey(View view){
		// make sure we have account data
		if (accountData == null){
			getAccountData(null);
			return;
		}

		// launch a QR code for our public key
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.shareText(accountData.getPublicKeyAsBase64());
	}
	
	public void onViewHistory(View view){
		Intent intent = new Intent(this, TransactionViewerActivity.class);
		this.startActivity(intent);
	}

	/**
	 * Scan someone's public key
	 */
	public void onScanKey(View view){
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.addExtra("PROMPT_MESSAGE" , "Place Web Vault QR code in square");
		integrator.addExtra("SCAN_WIDTH" , (int)com.tools.Tools.convertDpToPix(ctx, 200));
		integrator.addExtra("SCAN_HEIGHT" , (int)com.tools.Tools.convertDpToPix(ctx, 200));

		integrator.initiateScan();
	}

	/**
	 * Update the string to include the symbol for the currency
	 */
	private void updateAmountUnitType(){
		if (asset == null)
			return;
		String amount = amountToSend.getText().toString();
		amount = amount.replaceAll("[^\\d.]", "");
		if (amount == null || amount.length() == 0)
			amountToSend.setText(Html.fromHtml(asset.getSymbol()));
		else
			amountToSend.setText(asset.getFormattedAmount(Double.parseDouble(amount)));
	}

	/**
	 * Fetch the balance and show to correct location. If no account data yet available, then just return.
	 */
	private void fetchBalance(){
		if (accountData == null)
			return;

		// get the balance
		ArrayList<String> bars = new ArrayList<String>(1);
		bars.add(FETCH_DATA_TAG);
		AccountValueTask<MainActivity> task = new AccountValueTask<MainActivity>(
				this,
				Utils.SCARON_TYPE,
				accountData.getPublicKeyAsBase64(),
				bars);
		task.setFinishedCallback(new FinishedCallback<MainActivity, AccountValueTask.Output>() {

			@Override
			public void onFinish(MainActivity activity, Output result) {
				if (activity == null)
					return;
				if (result.exception == null){
					activity.asset = result.asset;
					activity.updateAmountUnitType();
					activity.balanceField.setText(result.asset.getFormattedAmount(result.value));
				}else
					Utils.showCustomToast(activity, result.exception.getMessage(), true, 0.5f);
			}
		});
		task.execute();
	}

	@Override
	protected void additionalConfigurationStoring() {

	}

	@Override
	protected void onDestroyOverride() {

	}
}
