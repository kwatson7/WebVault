package com.webVault;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.tools.CustomActivity;
import com.tools.DownloadFile;
import com.webVault.StockSymbol.DownloadCallback;

public class YahooStock extends StockSymbol{

	// private constants
	private static final String CHARSET_NAME = "UTF-8";
	private static final Charset CHARSET = Charset.forName(CHARSET_NAME);
	private static final String URL_BASE = "http://download.finance.yahoo.com/d/%20quotes.csv?e=.csv&f=sl1d1t1&s='";
	private static final int N_FIELDS = 4;
	private static final String DOWNLOAD_ERROR = "Data could not be downloaded";

	// private fields
	private double price;
	private Date date;
	private String symbol;

	public YahooStock(String symbol){
		this.symbol = symbol;
	}

	@Override
	public double getPrice() {
		return price;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public void downloadPrice() throws IOException {


		// file to write to
		String path = getFileToWriteTo();

		// download data
		DownloadFile downloader = new DownloadFile(getFullUrl());
		boolean success = downloader.downloadFile(path);
		if (!success){
			// delete temporary file
			if ((new File(path)).exists())
				(new File(path)).delete();
			throw new IOException(DOWNLOAD_ERROR);
		}

		try{
			// parse the data from file
			parseCsvFile(path);
		}finally{
			// delete temporary file
			(new File(path)).delete();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void downloadPriceInBackground(final DownloadCallback callback) {

		// file to write to. If we can't write then call callback with exception
		String path;
		try {
			path = getFileToWriteTo();
		} catch (IOException e) {
			callback.onDownloadUiThread(e);
			return;
		}

		// download data
		DownloadFile downloader = new DownloadFile(getFullUrl());
		downloader.downloadFileBackground(null, path, false, null, new DownloadFile.GetFileCallback(){

			@Override
			public void onPostFinished(CustomActivity act, boolean result,
					String fileName) {

				IOException exception = null;

				// if not successful, then try to delete file and quit
				if (!result){
					if ((new File(fileName)).exists())
						(new File(fileName)).delete();
					exception = new IOException(DOWNLOAD_ERROR);
				}else{
					// otherwise parse it
					try {
						parseCsvFile(fileName);
					} catch (IOException e) { 
						exception = e;
					}finally{
						(new File(fileName)).delete();
					}
				}

				// outside callback
				callback.onDownloadBackGroundThread(exception);
			}

			@Override
			public void onPostFinishedUiThread(CustomActivity act,
					boolean result, String fileName) {
				if (result)
					callback.onDownloadUiThread(null);
				else
					callback.onDownloadUiThread(new IOException(DOWNLOAD_ERROR));
			}});


	}

	/**
	 * Get the full url for this particular symbol
	 * @return
	 */
	private String getFullUrl(){
		return URL_BASE + symbol + "'";
	}

	/**
	 * Get the ticker with quotes surrounding it
	 * @return
	 */
	private String getFullTicker(){
		return "\"" + symbol + "\"";
	}

	/**
	 * Determine which file to write to. It will be a random file on the external directory
	 * @return The file
	 * @throws IOException
	 */
	private String getFileToWriteTo() throws IOException{
		// determine where to save file
		String path = Utils.getExternalStorageTopPath();
		if (path == null)
			throw new IOException("Cannot write to external storage");

		// create a random file to write to
		path = path + com.tools.Tools.randomString(20) + ".tmp";

		return path;
	}

	/**
	 * Create a gold price object from a correctly formatted csv file
	 * @param csvFile The csv file downloaded from yahoo
	 * @throws IOException 
	 */
	private void parseCsvFile(String csvFile) throws IOException{

		// read file
		byte[] data = com.tools.Tools.readFile(csvFile);
		if (data == null)
			throw new IOException("File could not be read");
		String string = new String(data, CHARSET);

		// split by commas
		ArrayList<String> items = new ArrayList<String>(Arrays.asList(string.split("\\s*,\\s*")));

		// make sure has 4 items
		if (items.size() != N_FIELDS)
			throw new IOException("File is not formatted correctly. Expect 4 fields");

		// check ticker
		if (!items.get(0).equals(getFullTicker()))
			throw new IOException("Ticker symbol is not correct. Received " + items.get(0));

		// strip off end of line
		items.set(3, items.get(3).replaceAll("\\r\\n", ""));

		// create date
		SimpleDateFormat format = new SimpleDateFormat("\"MM/dd/yyyy\"\"hh:mma\"", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
		try {
			date = format.parse(items.get(2)+items.get(3));
		} catch (ParseException e) {
			throw new IOException("Bad file format: " + items.get(2)+items.get(3));
		}

		// grab price
		price = Double.parseDouble(items.get(1));
	}
}
