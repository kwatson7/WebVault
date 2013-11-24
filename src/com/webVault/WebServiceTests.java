package com.webVault;

import com.parse.ParseException;
import com.tools.Tools;
import com.tools.encryption.PublicPrivateEncryptor;
import com.webVault.serverobjects.Transaction;

import junit.framework.Assert;
import android.test.AndroidTestCase;


public class WebServiceTests extends AndroidTestCase {
    
	/*
    public void testDownloadGold() throws Throwable{
    	Gold gold = new Gold();
    	gold.downloadPrice();
    	Assert.assertTrue(true);
    }
    
    public void testUnknownAssetTransaction() throws Throwable{
    	PublicPrivateEncryptor receiver = PublicPrivateEncryptor.loadFromFile(Utils.getExternalStorageTopPath() + "receiver.key", "yes");
    	PublicPrivateEncryptor sender = PublicPrivateEncryptor.loadFromFile(Utils.getExternalStorageTopPath() + "sender.key", "yes");   		
    	Transaction transaction = new Transaction(receiver, sender, "itemType", 0, "sup friend");
    	try{
    		transaction.getParse().save();
    	}catch(ParseException e){
    		Assert.assertEquals(e.getMessage(), "Unknown asset");
    	}
    }
    */
    
    /*
    public void testCreateAsset() throws Throwable{
    	PublicPrivateEncryptor owner = PublicPrivateEncryptor.loadFromFile(Utils.getExternalStorageTopPath() + "receiver.key", "yes");
    	Asset asset = new Asset(owner, "Scaron", "1/300th of a kruggerand", "YahooStock", Gold.ticker, 4);
    	asset.getParse().save();
    }
    */
    

    public void testTransactionOfOwner() throws Throwable{
    	PublicPrivateEncryptor owner = PublicPrivateEncryptor.loadFromFile(Utils.getExternalStorageTopPath() + "scaronOwner.key", "yes");
    	PublicPrivateEncryptor receiver = PublicPrivateEncryptor.loadFromFile(Utils.getExternalStorageTopPath() + "kylesDefaultWebVaultKeys.key", "yes");   		
    	Transaction transaction = new Transaction(receiver, owner, Utils.SCARON_TYPE, 300, "First Transaction");
    	transaction.getParse().save();
    }

	
	public void test1() throws Throwable{

		    	String string = com.tools.Tools.convertMillisecondsToFormattedString(97243,  6.6667e-005);
		    	Assert.assertTrue(true);
		    
	}
    
}