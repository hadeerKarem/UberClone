/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {

  Switch switchUberServices;
  String TAG = "MainActivityLogTag";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportActionBar().hide();

    switchUberServices = findViewById(R.id.switchUberServices);

    if (ParseUser.getCurrentUser() == null) {
      ParseAnonymousUtils.logIn(new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
          if (e == null) {
            Log.i(TAG, "Anonymous Login succeed");
          } else {
            Log.i(TAG, "Anonymous Login failed");
          }
        }
      });
    }
    else {
      if (ParseUser.getCurrentUser().get("userType") != null) {
        Log.i(TAG, "Redirecting as " + ParseUser.getCurrentUser().get("userType"));
        redirectActivity();
      }
    }

    switchUberServices.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "switch value: " + isChecked);
      }
    });
    
    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

  public void getStartedClicked(View view) {

    String userType;
    if (switchUberServices.isChecked()) {
      userType = "driver";
    } else {
      userType = "rider";
    }
    ParseUser.getCurrentUser().put("userType", userType);
    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
      @Override
      public void done(ParseException e) {
        if (e == null) {
          Log.i(TAG, "Redirecting as " + ParseUser.getCurrentUser().get("userType"));
          redirectActivity();
        } else {
          e.printStackTrace();
        }
      }
    });
  }

  public void redirectActivity() {
    if (ParseUser.getCurrentUser().get("userType").equals("rider")) {
      Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
      startActivity(intent);

    } else {
      Intent intent = new Intent(getApplicationContext(), ViewRequestsActivity.class);
      startActivity(intent);
    }
  }
}