package com.webVault;

/**
 * Nested classes holding keys for server commands
 */
public class CloudKeys {
	
	public static class GetUserValue extends CloudKeys{
		public static final String COMMAND = "userValue";
		public static final String KEY_USER = "User";
		public static final String KEY_ITEM_TYPE = "ItemType";
		
		public static final int EXCEPTION_CODE_UNKNOWN_RETURN_TYPE = 863;
		public static final String EXCEPTION_UNKNOWN_RETURN_TYPE = "Unknown return type";
	}
}