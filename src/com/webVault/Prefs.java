package com.webVault;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	
	// strings for accessing preferences file
	public static final String PREF_FILE = "appInfo.prefs";
	private static final int MODE_PRIVATE = Context.MODE_PRIVATE;
	private static final String KEY_FILE = "KEY_FILE";
	private static final String NICKNAME_FILE = "NICKNAME_FILE";
	private static final String PATTERN_ARRAY = "PATTERN_ARRAY";
	
	// default values
	public static final String DEFAULT_STRING = null;
	public static final long DEFAULT_LONG = -1;
	public static final int DEFAULT_INT = -1;
	public static final boolean DEFAULT_BOOLEAN = false;
	
	// debugging strings
	public static class debug{
		/** force the id of the user to have a value of 1 and "secret" */
		public static final boolean forceId = false;
		/** allow for multiple updates to database rows when we dont' expect it, or throw exception othrwise */
		public static final boolean allowMultipleUpdates = false;
	}
	
	/**
	 * get the file where the user's keys are stored
	 * @return the user name. Null if it does not exist
	 */
	public static String getKeyFile(Context ctx){
		return getStringPref(ctx, KEY_FILE);
	}
	
	/**
	 * Set the file where the user's keys are stored
	 * @param ctx Context required to set prefs
	 * @param keyFile The actual file name with full path
	 */
	public static void setKeyFile(Context ctx, String keyFile){
		setStringPref(ctx, KEY_FILE, keyFile);
	}
	
	/**
	 * Get the encrypted file where the list of nicknames and corresponding public keys are stored
	 * @param ctx
	 * @return
	 */
	public static String getNickNameLookupFile(Context ctx){
		String name = getStringPref(ctx, NICKNAME_FILE);
		if (name == null)
			name = Utils.getDefaultNickNameFullPath();
		return name;
	}
	
	/**
	 * Set the encrypted file where the list of nicknames and corresponding public keys are stored
	 * @param ctx Context
	 * @param file the full file
	 */
	public static void setNickNameLookupFile(Context ctx, String file){
		setStringPref(ctx, NICKNAME_FILE, file);
	}
	
	/**
	 * Return the char array to check against when the user enters a pattern
	 * @param ctx
	 * @return
	 */
	public static char[] getPatternArray(Context ctx){
		String t = getStringPref(ctx, PATTERN_ARRAY);
		if (t == null)
			return null;
		else
			return t.toCharArray();
	}
	
	/**
	 * Set the pattern array for the pattern to get secret info
	 * @param ctx
	 * @param pattern
	 */
	public static void setPatternArray(Context ctx, char[] pattern){
		setStringPref(ctx, PATTERN_ARRAY, new String(pattern));
	}
	
	// private methods used for helper inside this class
	/**
	 * Get the preference object
	 * @param ctx The context used to access object
	 * @return
	 */
	private static SharedPreferences getPrefs(Context ctx) {
		return ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE);
    }
	
    private static String getStringPref(Context ctx, String pref) {
        return getPrefs(ctx).getString(pref, DEFAULT_STRING);
    }
    public static void setStringPref(Context ctx, String pref, String value) {
        getPrefs(ctx).edit().putString(pref, value).commit();
    }
    
    private static long getLongPref(Context ctx, String pref) {
        return getPrefs(ctx).getLong(pref, DEFAULT_LONG);
    }
    public static void setLongPref(Context ctx, String pref, long value) {
        getPrefs(ctx).edit().putLong(pref, value).commit();
    }
    public static void setBooleanPref(Context ctx, String pref, boolean value){
    	getPrefs(ctx).edit().putBoolean(pref, value).commit();
    }
    public static boolean getBooleanPref(Context ctx, String pref, boolean defaultValue){
    	return getPrefs(ctx).getBoolean(pref, defaultValue);
    }
}