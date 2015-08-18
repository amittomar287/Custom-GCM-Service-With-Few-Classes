package com.android.buzz.main;

public interface Config {
	// CONSTANTS
    // Google project id
    static final String GOOGLE_SENDER_ID = "322915043733";  // Place here your Google project id
	// static final String GOOGLE_SENDER_ID = "9432966778899";
    /**
     * Tag used on log messages.
     */
    static final String TAG = "bluetooth buzz GCM";

    static final String DISPLAY_MESSAGE_ACTION =
            "com.buzz.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";
		
	
}

