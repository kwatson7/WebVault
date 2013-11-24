package com.webVault.serverobjects;

import java.util.HashMap;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.webVault.CloudKeys;

public class CloudFunctions {

	/**
	 * Get the account value for a given asset type. Called on calling thread
	 * @param userPublicStringBase64
	 * @param assetType
	 * @return
	 * @throws ParseException
	 */
	public static double getAccountValue(String userPublicStringBase64, String assetType) throws ParseException{

		// setup values for passing to cloud function
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(CloudKeys.GetUserValue.KEY_USER, userPublicStringBase64);
		params.put(CloudKeys.GetUserValue.KEY_ITEM_TYPE, assetType);

		// get the value
		Object value = null;
		value = ParseCloud.<Object>callFunction(CloudKeys.GetUserValue.COMMAND, params);

		// convert to double
		return convertObjectToDouble(value);
	}

	/**
	 * Convert an object that should be either an integer, float or double to a double output from cloud code
	 * @param value the object to convert
	 * @return the new double
	 * @throws ParseException with code CloudKeys.GetUserValue.EXCEPTION_CODE_UNKNOWN_RETURN_TYPE if is not integer, double, or float
	 */
	private static double convertObjectToDouble(Object value) throws ParseException{
		double output = 0;
		if (value instanceof Integer){
			output = ((Integer) value).doubleValue();
		}else if (value instanceof Float){
			output = ((Float) value).doubleValue();
		}else if (value instanceof Double){
			output = (Double) value;
		}else{
			throw new ParseException(CloudKeys.GetUserValue.EXCEPTION_CODE_UNKNOWN_RETURN_TYPE, CloudKeys.GetUserValue.EXCEPTION_UNKNOWN_RETURN_TYPE);
		}

		return output;
	}

}
