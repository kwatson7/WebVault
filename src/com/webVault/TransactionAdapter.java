package com.webVault;

import java.io.IOException;
import java.util.List;

import com.parse.ParseException;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.KeyNickname.NoNicknameException;
import com.webVault.serverobjects.Transaction;
import com.webVault.serverobjects.Transaction.UserNotInTransactionException;

import android.app.Activity;
import android.content.Context;
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

	public TransactionAdapter(Activity a, List<Transaction> transactionList, AccountHolder accountData) {
		data = transactionList;
		inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.accountData = accountData;
		act = a;
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
		
		// get the item
		Transaction transaction = (Transaction) getItem(position);

		// set fields
		// date
		date.setText(Utils.getFormattedDate(transaction.getDateCreated()));
		
		// the message
		try {
			message.setText(transaction.getDecryptedMessage(accountData.getUserKey()));
		} catch (EncryptionException e) {
			message.setText("Decryption Error");
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IOException e) {
			message.setText("Decryption Error");
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		} catch (IncorrectPasswordException e) {
			message.setText("Decryption Error");
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
		
		// change color based on amount and show the amount
		try{
			if (transaction.isSender(accountData.getPublicKeyAsBase64())){
				amount.setTextColor(act.getResources().getColor(R.color.light_red));
				amount.setText(transaction.getAsset().getFormattedAmount(transaction.getAmount(), false));
			}else if (transaction.isReceiver(accountData.getPublicKeyAsBase64())){
				amount.setTextColor(act.getResources().getColor(R.color.light_green));
				amount.setText(transaction.getAsset().getFormattedAmount(transaction.getAmount(), true));
			}else{
				amount.setTextColor(act.getResources().getColor(android.R.color.black));
				amount.setText(transaction.getAsset().getFormattedAmount(transaction.getAmount()));
			}
		}catch (ParseException e){
			amount.setText("Parse Error");
			amount.setTextColor(act.getResources().getColor(android.R.color.black));
			Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
		}

		return vi;
	}
}
