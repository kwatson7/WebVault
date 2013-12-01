package com.webVault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.tools.encryption.IncorrectPasswordException;
import com.tools.encryption.PublicPrivateEncryptor;
import com.tools.encryption.SymmetricEncryptor;

import junit.framework.Assert;
import junit.framework.TestCase;
import android.test.AndroidTestCase;


public class EncryptionTests extends AndroidTestCase {

	String path = Utils.getExternalStorageTopPath();
	String originalFile = path + "test.txt";
	String encryptFile = path + "testEncrypt.txt";
	String decryptFile = path + "testDecrypt.txt";
	String textInFile = "hello text";
	String passwordCorrect = "Password";
	String passwordIncorrect = "passwordIncorrect";
	String pubPrivKeyFile = path + "key.txt";
    
    private String getPath(){
    	return Utils.getExternalStorageTopPath();
    }
    
    public void testFileEncrypt() throws Throwable{
		// make a file
		PrintStream out = null;
		try{
			File file = new File(originalFile);
			if (!file.exists()){
				com.tools.Tools.writeRequiredFolders(file.getAbsolutePath());
				file.createNewFile();
			}
			out = new PrintStream(new FileOutputStream(originalFile));

			out.print(textInFile);
		}
		finally {
			if (out != null) out.close();
		}

		// now encrypt the file
		SymmetricEncryptor encryptor = new SymmetricEncryptor(passwordCorrect);
		encryptor.encryptFile(originalFile, encryptFile, true);

		// now decrypt
		encryptor.decryptFile(encryptFile, decryptFile, true);
		
		// check if equal
		Assert.assertTrue(com.tools.Tools.isFileEqual(originalFile,  decryptFile));
		String content = encryptor.readEncryptedFile(encryptFile);
		Assert.assertTrue(content.equals(textInFile));
    }
    
    public void testDecryptNewInstance() throws Throwable{

    	SymmetricEncryptor encryptor2 = new SymmetricEncryptor(passwordCorrect);
    	encryptor2.decryptFile(encryptFile, decryptFile, true);

    	// check if equal
    	Assert.assertTrue(com.tools.Tools.isFileEqual(originalFile,  decryptFile));

    	String content = encryptor2.readEncryptedFile(encryptFile);
		Assert.assertTrue(content.equals(textInFile));
    }
    
    public void testWrongPasswordSymmetricEncryptor() throws Throwable{
		
    	SymmetricEncryptor encryptor2 = new SymmetricEncryptor(passwordIncorrect);
		try {
			encryptor2.decryptFile(encryptFile, decryptFile, true);
			Assert.assertTrue(false);
		} catch (com.tools.encryption.IncorrectPasswordException e) {
			Assert.assertTrue(true);
		}

		try {
			String content = encryptor2.readEncryptedFile(path + "testEncrypt.txt");
			Assert.assertTrue(false);
		} catch (com.tools.encryption.IncorrectPasswordException e) {
			Assert.assertTrue(true);
		}
    }
    
    public void testPubPriv() throws Throwable{
    	// create and write encrypt to file and check read properly
    	PublicPrivateEncryptor enc = PublicPrivateEncryptor.createFromNewKeyPair();
    	PrivateKey a = enc.getPrivate();
    	PublicKey b = enc.getPublic();

    	enc.saveToFile(pubPrivKeyFile, passwordCorrect);

    	PublicPrivateEncryptor enc2 = PublicPrivateEncryptor.loadFromFile(pubPrivKeyFile, passwordCorrect);
    	PrivateKey c = enc2.getPrivate();
    	PublicKey d = enc2.getPublic();
    	
    	// test that the keys were saved and retrieved properly
    	Assert.assertEquals(a, c);
    	Assert.assertEquals(b, d);
    	
    	// test the signing process
    	byte[] aa = "test encode".getBytes("UTF-8");
    	byte[] bb = enc.signData(aa);
    	Assert.assertTrue(enc.verifyData(aa, bb));
    }
    
    public void testPubPrivIncorrectPassword() throws Throwable{
    	// create and write encrypt to file and check read properly
    	PublicPrivateEncryptor enc = PublicPrivateEncryptor.createFromNewKeyPair();
    	PrivateKey a = enc.getPrivate();
    	PublicKey b = enc.getPublic();

    	enc.saveToFile(pubPrivKeyFile, passwordCorrect);

    	try{
    		PublicPrivateEncryptor enc2 = PublicPrivateEncryptor.loadFromFile(pubPrivKeyFile, passwordIncorrect);
    		Assert.assertTrue(false);
    	}catch(IncorrectPasswordException e){
    		Assert.assertTrue(true);
    	}
    }

		
    public void testEncryptString() throws Throwable{
    	SymmetricEncryptor encryptor = new SymmetricEncryptor(passwordCorrect);	
		byte[] encryp = encryptor.encryptStringToByteArray(textInFile);
		String dec = encryptor.decryptByteArrayToString(encryp);
		
		Assert.assertEquals(dec, textInFile);
		
		// test wrong password
		SymmetricEncryptor encryptor2 = new SymmetricEncryptor(passwordIncorrect);	
		try{
			encryptor2.decryptByteArrayToString(encryp);
			Assert.assertTrue(false);
		}catch(IncorrectPasswordException e){
			Assert.assertTrue(true);
		}
    }
    
    public void testEncryptStringToFile() throws Throwable{
    	SymmetricEncryptor encryptor = new SymmetricEncryptor(passwordCorrect);	
    	encryptor.encryptStringToFile(textInFile, encryptFile);
    	String content = encryptor.readEncryptedFile(encryptFile);
    	Assert.assertEquals(textInFile, content);
    	
    	// now wrong password
    	SymmetricEncryptor encryptor2 = new SymmetricEncryptor(passwordIncorrect);	
    	try{
			encryptor2.readEncryptedFile(encryptFile);
			Assert.assertTrue(false);
		}catch(IncorrectPasswordException e){
			Assert.assertTrue(true);
		}    	
    }
    
    public void testSendMessage() throws Throwable{

		// test sending the message
		PublicPrivateEncryptor sender = PublicPrivateEncryptor.createFromNewKeyPair();
		PublicPrivateEncryptor receiver = PublicPrivateEncryptor.createFromNewKeyPair();
		String message0 = "test out my encrypted message tool!";
		
		// create the encrypted message
		com.tools.encryption.EncryptedMessage encryptedMessage = new com.tools.encryption.EncryptedMessage(
				sender.getPublic(), receiver.getPublic(), message0, sender.getPrivate());
		
		// check signature
		Assert.assertTrue(encryptedMessage.verifyMessage());
		
		// check message
		String message = encryptedMessage.decryptMessageForReceiver(receiver.getPrivate());
		Assert.assertEquals(message, message0);
		
		// do again with new instance
		com.tools.encryption.EncryptedMessage encryptedMessage2 = new com.tools.encryption.EncryptedMessage(
				sender.getPublic(),
				receiver.getPublic(),
				encryptedMessage.encryptedMessage,
				encryptedMessage.signedMessage,
				encryptedMessage.encryptedSecretKey,
				encryptedMessage.encryptedSecretKeyForSender);
		Assert.assertTrue(encryptedMessage2.verifyMessage());
		String message2 = encryptedMessage2.decryptMessageForReceiver(receiver.getPrivate());
		Assert.assertEquals(message2, message0);
    }
    
    public void testPublicPrivate() throws Throwable{
    	// generate new keypair
    	PublicPrivateEncryptor encryptor = PublicPrivateEncryptor.createFromNewKeyPair();
    	
    	// encyrpt a message and decrypt
    	String message = "hello friend";
    	byte[] encrypted = encryptor.encryptWithPublic(message.getBytes(Charset.forName("UTF-8")));
    	String message2 = new String(encryptor.decryptWithPrivate(encrypted), "UTF-8");
    	Assert.assertEquals(message, message2);
    	
    	// do it again with nonce
    	encrypted = encryptor.encryptWithPublic(message.getBytes(Charset.forName("UTF-8")), 4);
    	message2 = new String(encryptor.decryptWithPrivate(encrypted, 4), "UTF-8");
    	Assert.assertEquals(message, message2);
    }
}