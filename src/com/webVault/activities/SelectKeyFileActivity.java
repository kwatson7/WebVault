package com.webVault.activities;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.webVault.Prefs;
import com.webVault.R;
import com.webVault.Utils;

public class SelectKeyFileActivity extends CustomActivity{

	// graphics pointers
	private com.tools.NoDefaultSpinner keyFileSpinner;
	private EditText keyPairName;
	private EditText password1;
	private EditText password2;
	private CheckBox showPassword;
	
	// private variables
	private String selectedFile;
	private String[] fileArray;
	
	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		// check if we have a key, if not, then load screen, else quit
		if (Prefs.getKeyFile(ctx) != null){
			Intent intent = new Intent(this, MainActivity.class);
        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		
		// load screen
		initializeLayout();		
	}

	@Override
	protected void initializeLayout() {
		setContentView(R.layout.select_key_file);
		
		// load pointers
		keyFileSpinner = (com.tools.NoDefaultSpinner) findViewById(R.id.keySelector);
		keyPairName = (EditText) findViewById(R.id.keyPairName);
		password1 = (EditText) findViewById(R.id.password1);
		password2 = (EditText) findViewById(R.id.password2);
		showPassword = (CheckBox) findViewById(R.id.showPassword);
		
		// make listener when spinner is clicked
		keyFileSpinner.setOnItemSelectedListener(spinnerListener);
		keyFileSpinner.setOnTouchListener(spinnerOnTouch);
		keyFileSpinner.setOnKeyListener(spinnerOnKey);
		
		loadKeyFiles();
	}

	@Override
	protected void additionalConfigurationStoring() {
	}

	@Override
	protected void onDestroyOverride() {
	}
	
	/**
	 * Either show or don't show password based on this checkbox
	 * @param view
	 */
	public void showPasswordClicked(View view){
		boolean isClicked = showPassword.isChecked();
		if (isClicked){
			password1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			password2.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			password1.invalidate();
			password2.invalidate();
		}else{
			password1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			password2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			password1.invalidate();
			password2.invalidate();
		}
	}
	
	/**
	 * Create new key pair button clicked. Create the pair / store to file / change selected file / change spinner
	 * @param view
	 */
	public void createNewKeyPair(View view){
		
		// grab name of file
		final String fileName = parseName();
		if (fileName == null)
			return;
		
		// grab the passwords
		final String password = parsePassword();
		if (password == null)
			return;
		
		// create a new key pair in background
		(new CustomAsyncTask<SelectKeyFileActivity, Void, com.tools.encryption.PublicPrivateEncryptor>(
				this,
				0,
				true, 
				false,
				null) {

			@Override
			protected void onPreExecute() {				
			}

			@Override
			protected PublicPrivateEncryptor doInBackground(Void... params) {
				// create the encryption
				com.tools.encryption.PublicPrivateEncryptor pair = null;
				try {
					pair = PublicPrivateEncryptor.createFromNewKeyPair();
				} catch (EncryptionException e) {
					if (applicationCtx != null)
						Utils.showCustomToast(applicationCtx, "encryption problem", true, 1);
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					return null;
				}
				
				// save to file
				try {
					pair.saveToFile(fileName, password);
				} catch (IOException e) {
					if (applicationCtx != null)
						Utils.showCustomToast(applicationCtx, e.getMessage(), true, 1);
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					return null;
				} catch (EncryptionException e) {
					Utils.showCustomToast(applicationCtx, "encryption problem", true, 1);
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
					return null;
				}
				
				// current file stored
				if (callingActivity != null)
					callingActivity.selectedFile = fileName;
				
				return pair;
			}

			@Override
			protected void onProgressUpdate(Void... progress) {				
			}

			@Override
			protected void onPostExectueOverride(PublicPrivateEncryptor result) {
				// if pair isn't null, then process file
				if (result != null){
					callingActivity.saveKeyFileToPrefsAndShowToast(fileName);
				}
			}

			@Override
			protected void setupDialog() {
				// show dialog for this long process
				if (callingActivity != null){
					dialog = new ProgressDialog(callingActivity);
					dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					dialog.setTitle("Creating Key Pair");
					dialog.setMessage("Please wait...");
					dialog.setIndeterminate(true);
					dialog.setCancelable(false);
				}	
			}
		}).execute();	
	}

	/**
	 * Save the full filename to prefs file, show toast, and go to next screen
	 * @param filename The full filename
	 */
	private void saveKeyFileToPrefsAndShowToast(String filename){
		Prefs.setKeyFile(ctx, filename);
		Utils.showCustomToast(ctx, "Key File Selected", true, 1);

		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}

	/**
	 * load the key files into the spinner
	 */
	private void loadKeyFiles(){

		// find files in directory
		File folder = new File(Utils.getExternalStorageTopPath());
		fileArray = folder.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".key") || filename.endsWith(".keys");
			}
		});
		if (fileArray == null)
			fileArray = new String[0];

		// make array adapter to hold group names
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
				this, R.layout.spinner_item, fileArray);		

		// set adapter and launch it
		keyFileSpinner.setAdapter(spinnerArrayAdapter);		
	}
	
	/**
	 * Listener for spinner touch, just load the key files
	 */
	private View.OnTouchListener spinnerOnTouch = new View.OnTouchListener() {
	    public boolean onTouch(View v, MotionEvent event) {
	        if (event.getAction() == MotionEvent.ACTION_UP) {
	        	loadKeyFiles();
	        }
	        return false;
	    }
	};
	
	/**
	 * Listener for spinner touch, just load the key files
	 */
	private View.OnKeyListener spinnerOnKey = new View.OnKeyListener() {
	    public boolean onKey(View v, int keyCode, KeyEvent event) {
	        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
	        	loadKeyFiles();
	            return false;
	        } else {
	            return false;
	        }
	    }
	};

	/**
	 * The spinner listener for selected a key file to use
	 */
	private Spinner.OnItemSelectedListener spinnerListener = new Spinner.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parentView, 
				View selectedItemView, int position, long id) {

			selectedFile = fileArray[position];
			saveKeyFileToPrefsAndShowToast((new File(Utils.getExternalStorageTopPath(), selectedFile)).getAbsolutePath());
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// do nothing, just leave
		}
	};
	
	/**
	 * Return the name of the file to be used for storeing keys with full path. If the file name is not unique, null is returned and toast is shown.
	 * @return
	 */
	private String parseName(){
		// grab name and remove non allowable characters
		String name = keyPairName.getText().toString();
		name = com.tools.Tools.getAllowableFileName(name);
		
		// check if blank
		if (name.length() == 0){
			Utils.showCustomToast(ctx, "Name can't be blank", false, 1);
			return null;
		}
		
		// check if already exists
		File file = new File(Utils.getExternalStorageTopPath(), name+".key");
		if (file.exists()){
			Utils.showCustomToast(ctx, "Name already taken", false, 1);
			return null;
		}
		
		return file.getAbsolutePath();
	}
	
	/**
	 * Parse password to make sure they are equal. Will return null and show toast if they don't match
	 * @return the password
	 */
	private String parsePassword(){
		// check not empty and equal
		String p1 = password1.getText().toString();
		String p2 = password2.getText().toString();
		
		if (!p1.equals(p2)){
			Utils.showCustomToast(ctx, "Passwords are not equal", false, 1);
			return null;
		}
		
		if (p1.length() == 0){
			Utils.showCustomToast(ctx, "Password can't be empty", false, 1);
			return null;
		}
			
		return p1;
	}
}
