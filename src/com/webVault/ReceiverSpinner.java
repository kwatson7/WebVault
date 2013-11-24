package com.webVault;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.tools.NoDefaultSpinner;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.KeyNickname;
import com.webVault.AccountData.TimeoutException;

public class ReceiverSpinner extends NoDefaultSpinner{

	// private variables
	private KeyNickname selected;
	private SpinAdapter spinnerAdapter;

	public ReceiverSpinner(Context context) {
		super(context);
		initializeListeners();
	}

	public ReceiverSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeListeners();
	}

	public ReceiverSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeListeners();
	}

	/**
	 * Get the item at the selected position
	 * @param position
	 * @return the item selected
	 */
	public KeyNickname getItem(int position){
		return spinnerAdapter.getItem(position);
	}
	
	/**
	 * Set the spinner to be at the selected key. If it does not match, then return false
	 * @param accountHolder the accountHolder for the account. See AccountData.getAccountData();
	 * @param publicKey The key to set the spinner to
	 * @return True if it was set, false otherwise
	 * @throws TimeoutException if the account data has been locked
	 */
	public boolean setSelectedKey(AccountHolder accountHolder, String publicKey){
		KeyNickname[] keyList = accountHolder.getUsersKeys();
		
		// key doesn't exist
		if (keyList == null || keyList.length == 0)
			return false;
		
		for (int i = 0; i < keyList.length; i++){
			if (keyList[i].publicKey.equals(publicKey)){
				// make array adapter to hold group names
				spinnerAdapter = new SpinAdapter(
						getContext(), R.layout.spinner_item, keyList);	
				setAdapter(spinnerAdapter);	
				setSelection(i);
				return true;
			}
		}
		
		// if we made it here, then the selection was not found
		return false;
	}
	
	/**
	 * Return the selected item. Will be null if nothing selected yet and/or the setOnItemSelectedListener has been overriden
	 * @return
	 */
	public KeyNickname getSelected(){
		return selected;
	}

	private void initializeListeners(){
		// make listener when spinner is clicked
		setOnItemSelectedListener(spinnerListener);
		setOnTouchListener(spinnerOnTouch);
		setOnKeyListener(spinnerOnKey);
	}

	/**
	 * Listener for spinner touch, just load the key files
	 */
	private View.OnTouchListener spinnerOnTouch = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				try {
					loadContacts();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
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
				try {
					loadContacts();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
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
			selected = spinnerAdapter.getItem(position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// do nothing, just leave
		}
	};

	/**
	 * load the contacts into the spinner
	 * @throws TimeoutException 
	 */
	private void loadContacts() throws TimeoutException{

		AccountHolder accountHolder = AccountData.getAccountData();
		KeyNickname[] keyList = accountHolder.getUsersKeys();

		// make array adapter to hold group names
		spinnerAdapter = new SpinAdapter(
				getContext(), R.layout.spinner_item, keyList);	

		// set adapter and launch it
		setAdapter(spinnerAdapter);		
	}

	public static class SpinAdapter extends ArrayAdapter<KeyNickname>{

		// Your sent context
		private Context context;
		// Your custom values for the spinner (User)
		private KeyNickname[] values;
		private int textViewResourceId;

		public SpinAdapter(Context context, int textViewResourceId,
				KeyNickname[] values) {
			super(context, textViewResourceId, values);
			this.context = context;
			this.values = values;
			this.textViewResourceId = textViewResourceId;
		}

		public int getCount(){
			return values.length;
		}

		public KeyNickname getItem(int position){
			return values[position];
		}

		public long getItemId(int position){
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if(row == null)
			{
				LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				row = inflater.inflate(textViewResourceId, parent, false);
			}
			//put the data in it
			KeyNickname item = getItem(position);
			if(item != null)
			{   
				TextView text1 = (TextView) row.findViewById(android.R.id.text1);
				text1.setText(item.nickname);
			}

			return row;
		}

		// And here is when the "chooser" is popped up
		// Normally is the same view, but you can customize it if you want
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if(row == null)
			{
				LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				row = inflater.inflate(textViewResourceId, parent, false);
			}
			//put the data in it
			KeyNickname item = getItem(position);
			if(item != null)
			{   
				TextView text1 = (TextView) row.findViewById(android.R.id.text1);
				text1.setText(item.nickname);
			}

			return row;
		}
	}
}
