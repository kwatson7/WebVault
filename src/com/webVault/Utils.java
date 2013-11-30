package com.webVault;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.tools.ExpiringValue;

import android.content.Context;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {

	// public constants
	public static final String LOG_TAG = "WebVault";
	public static final char pathsep = '/';
	public static final String appPath = "Web Vault";
	public static final String SCARON_TYPE = "KRaIllDliR";
	
	// private constants
	private static final String DEFAULT_NICKNAME_FILE = "nicknames";
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	

	/**
	 * Return a string that is the path to top level app picture storage. It will have a path separator
	 * @return
	 */
	public static String getExternalStorageTopPath(){

		// grab the path
		File external = Environment.getExternalStorageDirectory();
		String path = external.getAbsolutePath();

		// now put / if not there
		if (path.length() > 1 && path.charAt(path.length()-1) != pathsep)
			path += pathsep;

		// now add this app directory
		path += appPath + pathsep;

		return path;
	}
	
	/**
	 * The default nickname path.
	 * @return
	 */
	public static String getDefaultNickNameFullPath(){
		return (new File(getExternalStorageTopPath(), DEFAULT_NICKNAME_FILE).getAbsolutePath());
	}
	
	/**
	 * Show a toast with the app specific custom layout
	 * @param ctx The context used to show 
	 * @param text The text to show
	 * @param showBottom true to show on bottom, false to show on top
	 * @param displayTimeFactor Multiply default display time by this number (standard is 1) 
	 */
	public static void showCustomToast(Context ctx, String text, boolean showBottom, float extraFactor){

		// if text is null, then assign
		if (text == null)
			text = "Unknown message";
		
		// constants
		final int PIXELS_FROM_EDGE = 20;
		
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// load the layout
		View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) null);
		TextView toast_text = (TextView) layout.findViewById(R.id.message);

		// set display properties
		Toast toast = new Toast(ctx.getApplicationContext());
		if (showBottom)
			toast.setGravity(Gravity.BOTTOM, 0, (int) com.tools.Tools.convertDpToPix(ctx, PIXELS_FROM_EDGE));
		else
			toast.setGravity(Gravity.TOP, 0, (int) com.tools.Tools.convertDpToPix(ctx, PIXELS_FROM_EDGE));
		toast.setView(layout);

		// set the text
		toast_text.setText(text);

		// show it for custom amount of time
		com.tools.ToastExpander.showFor(toast, (long) (com.tools.Tools.getTimeToReadForToast(text)*extraFactor));
	}
	
	/**
	 * Returns a formatted string representation of date. In the format: yyyy-MM-dd HH:mm:ss
	 * @param date
	 * @return
	 */
	public static String getFormattedDate(Date date){
		DateFormat formatter = new SimpleDateFormat(Utils.DATE_FORMAT);
		return formatter.format(date);
	}
}
