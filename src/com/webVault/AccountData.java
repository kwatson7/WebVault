package com.webVault;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;

import com.tools.ExpiringValue;
import com.tools.encryption.EncryptionException;
import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.tools.encryption.SymmetricEncryptor;

/**
 * Class that holds sensitive account data. This should not be saved to permanent memory non encrypted
 * @author Kyle
 *
 */
public class AccountData {
	// private constants
	private static final float secondsToExpirePrivateData = 300;
	
	/**
	 * Static account data
	 */
	private static com.tools.ExpiringValue<AccountHolder> accountHolder;

	/**
	 * Class to actually hold the data
	 * @author Kyle
	 *
	 */
	public static class AccountHolder{
		// private variables
		/**
		 * The current user's public and private key
		 */
		private com.tools.encryption.PublicPrivateEncryptor userKey;
		/**
		 * A lookup table of nicknames (values) and public keys (keys)
		 */
		private HashMap<String, String> nicknameLookup = new HashMap<String, String>();
		
		/**
		 * The password used to retrieve save data
		 */
		private String password;
		
		/**
		 * Construct an account data object
		 * @param userKey the public and private key of current user
		 * @param nicknameLookup a lookup table with nickname as value and public key as key
		 * @param password password to store data to file
		 */
		private AccountHolder(PublicPrivateEncryptor userKey, HashMap<String, String> nicknameLookup, String password){
			this.userKey = userKey;
			this.nicknameLookup = nicknameLookup;
			this.password = password;
		}
		
		/**
		 * Save the data to the correct files
		 * @param ctx Context required to save
		 * @param password The password to use
		 * @throws IOException
		 * @throws EncryptionException
		 */
		private void saveData(Context ctx)
				throws IOException, EncryptionException{
			userKey.saveToFile(Prefs.getKeyFile(ctx), password);
			com.tools.encryption.SymmetricEncryptor encryptor = new SymmetricEncryptor(password);
			encryptor.encryptByteArrayToFile(hashMapToByteArray(nicknameLookup), Prefs.getNickNameLookupFile(ctx));
		}
		
		/**
		 * Add a key to the list of saved keys with a nickname referencing it
		 * @param userPublicKey The public key to save
		 * @param nickname the nickname used to reference
		 * @throws EncryptionException 
		 * @throws IOException 
		 * @throws NicknameExistsException 
		 */
		public void addKey(String userPublicKey, String nickname, Context ctx) throws IOException, EncryptionException{
			nicknameLookup.put(userPublicKey, nickname);
			saveData(ctx);
		}
		
		/**
		 * Add a key with the key used as the nickname also
		 * @param userPublicKey the key to save
		 * @throws EncryptionException 
		 * @throws IOException 
		 */
		public void addKey(String userPublicKey, Context ctx) throws IOException, EncryptionException{
			if (nicknameLookup.containsKey(userPublicKey))
				return;
			else{
				nicknameLookup.put(userPublicKey, userPublicKey);
				saveData(ctx);
			}
		}
		
		/**
		 * Get the nickname for the given key
		 * @param userPublicKey The usuer's public key to lookup
		 * @return the nickname, null if none exists
		 */
		public String getNickname(String userPublicKey){
			return nicknameLookup.get(userPublicKey);
		}

		/**
		 * Return the public key as a base64 string
		 * @return
		 */
		public String getPublicKeyAsBase64(){
			return userKey.getPublicAsBase64();
		}
		
		/**
		 * Return the user of the device's keys
		 * @return
		 */
		public PublicPrivateEncryptor getUserKey(){
			return userKey;
		}
		
		/**
		 * Return the list of users and their nicknames as an array list
		 * @return
		 */
		public KeyNickname[] getUsersKeys(){
			Set<Entry<String, String>> map = nicknameLookup.entrySet();
			KeyNickname[] out = new KeyNickname[map.size()];
			Iterator<Entry<String, String>> iterator = map.iterator();
			int i = 0;
			while (iterator.hasNext()){
				Entry<String, String> item = iterator.next();
				out[i] = new KeyNickname(item.getKey(), item.getValue());
				i++;
			}
			return out;
		}
	}
	
	public static class KeyNickname{
		public String publicKey;
		public String nickname;
		
		private KeyNickname(String publicKey, String nickname){
			this.publicKey = publicKey;
			this.nickname = nickname;
		}
	}

	/**
	 * Return the private account data. 
	 * @return
	 * @throws TimeoutException if a timeout has occurred, and they will have to re-enter their password
	 */
	public static AccountHolder getAccountData() throws TimeoutException{
		// no data, so generate a new one
		if (accountHolder == null){
			accountHolder = new ExpiringValue<AccountHolder>(secondsToExpirePrivateData, null, null, true);
		}

		// timeout, so throw exception
		AccountHolder data = accountHolder.getValue();
		if (data == null)
			throw new TimeoutException();

		// return it
		return data;
	}
	
	/**
	 * Load the data stored on device
	 * @param ctx Required to read
	 * @param password password of stored data
	 * @return the account data
	 * @throws IOException
	 * @throws IncorrectPasswordException
	 * @throws EncryptionException
	 */
	public static AccountHolder loadData(Context ctx, String password)
			throws IOException, IncorrectPasswordException, EncryptionException{
		// user keys
		com.tools.encryption.PublicPrivateEncryptor userKey = PublicPrivateEncryptor.loadFromFile(
				Prefs.getKeyFile(ctx), password);

		// nickname dictionary
		String file = Prefs.getNickNameLookupFile(ctx);
		HashMap<String, String> nicknameLookup;
		if ((new File(file)).exists()){
			com.tools.encryption.SymmetricEncryptor encryptor = new SymmetricEncryptor(password);
			byte[] data = encryptor.decryptFileToByteArray(file, true);	

			// convert to hashmap		 
			try {
				nicknameLookup = byteArrayToHashMap(data);
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
		}else{
			nicknameLookup = new HashMap<String, String>();
		}
		
		AccountHolder dataOut = new AccountHolder(userKey, nicknameLookup, password);
		accountHolder.setValue(dataOut);
		return dataOut;
	}

	/**
	 * convert a byte array to a hashmap
	 * @param byteArray
	 * @return
	 * @throws StreamCorruptedException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static HashMap<String, String> byteArrayToHashMap(byte[] byteArray)
			throws StreamCorruptedException, IOException, ClassNotFoundException{
		ByteArrayInputStream byteIn = new ByteArrayInputStream(byteArray);
		ObjectInputStream in = new ObjectInputStream(byteIn);
		HashMap<String, String> data2 = (HashMap<String, String>) in.readObject();
		return data2;
	}

	/**
	 * Convert hashmap to byte array
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static byte[] hashMapToByteArray(HashMap<String, String> data) throws IOException{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteOut);
		out.writeObject(data);
		return byteOut.toByteArray();
	}

	public static class TimeoutException extends Exception{

		/**
		 * 
		 */
		private static final long serialVersionUID = -4763961531446112832L;

		public TimeoutException(){
			super("Credentials have timedout");
		}

	}
}
