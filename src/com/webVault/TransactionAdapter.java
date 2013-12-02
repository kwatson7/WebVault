package com.webVault;

import java.io.IOException;
import java.util.List;

import com.parse.ParseException;
import com.tools.TwoObjects;
import com.tools.ViewLoader;
import com.tools.ViewLoader.LoadData;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.KeyNickname.NoNicknameException;
import com.webVault.serverobjects.Transaction;
import com.webVault.serverobjects.Transaction.UserNotInTransactionException;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TransactionAdapter 
extends BaseAdapter{

	private List<Transaction> data;
	private LayoutInflater inflater = null;
	private AccountHolder accountData = null;
	private Activity act;
	private com.tools.ViewLoader<Integer, Integer, TwoObjects<Spanned, Boolean>, TextView> assetLoader;		
	private com.tools.ViewLoader<Integer, Integer, Boolean, View> transactionValidator;	

	public TransactionAdapter(Activity a, List<Transaction> transactionList, final AccountHolder accountData) {
		data = transactionList;
		inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.accountData = accountData;
		act = a;

		// the background thread asset loader
		assetLoader = new ViewLoader<Integer, Integer, TwoObjects<Spanned, Boolean>, TextView>(
				new TwoObjects<Spanned, Boolean>(new SpannedString("Loading..."), null),
				new LoadData<Integer, TwoObjects<Spanned, Boolean>, TextView>() {

					@Override
					public TwoObjects<Spanned, Boolean> onGetData(Integer key) {
						// get the item
						Transaction transaction = (Transaction) getItem(key);

						// initialize output
						TwoObjects<Spanned, Boolean> out = new TwoObjects<Spanned, Boolean>(null, null);
						
						// change color based on amount and show the amount
						try{
							if (transaction.isSender(accountData.getPublicKeyAsBase64())){
								out.mObject1 = transaction.getAsset().getFormattedAmount(transaction.getAmount(), false);
								out.mObject2 = false;
							}else if (transaction.isReceiver(accountData.getPublicKeyAsBase64())){
								out.mObject1 = transaction.getAsset().getFormattedAmount(transaction.getAmount(), true);
								out.mObject2 = true;
							}else{
								out.mObject1 = transaction.getAsset().getFormattedAmount(transaction.getAmount());
								out.mObject2 = null;
							}
						}catch (ParseException e){
							out.mObject1 = new SpannedString("Parse Error");
							out.mObject2 = null;
							Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
						}
						
						return out;
					}

					@Override
					public void onBindView(TwoObjects<Spanned, Boolean> data, TextView view) {
						if (view == null || data == null)
							return;

						// set the text
						view.setText(data.mObject1);

						// set the color
						if (data.mObject2 == null){
							view.setTextColor(act.getResources().getColor(android.R.color.black));
						}else if (data.mObject2){
							view.setTextColor(act.getResources().getColor(R.color.light_green));
						}else{
							view.setTextColor(act.getResources().getColor(R.color.light_red));
						}
					}
				});
		
		// background transaction validator
		transactionValidator = new ViewLoader<Integer, Integer, Boolean, View>(
				null,
				new LoadData<Integer, Boolean, View>() {

					@Override
					public Boolean onGetData(Integer key) {
						// get the item
						Transaction transaction = (Transaction) getItem(key);
						boolean value = false;
						try {
							value = transaction.verifyTransaction();
						} catch (EncryptionException e) {
							Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
							value = false;
						}
						return value;
					}

					@Override
					public void onBindView(Boolean data, View view) {
						if (view == null || act==null)
							return;
						if (data == null)
							view.setBackgroundColor(act.getResources().getColor(android.R.color.darker_gray));
						else if (data)
							view.setBackgroundColor(act.getResources().getColor(android.R.color.background_light));
						else
							view.setBackgroundColor(act.getResources().getColor(R.color.light_red));
						
					}
				});
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int arg0) {
		return data.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// attempt to use recycled view
		View vi=convertView;
		if(convertView==null)
			vi = inflater.inflate(R.layout.transaction_item, null);

		// grab pointers
		TextView date = (TextView)vi.findViewById(R.id.date);
		TextView otherPerson = (TextView)vi.findViewById(R.id.otherPerson);
		TextView message = (TextView)vi.findViewById(R.id.message);
		TextView amount = (TextView)vi.findViewById(R.id.amount);
		TextView total = (TextView)vi.findViewById(R.id.total);

		// get the item
		Transaction transaction = (Transaction) getItem(position);

		// date
		date.setText(Utils.getFormattedDate(transaction.getDateCreated()));

		// the message
		try {
			message.setText(transaction.getDecryptedMessage(accountData.getUserKey()));
		} catch (EncryptionException e) {
			message.setText(e.getMessage());
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IOException e) {
			message.setText(e.getMessage());
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IncorrectPasswordException e) {
			message.setText(e.getMessage());
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}

		// the other persons name
		try {
			otherPerson.setText(transaction.getOtherPersonsNickname(accountData));
		} catch (NoNicknameException e) {
			otherPerson.setText("???");
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (UserNotInTransactionException e) {
			otherPerson.setText("???");
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}

		// load the asset
		assetLoader.DisplayView(position, position, amount);
		
		// the total
		try {
			total.setText("(" + transaction.getNewTotal(accountData.getPublicKeyAsBase64()) + ")");
		} catch (UserNotInTransactionException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
			total.setText("Error");
		}
		
		// validate the transaction
		transactionValidator.DisplayView(position, position, vi);
		try {
			if(!transaction.verifyTransaction()){
				vi.setBackgroundColor(act.getResources().getColor(R.color.light_red));
			}
		} catch (NotFoundException e) {
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (EncryptionException e) {
			vi.setBackgroundColor(act.getResources().getColor(R.color.light_red));
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}
		
		return vi;
	}
}
