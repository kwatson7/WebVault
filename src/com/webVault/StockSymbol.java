package com.webVault;

import java.io.IOException;
import java.util.Date;

/**
 * Stock Symbol interface to hold date and time of stock
 * @author Kyle
 *
 */
public abstract class StockSymbol{
	
	/**
	 * Get the latest price of stock
	 * @return
	 */
	public abstract double getPrice();
	/**
	 * Get the date the latest price was updated
	 * @return
	 */
	public abstract Date getDate();
	/**
	 * Download the data on the current thread
	 * @throws IOException
	 */
	public abstract void downloadPrice() throws IOException;
	/**
	 * Download the data on a background thread
	 * @param callback The callback to run when finished
	 */
	public abstract void  downloadPriceInBackground (DownloadCallback callback);
	
	public interface DownloadCallback{
		/**
		 * NOT GUARANTEED TO BE CALLED. Will not be called if errors occur before we enter background thread.
		 * @param exception If any exception occured. Null if successful
		 */
		public void onDownloadBackGroundThread(IOException exception);
		/**
		 * Called on main calling thread when finished downloading.
		 * @param exception If any exception occured. Null if successful.
		 */
		public void onDownloadUiThread(IOException exception);
	}
}
