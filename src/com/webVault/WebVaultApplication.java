package com.webVault;
import com.parse.Parse;
import com.parse.ParseACL;

import com.parse.ParseUser;

import android.app.Application;

public class WebVaultApplication
extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Add your initialization code here
		Parse.initialize(this, "k35dEfzDIjK53MjGioLCl87R1VAfpspSmkQcpbnB",
				"dfIOznmjzlwESiN40M3HkT34Wan5oI3lGYfvcc5w"); 

		ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
		// Optionally enable public read access by default.
		defaultACL.setPublicReadAccess(true);
		defaultACL.setPublicWriteAccess(true);
		ParseACL.setDefaultACL(defaultACL, true);
	}

}
