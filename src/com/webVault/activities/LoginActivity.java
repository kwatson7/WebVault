package com.webVault.activities;

import java.io.IOException;

import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tools.CustomActivity;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.SymmetricEncryptor;
import com.webVault.AccountData;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.TimeoutException;
import com.webVault.Prefs;
import com.webVault.Utils;

public class LoginActivity extends CustomActivity {


	private static final String PATTERN_KEY = "PATTERN_KEY";
	private static final String PATTERN_PASSWORD = "PATTERN_PASSWORD";

	// private constants
	private static final int LOGIN_REQUEST_CODE = 71716;

	//enums for activity calls
	public enum LoginCalls {
		REQ_CREATE_PATTERN, REQ_ENTER_PATTERN, REQ_ENTER_PASSWORD;
		private static LoginCalls convert(int value)
		{
			return LoginCalls.class.getEnumConstants()[value];
		}
	}

	public enum LoginResults{
		SUCCESSFUL_KEY, CANCEL, FAILED, ERROR, SUCCESSFUL_PASSWORD;
		private static LoginResults convert(int value)
		{
			return LoginResults.class.getEnumConstants()[value];
		}
	}
	
	/**
	 * Parse activity result in onActivityResult when called by launchActivity
	 * @param requestCode
	 * @param resultCode
	 * @param intent
	 * @param onLogin
	 * @return true if the result is from this call, false otherwise
	 */
	public static boolean parseActivityResult(int requestCode, int resultCode, Intent intent, OnLogin onLogin){
		if (requestCode != LOGIN_REQUEST_CODE){
			return false;
		}else{
			// parse possible values
			switch (LoginResults.convert(resultCode)){
			case CANCEL:
				onLogin.onLoginCanceled();
				break;
			case FAILED:
				onLogin.onLoginFailed();
				break;
			case SUCCESSFUL_KEY:
				onLogin.onSuccessfulLoginKey(intent.getCharArrayExtra(PATTERN_KEY));
				break;
			case ERROR:
				onLogin.onLoginError();
				break;
			case SUCCESSFUL_PASSWORD:
				onLogin.onSuccessfulLoginPassword(intent.getStringExtra(PATTERN_PASSWORD));
				break;
			}

			return true;
		}
	}
	
	/**
	 * Launch the login activity. Then in onActivityResult, use parseActivityResult
	 * @param act the calling activity
	 */
	public static void launchActivity(Activity act){
		Intent intent = new Intent(act, LoginActivity.class);
		act.startActivityForResult(intent, LOGIN_REQUEST_CODE);
	}


	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		// just launch pattern activity for now
		//Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
		//		ctx, LockPatternActivity.class);
		//intent.putExtra(LockPatternActivity.EXTRA_PATTERN, Prefs.getPatternArray(ctx));
		//	startActivityForResult(intent, LoginCalls.REQ_ENTER_PATTERN.ordinal());

		launchPasswordActivity();
	}

	/**
	 * Launch the password acitivty
	 */
	private void launchPasswordActivity(){
		// ask user for password
		Intent intent = new Intent(this, com.tools.DialogWithInputBox.class);
		intent.putExtra(com.tools.DialogWithInputBox.HINT_BUNDLE, "your password");
		intent.putExtra(com.tools.DialogWithInputBox.TITLE_BUNDLE, "Please enter your password to access your account");
		intent.putExtra(com.tools.DialogWithInputBox.INPUT_TYPE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		startActivityForResult(intent, LoginCalls.REQ_ENTER_PASSWORD.ordinal());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		switch (LoginCalls.convert(requestCode)) {
		// the user created a pattern
		case REQ_CREATE_PATTERN: {
			if (resultCode == RESULT_OK) {
				//savedPattern = data.getCharArrayExtra(
				//LockPatternActivity.EXTRA_PATTERN);
				//test2();
			}
			break;
		}

		// the user is entering the pattern
		case REQ_ENTER_PATTERN: {
			switch (resultCode) {
			// login was good
			case RESULT_OK:
				// grab the pattern that can be used to decode data
				char[] key = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN_WITH_SALT);
				Intent intent = new Intent();
				intent.putExtra(PATTERN_KEY, key);
				setResult(LoginResults.SUCCESSFUL_KEY.ordinal(), intent);
				finish();

				break;
			case RESULT_CANCELED:
				setResult(LoginResults.CANCEL.ordinal());
				finish();
				break;
			case LockPatternActivity.RESULT_FAILED:
				setResult(LoginResults.FAILED.ordinal());
				finish();
				break;
			}

			break;
		}// REQ_ENTER_PATTERN
		case REQ_ENTER_PASSWORD:
			
			switch (resultCode) {
			
			// login was good
			case RESULT_OK:
				// we entered the password, so grab it
				String password = data.getStringExtra(com.tools.DialogWithInputBox.RESULT);

				// try to extract data with password. try again if wrong password
				try {
					AccountData.loadData(this, password);
					
					// finish this activity
					Intent intent = new Intent();
					intent.putExtra(PATTERN_PASSWORD, password);
					setResult(LoginResults.SUCCESSFUL_PASSWORD.ordinal(), intent);
					finish();
					
				} catch (IOException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					Utils.showCustomToast(this, "Could not read file", false, 1);
					
					// finish this activity
					Intent intent = new Intent();
					setResult(LoginResults.ERROR.ordinal(), intent);
					finish();
					
				} catch (IncorrectPasswordException e) {
					Utils.showCustomToast(this, "Incorrect Password... try again", false, 1);
					launchPasswordActivity();
					return;
				} catch (EncryptionException e) {
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					Utils.showCustomToast(this, e.getMessage(), false, 1);
					
					// finish this activity
					Intent intent = new Intent();
					setResult(LoginResults.ERROR.ordinal(), intent);
					finish();
				}

				break;
			case RESULT_CANCELED:
				setResult(LoginResults.CANCEL.ordinal());
				finish();
				break;
			}
			
			
		default:
			break;
		}
	}

	private void decodeDataHARDCODEPASSWORD(char[] key){

		try {
			SymmetricEncryptor encryptor = new SymmetricEncryptor("password");
			byte[] data2 = encryptor.decryptFileToByteArray(Utils.getExternalStorageTopPath()+"kylesDefaultWebVaultKeys.key", true);
			encryptor = new SymmetricEncryptor(key);
			encryptor.encryptByteArrayToFile(data2, Utils.getExternalStorageTopPath()+"kylesDefaultWebVaultKeys.key");
		} catch (EncryptionException e) {
			// TODO Auto-generated catch block
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IncorrectPasswordException e) {
			// TODO Auto-generated catch block
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
	}

	@Override
	protected void initializeLayout() {

	}

	@Override
	protected void additionalConfigurationStoring() {		
	}

	@Override
	protected void onDestroyOverride() {

	}
	
	/**
	 * Interface for handling the different results after a login
	 * @author Kyle
	 *
	 */
	public interface OnLogin{
		/**
		 * Successful login
		 * @param key the key we were able to extract from login
		 */
		public void onSuccessfulLoginKey(char[] key);
		/**
		 * The user cancelled the login
		 */
		public void onLoginCanceled();
		/**
		 * The user failed the login
		 */
		public void onLoginFailed();
		/**
		 * The user successfully logged in with a password
		 * @param password
		 */
		public void onSuccessfulLoginPassword(String password);
		
		/**
		 * The login failed with an error
		 * @param error The string for the error
		 */
		public void onLoginError();
	}
}
