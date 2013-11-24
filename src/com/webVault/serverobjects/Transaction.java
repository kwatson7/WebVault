package com.webVault.serverobjects;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;


import android.app.ProgressDialog;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.webVault.Tasks.AccountValueTask;
import com.webVault.Tasks.AccountValueTask.Output;

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

		// generate the string we will hash and add nonce
		String hashString = createHashString(receiver, sender);
		byte[] hashBytes = com.tools.encryption.PublicPrivateEncryptor.appendNOnceToString(hashString, N_BYTES_N_ONCE);

		// now sign it
		parse.put(HashArray, hashBytes);
		parse.put(Signature, sender.signData(hashBytes));
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
	 * Decrypt the message with the given receiver. Must have private key
	 * @param receiver The receiver with pair of keys
	 * @return The decrypted message
	 * @throws EncryptionException
	 * @throws IOException
	 * @throws IncorrectPasswordException
	 */
	public String getDecryptedMessage(PublicPrivateEncryptor receiver)
			throws EncryptionException, IOException, IncorrectPasswordException{
		com.tools.encryption.PublicPrivateEncryptor sender = new PublicPrivateEncryptor(null, getSenderPublicKey());
		com.tools.encryption.EncryptedMessage message = new com.tools.encryption.EncryptedMessage(
				sender.getPublic(), receiver.getPublic(), getEncryptedMessage(), getSignedMessage(), getEncryptedSecretKeyForMessage());
		return message.decryptMessage(receiver.getPrivate());
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


}
