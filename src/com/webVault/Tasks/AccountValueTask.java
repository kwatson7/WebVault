package com.webVault.Tasks;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.parse.ParseException;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.webVault.Utils;
import com.webVault.serverobjects.Asset;
import com.webVault.serverobjects.CloudFunctions;

public class AccountValueTask <ACTIVITY_TYPE extends CustomActivity>  
extends CustomAsyncTask<ACTIVITY_TYPE, Void, AccountValueTask.Output>{
	
	// private variables
	String publicKeyBase64;
	String itemType;
	
	/**
	 * 
	 * @param act
	 * @param itemType
	 * @param publicKeyBase64
	 * @param progressBars
	 */
	public AccountValueTask(
			ACTIVITY_TYPE act,
			String itemType,
			String publicKeyBase64,
			ArrayList<String> progressBars) {
		super(act, 0, false, true, progressBars);
		
		// store values
		this.itemType = itemType;
		this.publicKeyBase64 = publicKeyBase64;
	}

	public static class Output {

		/**
		 * The value of the user for the given asset
		 */
		public double value;
		/**
		 * Exception if any. Null if none
		 */
		public ParseException exception;
		/**
		 * The public key of the user in question
		 */
		public String userPublicKeyBase64;
		/**
		 * The asset in question	
		 */
		public Asset asset;
		
		private Output(double value, ParseException exception, Asset asset, String userPublicKey){
			this.value = value;
			this.exception = exception;
			this.userPublicKeyBase64 = userPublicKey;
			this.asset = asset;
		}
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected Output doInBackground(Void... params) {
		
		// call the function
		double value = 0;
		ParseException e1 = null;
		try {
			value = CloudFunctions.getAccountValue(publicKeyBase64, itemType);
		} catch (ParseException e) {
			e1 = e;
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
		
		// now grab the asset type
		Asset asset = null;
		if (e1 == null){
			try {
				asset = Asset.fetchAsset(itemType);
			} catch (ParseException e) {
				e1 = e;
				Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			}
		}
		return new Output(value, e1, asset, publicKeyBase64);
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
		
	}

	@Override
	protected void onPostExectueOverride(Output result) {
		
	}

	@Override
	protected void setupDialog() {

	}
}
