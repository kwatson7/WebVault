package com.webVault.serverobjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.TwoObjects;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.webVault.AccountData.AccountHolder;
import com.webVault.AccountData.KeyNickname.NoNicknameException;
import com.webVault.Utils;

public class Transaction{

	// the object name
	private static final String OBJECT_NAME = "Transaction"; 			// the object name for the ParseObject
	private static final int N_BYTES_N_ONCE = 4; 						// number of bytes for nonce

	// the fields
	private static final String SenderPublic = "SenderPublic";  		// sender's public key
	private static final String ReceiverPublic = "ReceiverPublic"; 		// receiver's public key
	private static final String ItemType = "ItemType";					// type of item being transferred
	private static final String Amount = "Amount"; 						// number of items transferred
	private static final String EncryptedMessage = "EncryptedMessage"; 	// encrypted message
	private static final String SignedMessage = "SignedMessage"; 		// signature of message
	private static final String HashArray = "HashArray"; 				// the important aspects of message that will be hashed
	private static final String Signature = "Signature"; 				// hash of hash_string that verifies sender actually sent with private key
	private static final String EncryptedSecretKeyMessage = "EncryptedSecretKeyMessage"; 	// the encrypted secret key for the message
	private static final String EncryptedSecretKeyMessageForSender = "EncryptedSecretKeyMessageForSender"; // the encrypted secret key that the receiver can decrypt
	private static final String SendersNewTotal = "SendersNewTotal"; 	// the new total of the sender
	private static final String ReceiversNewTotal = "ReceiversNewTotal";// the new total of the receiver
	
	// misc private variables
	private ParseObject parse; 											// The parse object this wraps around

	/**
	 * Create a Comment on a post
	 * @param post the CatPicture post this comment is for
	 * @param user the user who is commenting
	 * @param data the string data of the actual comment
	 * @throws IOException 
	 * @throws EncryptionException 
	 * @throws InvalidTransactionException 
	 */
	public Transaction(
			com.tools.encryption.PublicPrivateEncryptor receiver,
			com.tools.encryption.PublicPrivateEncryptor sender,
			String itemType,
			double amount,
			String message
	) throws EncryptionException, InvalidTransactionException, IOException{

		// check acceptable transaction
		checkTransaction(amount, itemType);

		parse = new ParseObject(OBJECT_NAME);

		// put the data into the parse object
		parse.put(ReceiverPublic, receiver.getPublicAsBase64());
		parse.put(ItemType, itemType);
		parse.put(Amount, amount);
		parse.put(SenderPublic, sender.getPublicAsBase64());

		// encrypt the message
		com.tools.encryption.EncryptedMessage encrypted = new com.tools.encryption.EncryptedMessage(
				sender.getPublic(), receiver.getPublic(), message, sender.getPrivate());
		parse.put(EncryptedMessage, encrypted.encryptedMessage);
		parse.put(SignedMessage, encrypted.signedMessage);
		parse.put(EncryptedSecretKeyMessage, encrypted.encryptedSecretKey);
		parse.put(EncryptedSecretKeyMessageForSender, encrypted.encryptedSecretKeyForSender);

		// generate the string we will hash and add nonce
		String hashString = createHashString(receiver, sender);
		byte[] hashBytes = com.tools.encryption.PublicPrivateEncryptor.appendNOnceToString(hashString, N_BYTES_N_ONCE);

		// now sign it
		parse.put(HashArray, hashBytes);
		parse.put(Signature, sender.signData(hashBytes));
	}

	/**
	 * Query the transaction on calling thread
	 * @param skip The number of queries to skip
	 * @param nNewQueries the number of new queries to get
	 * @param publicKey the public key we are going to search for
	 * @return the list of transactions
	 * @throws ParseException
	 */
	public static List<Transaction> queryTransactions(int skip, int nNewQueries, String publicKey)
	throws ParseException{

		// find only transaction matching the public key for sender
		ParseQuery sender = new ParseQuery(Transaction.OBJECT_NAME);
		sender.whereEqualTo(SenderPublic, publicKey);

		// repeat for receiver
		ParseQuery receiver = new ParseQuery(Transaction.OBJECT_NAME);
		receiver.whereEqualTo(ReceiverPublic, publicKey);

		// put two queries together
		List<ParseQuery> queries = new ArrayList<ParseQuery>();
		queries.add(sender);
		queries.add(receiver);
		ParseQuery mainQuery = ParseQuery.or(queries);

		// set find most recent but skip some if input
		mainQuery.orderByDescending("createdAt");
		mainQuery.setSkip(skip);
		mainQuery.setLimit(nNewQueries);

		// query and convert to Transaction
		return Transaction.convertList(mainQuery.find());		
	}

	/**
	 * Query the transactions in the background. If the dialog is closed, the activity will be ended
	 * @param <ACTIVITY_TYPE> the type of activity this was called from
	 * @param act The activity
	 * @param progressBars any progress to show, can be null
	 * @param isShowDialog boolean to show a progress dialog
	 * @param skip the number of  transacations to skip
	 * @param nNewQueries the total number to get
	 * @param publicKey the public key we are going to search for
	 * @param callback this will be called when complete
	 */
	public static <ACTIVITY_TYPE extends CustomActivity>void queryTransactionsInBackground(
			ACTIVITY_TYPE act,
			ArrayList<String> progressBars,
			final boolean isShowDialog,
			final int skip,
			final int nNewQueries,
			final String publicKey,
			FinishedCallback<ACTIVITY_TYPE, TwoObjects<ParseException, List<Transaction>>> callback){

		// create the task
		CustomAsyncTask<ACTIVITY_TYPE, Void, TwoObjects<ParseException, List<Transaction>>> task =
			new CustomAsyncTask<ACTIVITY_TYPE, Void, TwoObjects<ParseException, List<Transaction>>>(act, 0, false, true, progressBars){

			@Override
			protected void onPreExecute() {

			}

			@Override
			protected TwoObjects<ParseException, List<Transaction>> doInBackground(Void... params) {
				TwoObjects<ParseException, List<Transaction>> out =
					new TwoObjects<ParseException, List<Transaction>>(null, null);
				try {
					out.mObject2 = queryTransactions(skip, nNewQueries, publicKey);
				} catch (ParseException e) {
					out.mObject1 = e;
					Log.e(Utils.LOG_TAG, Log.getStackTraceString(e));
				}
				return out;
			}

			@Override
			protected void onProgressUpdate(Void... progress) {

			}

			@Override
			protected void onPostExectueOverride(TwoObjects<ParseException, List<Transaction>> result) {

			}

			@Override
			protected void setupDialog() {
				// show dialog for this long process
				if (callingActivity != null && isShowDialog){
					dialog = new ProgressDialog(callingActivity);
					dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					dialog.setTitle("Fetching Transactions");
					dialog.setMessage("Please wait...");
					dialog.setIndeterminate(true);
					dialog.setCancelable(true);
					dialog.setOnCancelListener(new OnCancelListener() {
						
						@Override
						public void onCancel(DialogInterface dialog) {
							if (callingActivity != null && !callingActivity.isFinishing())
							callingActivity.finish();
						}
					});
				}
			}
		};
		task.setFinishedCallback(callback);
		task.execute();
	}

	/**
	 * Save transaction in the background with a progress dialog showing
	 * @param act The activity that called it
	 * @param progressBars Any additional progress bars that are desired
	 * @param callback a callback when finished... can be null
	 */
	public <ACTIVITY_TYPE extends CustomActivity>void saveInBackground(
			ACTIVITY_TYPE act,
			ArrayList<String> progressBars,
			FinishedCallback<ACTIVITY_TYPE, ParseException> callback){

		// create the task
		CustomAsyncTask<ACTIVITY_TYPE, Void, ParseException> task = new CustomAsyncTask<ACTIVITY_TYPE, Void, ParseException>(act, 0, true, false, progressBars){

			@Override
			protected void onPreExecute() {

			}

			@Override
			protected ParseException doInBackground(Void... params) {
				ParseException e1 = null;
				try {
					parse.save();
				} catch (ParseException e) {
					e1 = e;
				}
				return e1;
			}

			@Override
			protected void onProgressUpdate(Void... progress) {

			}

			@Override
			protected void onPostExectueOverride(ParseException result) {

			}

			@Override
			protected void setupDialog() {
				// show dialog for this long process
				if (callingActivity != null){
					dialog = new ProgressDialog(callingActivity);
					dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					dialog.setTitle("Sending Transaction");
					dialog.setMessage("Please wait...");
					dialog.setIndeterminate(true);
					dialog.setCancelable(false);
				}
			}
		};
		task.setFinishedCallback(callback);
		task.execute();
	}

	/**
	 * Create a Transaction from a representative ParseObject
	 * @param parse
	 */
	public Transaction(ParseObject parse){
		this.parse = parse;
	}

	public ParseObject getParse(){
		return parse;
	}

	/**
	 * Return the date this transaction was created
	 * @return
	 */
	public Date getDateCreated(){
		return parse.getCreatedAt();
	}

	/**
	 * Get the asset for the given transaction. Queries server, so can be slow
	 * @return
	 * @throws ParseException
	 */
	public Asset getAsset() throws ParseException{
		Asset asset = Asset.fetchAsset(getItemId());
		return asset;
	}
	
	/**
	 * Get the new total after this transaction for the input public key
	 * @param publicKey The public key to look at
	 * @return the new total
	 * @throws UserNotInTransactionException if the user is not in this transaction
	 */
	public double getNewTotal(String publicKey) throws UserNotInTransactionException{
		if (isSender(publicKey))
			return parse.getDouble(SendersNewTotal);
		else if (isReceiver(publicKey))
			return parse.getDouble(ReceiversNewTotal);
		else
			throw new UserNotInTransactionException();
	}

	/**
	 * Determine the other person in transaction (other than the one passed in).
	 * @param accountData the accountData on this device
	 * @return the nickname of the other person in transaction
	 * @throws NoNicknameException 
	 * @throws UserNotInTransactionException 
	 */
	public String getOtherPersonsNickname(AccountHolder accountData) throws NoNicknameException, UserNotInTransactionException{
		// the sender, receiver
		String receiverPublicKey = getReceiverPublicKey();
		String senderPublicKey = getSenderPublicKey();
		String thisUser = accountData.getPublicKeyAsBase64();

		// check if user is sender, if so grab the other's nieckname
		if (senderPublicKey.equals(thisUser)){
			return accountData.getNickname(receiverPublicKey);
		}else if (receiverPublicKey.equals(thisUser)){
			return accountData.getNickname(senderPublicKey);
		}else{
			throw new UserNotInTransactionException();
		}
	}

	/**
	 * determine if the input user is the sender
	 * @param userPublicKey
	 * @return true if sender
	 */
	public boolean isSender(String userPublicKey){
		String senderPublicKey = getSenderPublicKey();

		// check if user is sender, if so grab the other's nieckname
		return senderPublicKey.equals(userPublicKey);
	}

	/**
	 * determine if the input user is the receiver
	 * @param userPublicKey
	 * @return true if sender
	 */
	public boolean isReceiver(String userPublicKey){
		String receiverPublicKey = getReceiverPublicKey();

		// check if user is sender, if so grab the other's nieckname
		return receiverPublicKey.equals(userPublicKey);
	}

	/**
	 * Create a string that says creates a formatted string indicating who sent what to whome. 
	 * Example: "Bob sent $6 to Alice."
	 * @param accountData Required to lookup nicknames
	 * @return The formatted string
	 * @throws ParseException
	 */
	public SpannableStringBuilder createFormatedTransactionString(AccountHolder accountData) throws ParseException{
		// the sender, receiver, and user of device, and amount
		String receiverPublicKey = getReceiverPublicKey();
		String senderPublicKey = getSenderPublicKey();
		String thisUser = accountData.getPublicKeyAsBase64();
		double amount = getAmount();

		// the asset
		Asset asset = getAsset();

		// start building the string
		final String you = "You";
		final SpannableStringBuilder sb = new SpannableStringBuilder();

		// check if user is sender else add the sender nickname
		if (senderPublicKey.equals(thisUser)){
			sb.append(you);
			final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
			sb.setSpan(bss, 0, you.length(), Spannable.SPAN_MARK_MARK);
		}else{
			try {
				sb.append(accountData.getNickname(senderPublicKey));
			} catch (NoNicknameException e) {
				sb.append("No Name");
			}
		}
		sb.append(" sent ");

		// how much
		sb.append(asset.getFormattedAmount(amount));
		sb.append(" to ");

		// repeat for receiver
		if (receiverPublicKey.equals(thisUser)){

			sb.append(you);
			final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
			sb.setSpan(bss, sb.length()-you.length(), sb.length(), Spannable.SPAN_MARK_MARK);
		}else{
			try {
				sb.append(accountData.getNickname(receiverPublicKey));
			} catch (NoNicknameException e) {
				sb.append("No Name");
			}
		}

		return sb;		
	}


	/**
	 * Decrypt the message with the given receiver. Must have private key
	 * @param privateKeyPair Will try to decode the message with the given pair of keys as either the receiver or sender
	 * @return The decrypted message
	 * @throws EncryptionException
	 * @throws IOException
	 * @throws IncorrectPasswordException
	 */
	public String getDecryptedMessage(PublicPrivateEncryptor privateKeyPair)
	throws EncryptionException, IOException, IncorrectPasswordException{
		com.tools.encryption.PublicPrivateEncryptor sender = new PublicPrivateEncryptor(null, getSenderPublicKey());
		com.tools.encryption.PublicPrivateEncryptor receiver = new PublicPrivateEncryptor(null, getReceiverPublicKey());
		byte[] mes = getEncryptedMessage();
		byte[] a = getEncryptedSecretKeyForMessage();
		byte[] b = getSignedMessage();
		byte[] c = getEncryptedSecretKeyForMessageForSender();
		if (mes == null || a==null || b ==null)
			return "";
		com.tools.encryption.EncryptedMessage message = new com.tools.encryption.EncryptedMessage(
				sender.getPublic(), receiver.getPublic(), mes, b, a, c);

		//verify the message
		if (!message.verifyMessage())
			throw new EncryptionException("Forged Message!");

		// check if we are receiver or sender
		return message.decryptMessage(privateKeyPair);
	}


	/**
	 * Convert a list of ParseObjects to Transactions
	 * @param parse
	 * @return
	 */
	public static List<Transaction> convertList(List<ParseObject> parse){
		List<Transaction> out = new ArrayList<Transaction>();
		for (ParseObject item : parse)
			out.add(new Transaction(item));
		return out;
	}

	/**
	 * Create and return the important info that will be hashed for signature.
	 * Base64SenderPublic + Base64ReceiverPublic + Amount + ItemType
	 * @param receiver The receiver
	 * @param sender The sender
	 * @return
	 */
	private String createHashString(
			com.tools.encryption.PublicPrivateEncryptor receiver,
			com.tools.encryption.PublicPrivateEncryptor sender){
		StringBuilder builder = new StringBuilder(1024);
		builder.append(sender.getPublicAsBase64());
		builder.append(receiver.getPublicAsBase64());
		builder.append(parse.get(Amount));
		builder.append(parse.get(ItemType));

		return builder.toString();
	}

	/**
	 * Get the string for the asset (item) id
	 * @return
	 */
	private String getItemId(){
		return getParse().getString(ItemType);
	}

	/**
	 * Get the sender's public key as a string
	 * @return
	 */
	private String getSenderPublicKey(){
		return parse.getString(SenderPublic);
	}

	/**
	 * Get the receiver's public key
	 * @return
	 */
	private String getReceiverPublicKey(){
		return parse.getString(ReceiverPublic);
	}

	/**
	 * Return the encrypted message
	 * @return
	 */
	private byte[] getEncryptedMessage(){
		return parse.getBytes(EncryptedMessage);
	}

	/**
	 * Get teh amount of the transaction
	 * @return
	 */
	public double getAmount(){
		return getParse().getDouble(Amount);
	}

	/**
	 * Return the signature of the encypted message
	 * @return
	 */
	private byte[] getSignedMessage(){
		return parse.getBytes(SignedMessage);
	}

	/**
	 * Return the encrypted secrete key used to decode message
	 * @return
	 */
	private byte[] getEncryptedSecretKeyForMessage(){
		return parse.getBytes(EncryptedSecretKeyMessage);
	}

	/**
	 * Return the encrypted secrete key used to decode message
	 * @return
	 */
	private byte[] getEncryptedSecretKeyForMessageForSender(){
		return parse.getBytes(EncryptedSecretKeyMessageForSender);
	}

	/**
	 * Check if we have a valid transaction. Will throw exception if invalid
	 * @param amount The amount of the transaction
	 * @param itemType The type of the transacation
	 * @throws InvalidTransactionException
	 */
	private void checkTransaction(double amount, String itemType) throws InvalidTransactionException{
		if (amount < 0)
			throw new InvalidTransactionException("Cannot perform negative transaction amounts");
	}

	public static class InvalidTransactionException
	extends Exception{
		private static final long serialVersionUID = -4911296298966420431L;

		public InvalidTransactionException(String message){
			super(message);
		}
	}

	/**
	 * Return the post linked to this comment
	 * @return
	 */
	//	public CatPicture getPost(){
	//		return new CatPicture(parse.getParseObject(POST));
	//	}

	/**
	 * Return the user who created this comment
	 * @return
	 */
	//	public CatUser getUser(){
	//		return new CatUser(parse.getParseUser(USER));
	//	}

	/**
	 * Return the comment string
	 * @return
	 */
	//public String getCommentString(){
	//	return parse.getString(COMMENT_STRING);
	//}

	/**
	 * The comment string
	 */
	//public String toString(){
	//		return getCommentString();
	//}
	//	
	/**
	 * Return the number of comments for a post <br>
	 * This queries the server, so should be called from a background thread
	 * @param post The post to query on
	 * @return the number of comments, returns -1 if there was an error
	 */
	/*
	public static int getNComments(CatPicture post){
		// query to get all teh votes associated with the input post
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(POST, post);
		int nComments = -1;
		try {
			nComments = query.count();
		} catch (ParseException e) {
			e.printStackTrace();
			Log.e(Utils.APP_TAG, e.getMessage());
		}
		return nComments;	
	}
	 */

	public static class UserNotInTransactionException
	extends Exception{

		/**
		 * 
		 */
		private static final long serialVersionUID = -5077039906232411977L;

		public UserNotInTransactionException(){
			super("User Not in Transaction");
		}
	}


}
