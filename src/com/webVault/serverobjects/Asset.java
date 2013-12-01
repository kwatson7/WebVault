package com.webVault.serverobjects;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.IInterface;
import android.text.Html;
import android.text.Spanned;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.tools.encryption.EncryptionException;
import com.webVault.StockSymbol;
import com.webVault.StockSymbol.DownloadCallback;
import com.webVault.YahooStock;

public class Asset{

	// public constants
	public static final int EXCEPTION_CODE_UNKNOWN_ASSET = 4173;
	public static final String EXCEPTION_STRING_UNKNOWN_ASSET = "Unknown Asset";
	
	// the object name
	private static final String OBJECT_NAME = "Asset"; 					// the object name for the ParseObject
	private static final int N_BYTES_EXTRA_HASH = 4; 						// number of bytes for nonce

	// the fields
	private static final String OwnerPublic = "OwnerPublic";  			// owner of the Asset
	private static final String Description = "Description"; 			// encrypted message
	private static final String Name = "Name"; 							// name of the unit
	private static final String Premium = "Premium"; 					// the premium as (%) to add to underlying online asset
	private static final String HashArray = "HashArray"; 				// the important aspects of message that will be hashed
	private static final String Signature = "Signature"; 				// hash of hash_string that verifies sender actually sent with private key
	private static final String DownloadService = "DownloadService"; 	// which service to use to download data
	private static final String Ticker = "Ticker"; 						// ticker symbol
	private static final String ObjectId = "objectId";
	private static final String FormattedSymbol = "FormattedSymbol";	// the symbol that can be parsed as html
	private static final String ConversionRatio = "ConversionRatio";

	// misc private variables
	private ParseObject parse; 											// The parse object this wraps around

	/**
	 * Create a new asset class
	 * @param owner The owner who is creating it
	 * @param name The name of the asset (eg "gold")
	 * @param description The longer description of the asset
	 * @param downloadService The string describing the download service to use to download the data
	 * @param ticker the ticker to use with downloadService
	 * @param formattedSymbol the symbol that can be formatted via html
	 * @param premium Any premium to add to underlying ticker
	 * @throws EncryptionException
	 */
	public Asset(
			com.tools.encryption.PublicPrivateEncryptor owner,
			String name,
			String description,
			String downloadService,
			String ticker,
			String formattedSymbol,
			double premium,
			double conversionRatio
			) throws EncryptionException{

		parse = new ParseObject(OBJECT_NAME);

		// put the data into the parse object
		parse.put(OwnerPublic, owner.getPublicAsBase64());
		parse.put(Name, name);
		parse.put(Description, description);
		parse.put(Premium, premium);
		parse.put(DownloadService, downloadService);
		parse.put(FormattedSymbol, formattedSymbol);
		parse.put(Ticker, ticker);
		parse.put(ConversionRatio, conversionRatio);

		// generate the string we will hash and add nonce
		String hashString = createHashString(owner, name);
		byte[] hashBytes = com.tools.encryption.PublicPrivateEncryptor.appendNOnceToString(hashString, N_BYTES_EXTRA_HASH);

		// now sign it
		com.tools.encryption.PublicPrivateEncryptor publicPrivate = new com.tools.encryption.PublicPrivateEncryptor(owner.getPrivate(), null);
		parse.put(HashArray, hashBytes);
		parse.put(Signature, publicPrivate.signData(hashBytes));
	}
	
	/**
	 * Fetch the price of the asset in default currency units (probably dollars) plus the premium
	 * @param callback
	 */
	public void fetchPriceInBackground(final PriceDownloadCallback callback){
		
		// read values from the parse object
		ParseObject parse = getParse();
		String downloadService = parse.getString(DownloadService);
		String symbol = parse.getString(Ticker);
		final double conversion = parse.getDouble(ConversionRatio);
		final double premium = parse.getDouble(Premium)/100;
		
		// get the type of Service
		final StockSymbol stock;
		if (downloadService.equalsIgnoreCase("YahooStock")){
			stock = new YahooStock(symbol);
		}else
			stock = null;
		
		
		// download the price
		stock.downloadPriceInBackground(new DownloadCallback() {
			
			@Override
			public void onDownloadUiThread(IOException exception) {
				double price = stock.getPrice();
				callback.onDownloadUiThread(price*(1+premium)*conversion, exception);
			}
			
			@Override
			public void onDownloadBackGroundThread(IOException exception) {
				double price = stock.getPrice();
				callback.onDownloadBackGroundThread(price*(1+premium)*conversion, exception);
			}
		});
			
	}
	
	public interface PriceDownloadCallback{
		/**
		 * NOT GUARANTEED TO BE CALLED. Will not be called if errors occur before we enter background thread.
		 * @param exception If any exception occured. Null if successful
		 */
		public void onDownloadBackGroundThread(double price, IOException exception);
		/**
		 * Called on main calling thread when finished downloading.
		 * @param exception If any exception occured. Null if successful.
		 */
		public void onDownloadUiThread(double price, IOException exception);
	}

	/**
	 * Create a Transaction from a representative ParseObject
	 * @param parse
	 */
	public Asset(ParseObject parse){
		this.parse = parse;
	}
	
	/**
	 * Query the asset for the given object id
	 * @param objectId
	 * @return the asset
	 * @throws ParseException if error, or unknown asset
	 */
	public static Asset fetchAsset(String objectId) throws ParseException{
		// query the server for the asset with the given id
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK);
		ParseObject object = query.getFirst();
		if (object == null){
			throw new ParseException(EXCEPTION_CODE_UNKNOWN_ASSET, EXCEPTION_STRING_UNKNOWN_ASSET);
		}
		return new Asset(object);
	}

	public ParseObject getParse(){
		return parse;
	}
	
	/**
	 * The ID of the given asset
	 * @return
	 */
	public String getId(){
		return parse.getObjectId();
	}
	
	/**
	 * Return the formatted amount for this asset using its symbol. 
	 * EG if the symbol is @, and amount is 5, then output is @5.
	 * EG, or if symbol is $, and amount is 6.4, then output is $6.4
	 * @param amount the amount of the type to format with symbol
	 * @return the html formatted output
	 */
	public Spanned getFormattedAmount(double amount){
		DecimalFormat df = new DecimalFormat("#.####");
		return Html.fromHtml(getSymbol() + df.format(amount));
	}
	
	/**
	 * Return the formatted amount for this asset using its symbol. 
	 * EG if the symbol is @, and amount is 5, and positive is true then output is +@5.
	 * EG, or if symbol is $, and amount is 6.4, then output is $6.4
	 * @param amount the amount of the type to format with symbol
	 * @param positive true if the amount is positive, and negative otherwise
	 * @return the html formatted output
	 */
	public Spanned getFormattedAmount(double amount, boolean positive){
		DecimalFormat df = new DecimalFormat("#.####");
		if (positive)
			return Html.fromHtml("+" + getSymbol() + df.format(amount));
		else
			return Html.fromHtml("-" + getSymbol() + df.format(amount));
	}
	
	/**
	 * Return the symbol that represents the assset
	 * @return
	 */
	public String getSymbol(){
		return parse.getString(FormattedSymbol);
	}
	
	/**
	 * Return the name of the asset
	 * @return
	 */
	public String getName(){
		return parse.getString(Name);
	}

	/**
	 * Convert a list of ParseObjects to Transactions
	 * @param parse
	 * @return
	 */
	public static List<Asset> convertList(List<ParseObject> parse){
		List<Asset> out = new ArrayList<Asset>();
		for (ParseObject item : parse)
			out.add(new Asset(item));
		return out;
	}

	/**
	 * Query the cat pictures
	 * @param skip The number of queries to skip
	 * @param nNewQueries the number of new queries to get
	 * @param findCallback the callback to perform when done
	 */
	public static void queryAssets(int skip, int nNewQueries, FindCallback findCallback){

		// initialize query
		ParseQuery query = new ParseQuery(OBJECT_NAME);

		// set parameters of query
		query.setLimit(1000);

		// now perform the query
		query.findInBackground(findCallback);
	}

	public static void queryAsset(String id, FindCallback findCallback){
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(ObjectId, id);
		query.findInBackground(findCallback);
	}

	/**
	 * Create and return the important info that will be hashed for signature.
	 * Base64SenderPublic + Base64ReceiverPublic + Amount + ItemType
	 * @param receiver The receiver
	 * @param sender The sender
	 * @return
	 */
	private String createHashString(
			com.tools.encryption.PublicPrivateEncryptor owner,
			String name){
		StringBuilder builder = new StringBuilder(1024);
		builder.append(owner.getPublicAsBase64());
		builder.append(name);

		return builder.toString();
	}
}
