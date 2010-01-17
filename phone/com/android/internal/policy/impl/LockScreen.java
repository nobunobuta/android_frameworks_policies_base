/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.policy.impl;

import android.provider.Contacts.People;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.content.ContentUris;
import android.net.Uri;
import android.database.Cursor;

import android.os.LocalPowerManager;
import com.android.internal.R;
import android.app.StatusBarManager;
import com.android.internal.widget.LockPatternUtils;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.media.AudioManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.provider.Settings.System;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Date;
import android.util.Log;
import com.android.internal.telephony.IccCard;
import android.content.Intent;
import java.text.SimpleDateFormat;
/**
 * The screen within {@link LockPatternKeyguardView} that shows general
 * information about the device depending on its state, and how to get
 * past it, as applicable.
 */
class LockScreen extends LinearLayout implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
        KeyguardUpdateMonitor.SimStateCallback, KeyguardUpdateMonitor.ConfigurationChangeCallback {
    public static boolean fromportrait;
    public static boolean ShowBatteryInfo;
    private StatusBarManager mStatusBar;
    public static String WhatIsShowing = "Nothing";


    public void RememberStatusMod(boolean enabled) {
        Settings.Secure.putInt(this.getContext().getContentResolver(), "SET_STATUS_MOD", 
                                enabled ? 1 : 0);
    }
    public boolean mStatusmod() {
        return Settings.Secure.getInt(this.getContext().getContentResolver(), 
                                      "SET_STATUS_MOD", 0) > 0;
    }
    public void RememberDisplayMod(boolean enabled) {
        Settings.Secure.putInt(this.getContext().getContentResolver(), "SET_DISPLAY_MOD", 
                                enabled ? 1 : 0);
    }   
    public boolean mDisplaymod() {
        return Settings.Secure.getInt(this.getContext().getContentResolver(), 
                                      "SET_DISPLAY_MOD", 0) > 0;
    }

    public void RememberAnimationMod(boolean enabled) {
        Settings.Secure.putInt(this.getContext().getContentResolver(), "SET_ANIMATION_MOD", 
                                enabled ? 1 : 0);
    }
    public boolean mAnimationMod() {
        return Settings.Secure.getInt(this.getContext().getContentResolver(), 
                                      "SET_ANIMATION_MOD", 0) > 0;
    }

    public void RememberSimInfoMod(boolean enabled) {
        Settings.Secure.putInt(this.getContext().getContentResolver(), "SET_SIMINFO_MOD", 
                                enabled ? 1 : 0);
    }
    public boolean mSimInfoMod() {
        return Settings.Secure.getInt(this.getContext().getContentResolver(), 
                                      "SET_SIMINFO_MOD", 0) > 0;
    }

    public void RememberICEMod(boolean enabled) {
        Settings.Secure.putInt(this.getContext().getContentResolver(), "SET_ICE_MOD", 
                                enabled ? 1 : 0);
    }
    public boolean mICEMod() {
        return Settings.Secure.getInt(this.getContext().getContentResolver(), 
                                      "SET_ICE_MOD", 0) > 0;
    }


    private final static String TAG = "Lockscreen";
    private AudioManager am = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);

    private final LockPatternUtils mLockPatternUtils;
    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardScreenCallback mCallback;
    private int credits;
    private boolean minteractivitymod = false;
    private boolean begininteractivitymod = false;

    private ViewGroup mSIM;
    private TextView mHeaderSimOk1;
    private TextView mHeaderSimOk2;

    private TextView mHeaderSimBad1;
    private TextView mHeaderSimBad2;

    private TextView mTime;
    private TextView mDate;

    private ViewGroup mSettingsGroup;
    private CheckBox mModBox;
    private CheckBox mModBox2;
    private CheckBox mModBox3;
    private CheckBox mModBox4;
    private CheckBox mModBox5;
    private CheckBox mModBox6;


    private ViewGroup mBatteryInfoGroup;
    private ImageView mBatteryInfoIcon;
    private TextView mBatteryInfoText;

    private ViewGroup mMusicGroup;
    private ImageButton mrewindIcon;
    private ImageButton mplayIcon;
    private ImageButton mpauseIcon;
    private ImageButton mforwardIcon;

    private ViewGroup mICE;
    private ViewGroup mICEfound;
    private ViewGroup mICEnotfound;
    private ViewGroup mICEownermain;
    private TextView mICEowner;
    private ViewGroup mICEcontactmain;
    private TextView mICEcontact;
    private TextView mICEnotes;

    private View mSpaceAdjuster;
    private View mContentSpaceAdjuster;
 
    private ViewGroup mNotificationsGroup;
    private ImageButton msettingsIcon;
    private ImageButton mbatteryIcon;
    private ImageButton mmusicIcon;
    private ImageButton mICEIcon;
    private ViewGroup mTheuhdowhat;
    private ViewGroup mNextAlarmGroup;
    private TextView mAlarmText;

    private ViewGroup mScreenLockedMessageGroup;

    private TextView mLockInstructions;

    private Button mEmergencyCallButton;

    /**
     * false means sim is missing or PUK'd
     */
    private boolean mSimOk = true;

    // are we showing battery information?
    private boolean mShowingBatteryInfo = false;

    // last known plugged in state
    private boolean mPluggedIn = false;

    // last known battery level
    public static int mBatteryLevel = 100;


    private View[] mOnlyVisibleWhenSimOk;

    private View[] mOnlyVisibleWhenSimNotOk;

    private Animation loadAnim(int id, Animation.AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    } 

    /**
     * @param context Used to setup the view.
     * @param lockPatternUtils Used to know the state of the lock pattern settings.
     * @param updateMonitor Used to register for updates on various keyguard related
     *    state, and query the initial state at setup.
     * @param callback Used to communicate back to the host keyguard view.
     */
    LockScreen(Context context, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback) {
        super(context);
        mStatusBar = (StatusBarManager)mContext.getSystemService(Context.STATUS_BAR_SERVICE);

        mLockPatternUtils = lockPatternUtils;
        mUpdateMonitor = updateMonitor;
        mCallback = callback;

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.keyguard_screen_lock, this, true);

        mSimOk = isSimOk(updateMonitor.getSimState());
        mShowingBatteryInfo = updateMonitor.shouldShowBatteryInfo();
        mPluggedIn = updateMonitor.isDevicePluggedIn();
        mBatteryLevel = updateMonitor.getBatteryLevel();

        mSIM = (ViewGroup) findViewById(R.id.SIM);
        mHeaderSimOk1 = (TextView) findViewById(R.id.headerSimOk1);
        mHeaderSimOk2 = (TextView) findViewById(R.id.headerSimOk2);

        mHeaderSimBad1 = (TextView) findViewById(R.id.headerSimBad1);
        mHeaderSimBad2 = (TextView) findViewById(R.id.headerSimBad2);

        mTime = (TextView) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);

        mSettingsGroup = (ViewGroup) findViewById(R.id.settingsgroup);
        mModBox = (CheckBox) findViewById(R.id.modbox);

        mModBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (!KeyguardViewMediator.Securitycheck) {
                 if (isChecked)
                 {
                   RememberStatusMod(true);
                   mStatusBar.disable(StatusBarManager.DISABLE_EXPAND);
                 }
                 if (!isChecked) 
                 {
                   RememberStatusMod(false);
                   mStatusBar.disable(StatusBarManager.DISABLE_NONE);
                 }
               }
             }
        });

        mModBox2 = (CheckBox) findViewById(R.id.modbox2);

        mModBox2.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (isChecked)
                 {
                   RememberDisplayMod(true);
                   KeyguardViewMediator.mRealPowerManager.enableUserActivity(true);
                 }
                 if (!isChecked) 
                 {
                   RememberDisplayMod(false);
                   KeyguardViewMediator.mRealPowerManager.enableUserActivity(false);
                 }
             }
        });

        mModBox3 = (CheckBox) findViewById(R.id.modbox3);

        mModBox3.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (isChecked)
                 {
                   RememberAnimationMod(true);
                 }
                 if (!isChecked) 
                 {
                   RememberAnimationMod(false);
                 }
             }
        });


        mModBox4 = (CheckBox) findViewById(R.id.modbox4);

        mModBox4.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (isChecked)
                 {
                   begininteractivitymod = true;
                 }
                 if (!isChecked) 
                 {
                   begininteractivitymod = false;
                 }
             }
        });

        mModBox5 = (CheckBox) findViewById(R.id.modbox5);

        mModBox5.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (isChecked)
                 {
                   RememberSimInfoMod(true);
                 }
                 if (!isChecked) 
                 {
                   RememberSimInfoMod(false);
                 }
             }
        });

        mModBox6 = (CheckBox) findViewById(R.id.modbox6);

        mModBox6.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
             {
                 if (isChecked)
                 {
                   RememberICEMod(true);
                 }
                 if (!isChecked) 
                 {
                   RememberICEMod(false);
                 }
             }
        });

        mSpaceAdjuster = findViewById(R.id.SpaceAdjuster);
        mContentSpaceAdjuster = findViewById(R.id.ContentSpaceAdjuster);

        mNotificationsGroup = (ViewGroup) findViewById(R.id.keyguardnotifications);
        msettingsIcon = (ImageButton) findViewById(R.id.settingsIcon); 

        msettingsIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!minteractivitymod) {
            if (begininteractivitymod) {
                minteractivitymod = true;
                }
            if (credits != 4) {
            if (credits != 0) {
                credits = 0;
                }
            if (credits == 0) {
                credits = 1;
                }
            if (WhatIsShowing != "Nothing") {
            if (WhatIsShowing == "Settings") {
                Log.d(TAG, "Hiding Settings");
                Showornottoshow(true);
                if (!mAnimationMod()) {
                setSettingsVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.GONE);
                }
                WhatIsShowing = "Nothing";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            else {
            if (WhatIsShowing == "Music") {
                Log.d(TAG, "Hiding Music and Revealing Settings");
                    CheckSettings();
                if (!mAnimationMod()) {
                setMusicVisibility(false, com.android.internal.R.anim.fade_out1);
                setSettingsVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mMusicGroup.setVisibility(View.GONE);
                mSettingsGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Settings";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "ICE") {
                Log.d(TAG, "Hiding ICE and Revealing Settings");
                    CheckSettings();
                if (!mAnimationMod()) {
                setICEVisibility(false, com.android.internal.R.anim.fade_out1);
                setSettingsVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.GONE);
                mSettingsGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Settings";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "Battery") {
                Log.d(TAG, "Hiding Battery and Revealing Settings");
                    CheckSettings();
                if (!mAnimationMod()) {
                setBatteryInfoVisibility(false, com.android.internal.R.anim.fade_out1);
                setSettingsVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mBatteryInfoGroup.setVisibility(View.GONE);
                mSettingsGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Settings";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
             }
           }
            else {
                Log.d(TAG, "Revealing Settings");
                   CheckSettings();
                    mTheuhdowhat.setVisibility(View.GONE); 
                    credits = 0;  
                Hideornottohide(true, false);
                if (!mAnimationMod()) {
                if (mSimInfoMod()) {             
                setSettingsVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                else {
                setSettingsVisibility(true, com.android.internal.R.anim.fade_in1);
                }
                }
                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Settings";
            }
            }
            else {
                credits = 6;
                WhatIsShowing = "Nothing";
                mBatteryInfoGroup.setVisibility(View.GONE);
                mMusicGroup.setVisibility(View.GONE);
                mSettingsGroup.setVisibility(View.GONE);
                mICE.setVisibility(View.GONE);
                setTheuhdowhatVisibility(true, com.android.internal.R.anim.fade_in1);
            }
            }
            }
        });

        mBatteryInfoGroup = (ViewGroup) findViewById(R.id.batteryInfo);
        mBatteryInfoIcon = (ImageView) findViewById(R.id.batteryInfoIcon);
        mBatteryInfoText = (TextView) findViewById(R.id.batteryInfoText);
        mbatteryIcon = (ImageButton) findViewById(R.id.batteryIcon);

        mbatteryIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!minteractivitymod) {
            if (begininteractivitymod) {
                minteractivitymod = true;
                }
            if (credits != 2) {
                credits = 0;
                }
            if (credits == 2) {
                credits = 3;
                }
            if (WhatIsShowing != "Nothing") {
            if (WhatIsShowing == "Battery") {
                Log.d(TAG, "Hiding Battery");
                Showornottoshow(true);
                if (!mAnimationMod()) { 
                setBatteryInfoVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mBatteryInfoGroup.setVisibility(View.GONE);
                }
                WhatIsShowing = "Nothing";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            else {
            if (WhatIsShowing == "Music") {
                Log.d(TAG, "Hiding Music and Revealing Battery");
                if (!mAnimationMod()) {
                setMusicVisibility(false, com.android.internal.R.anim.fade_out1);
                setBatteryInfoVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mMusicGroup.setVisibility(View.GONE);
                mBatteryInfoGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Battery";
                refreshBatteryDisplay();
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "ICE") {
                Log.d(TAG, "Hiding ICE and Revealing Battery");
                if (!mAnimationMod()) {
                setICEVisibility(false, com.android.internal.R.anim.fade_out1);
                setBatteryInfoVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.GONE);
                mBatteryInfoGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Battery";
                refreshBatteryDisplay();
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "Settings") {
                Log.d(TAG, "Hiding Settings and Revealing Battery");
                if (!mAnimationMod()) {
                setSettingsVisibility(false, com.android.internal.R.anim.fade_out1);
                setBatteryInfoVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.GONE);
                mBatteryInfoGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Battery";
                refreshBatteryDisplay();
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
             }
           }
            else {
                Log.d(TAG, "Revealing Battery");
                    mTheuhdowhat.setVisibility(View.GONE); 
                    credits = 0;  
                Hideornottohide(true, false);
                if (!mAnimationMod()) {
                if (mSimInfoMod()) {              
                setBatteryInfoVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                else {
                setBatteryInfoVisibility(true, com.android.internal.R.anim.fade_in1);
                }
                }
                if (mAnimationMod()) {
                mBatteryInfoGroup.setVisibility(View.VISIBLE);
                }
                WhatIsShowing = "Battery";
                refreshBatteryDisplay();
            }
            }
          }
        });

        mMusicGroup = (ViewGroup) findViewById(R.id.musicgroup);
        mrewindIcon = (ImageButton) findViewById(R.id.rewindIcon); 

        mrewindIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.previous");
             getContext().sendBroadcast(intent);
             }
          });

        mplayIcon = (ImageButton) findViewById(R.id.playIcon); 

        mplayIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.togglepause");
             getContext().sendBroadcast(intent);
             if (!am.isMusicActive()) {
                 mpauseIcon.setVisibility(View.VISIBLE);
                 mplayIcon.setVisibility(View.GONE);
                 }
             }
          });

        mpauseIcon = (ImageButton) findViewById(R.id.pauseIcon); 

        mpauseIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.togglepause");
             getContext().sendBroadcast(intent);
             if (am.isMusicActive()) {
                 mplayIcon.setVisibility(View.VISIBLE);
                 mpauseIcon.setVisibility(View.GONE);
                 }
             }
          });                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.GONE);
                mBatteryInfoGroup.setVisibility(View.VISIBLE);
                }

        mforwardIcon = (ImageButton) findViewById(R.id.forwardIcon); 

        mforwardIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
             Intent intent;
             intent = new Intent("com.android.music.musicservicecommand.next");
             getContext().sendBroadcast(intent);
             }
          });

        mmusicIcon = (ImageButton) findViewById(R.id.musicIcon);

        mmusicIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!minteractivitymod) {
            if (begininteractivitymod) {
                minteractivitymod = true;
                }
            if (credits != 1) {
                credits = 0;
                }
            if (credits == 1) {
                credits = 2;
                }
            if (WhatIsShowing != "Nothing") {
            if (WhatIsShowing == "Music") {
                Log.d(TAG, "Hiding Music");
                Showornottoshow(true);
                if (!mAnimationMod()) {
                setMusicVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mMusicGroup.setVisibility(View.GONE);
                }
                WhatIsShowing = "Nothing";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            else {
            if (WhatIsShowing == "Battery") {
                ShowBatteryInfo = false;
                Log.d(TAG, "Hiding battery info and Revealing Music");
                if (!mAnimationMod()) {
                setBatteryInfoVisibility(false, com.android.internal.R.anim.fade_out1);
                setMusicVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mBatteryInfoGroup.setVisibility(View.GONE);
                mMusicGroup.setVisibility(View.VISIBLE);
                }
                    if (am.isMusicActive()) {
                        mpauseIcon.setVisibility(View.VISIBLE);
                        mplayIcon.setVisibility(View.GONE);
                        }
                    else {
                        mpauseIcon.setVisibility(View.GONE);
                        mplayIcon.setVisibility(View.VISIBLE);
                        } 
                WhatIsShowing = "Music";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "ICE") {
                Log.d(TAG, "Hiding ICE info and Revealing Music");
                if (!mAnimationMod()) {
                setICEVisibility(false, com.android.internal.R.anim.fade_out1);
                setMusicVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.GONE);
                mMusicGroup.setVisibility(View.VISIBLE);
                }
                    if (am.isMusicActive()) {
                        mpauseIcon.setVisibility(View.VISIBLE);
                        mplayIcon.setVisibility(View.GONE);
                        }
                    else {
                        mpauseIcon.setVisibility(View.GONE);
                        mplayIcon.setVisibility(View.VISIBLE);
                        } 
                WhatIsShowing = "Music";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            if (WhatIsShowing == "Settings") {
                Log.d(TAG, "Hiding Settings info and Revealing Music");
                if (!mAnimationMod()) {
                setSettingsVisibility(false, com.android.internal.R.anim.fade_out1);
                setMusicVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.GONE);
                mMusicGroup.setVisibility(View.VISIBLE);
                }
                    if (am.isMusicActive()) {
                        mpauseIcon.setVisibility(View.VISIBLE);
                        mplayIcon.setVisibility(View.GONE);
                        }
                    else {
                        mpauseIcon.setVisibility(View.GONE);
                        mplayIcon.setVisibility(View.VISIBLE);
                        } 
                WhatIsShowing = "Music";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
             }
           }
             else {
                Log.d(TAG, "Revealing Music");
                    mTheuhdowhat.setVisibility(View.GONE); 
                    credits = 0;  
                Hideornottohide(true, false);
                if (!mAnimationMod()) {               
                if (mSimInfoMod()) {
                setMusicVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                else {
                setMusicVisibility(true, com.android.internal.R.anim.fade_in1);
                }            
                }
                if (mAnimationMod()) {
                mMusicGroup.setVisibility(View.VISIBLE);
                }
                    if (am.isMusicActive()) {
                        mpauseIcon.setVisibility(View.VISIBLE);
                        mplayIcon.setVisibility(View.GONE);
                        }
                    else {
                        mpauseIcon.setVisibility(View.GONE);
                        mplayIcon.setVisibility(View.VISIBLE);
                        } 
                WhatIsShowing = "Music";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
            }
            }
          }
        });
        mICE = (ViewGroup) findViewById(R.id.ICE);
        mICEfound = (ViewGroup) findViewById(R.id.ICEfound);
        mICEnotfound = (ViewGroup) findViewById(R.id.ICEnotfound);
        mICEownermain = (ViewGroup) findViewById(R.id.ICEownermain);
        mICEowner = (TextView) findViewById(R.id.ICEowner);
        mICEcontactmain = (ViewGroup) findViewById(R.id.ICEcontactmain);
        mICEcontact = (TextView) findViewById(R.id.ICEcontact);
        mICEnotes = (TextView) findViewById(R.id.ICEnotes);
        mICEIcon = (ImageButton) findViewById(R.id.ICEIcon);

        mICEIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!minteractivitymod) {
            if (begininteractivitymod) {
                minteractivitymod = true;
                }
            if (credits != 3) {
                credits = 0;
                }
            if (credits == 3) {
                credits = 4;
                }
            if (WhatIsShowing != "Nothing") {
            if (WhatIsShowing == "ICE") {
                Log.d(TAG, "Hiding ICE");
                Showornottoshow(true);
                if (!mAnimationMod()) { 
                setICEVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.GONE);
                }
                WhatIsShowing = "Nothing";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
               }
            else {
            if (WhatIsShowing == "Music") {
                Log.d(TAG, "Hiding Music and Revealing ICE");
                if (!mAnimationMod()) {
                setMusicVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mMusicGroup.setVisibility(View.GONE);
                }
               }
            if (WhatIsShowing == "Battery") {
                Log.d(TAG, "Hiding ICE and Revealing ICE");
                if (!mAnimationMod()) {
                setBatteryInfoVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mBatteryInfoGroup.setVisibility(View.GONE);
                }
               }
            if (WhatIsShowing == "Settings") {
                Log.d(TAG, "Hiding Settings and Revealing ICE");
                if (!mAnimationMod()) {
                setSettingsVisibility(false, com.android.internal.R.anim.fade_out1);
                }
                if (mAnimationMod()) {
                mSettingsGroup.setVisibility(View.GONE);
                }
               }
                if (!mAnimationMod()) {
                setICEVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.VISIBLE);
                }
                CheckICEInfo();
                WhatIsShowing = "ICE";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
             }
           }
            else {
                Log.d(TAG, "Revealing ICE");
                    mTheuhdowhat.setVisibility(View.GONE); 
                    credits = 0;  
                Hideornottohide(true, false);
                if (!mAnimationMod()) {
                if (mSimInfoMod()) {              
                setICEVisibility(true, com.android.internal.R.anim.fade_in1_delay);
                }
                else {
                setICEVisibility(true, com.android.internal.R.anim.fade_in1);
                }          
                }
                if (mAnimationMod()) {
                mICE.setVisibility(View.VISIBLE);
                }
                CheckICEInfo();
                WhatIsShowing = "ICE";
                Log.d(TAG, "We are showing:" + WhatIsShowing);
            }
            }
          }
        });


        mNextAlarmGroup = (ViewGroup) findViewById(R.id.nextAlarmInfo);
        mAlarmText = (TextView) findViewById(R.id.nextAlarmText);

        mScreenLockedMessageGroup = (ViewGroup) findViewById(R.id.screenLockedInfo);

        mLockInstructions = (TextView) findViewById(R.id.lockInstructions);

        mEmergencyCallButton = (Button) findViewById(R.id.emergencyCallButton);

        mEmergencyCallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.takeEmergencyCallAction();
            }
        });

        mTheuhdowhat = (ViewGroup) findViewById(R.id.Theuhdowhat);

        mOnlyVisibleWhenSimOk = new View[] {
            mNextAlarmGroup,
            mNotificationsGroup,
            mLockInstructions,
            msettingsIcon,
            mbatteryIcon,
            mmusicIcon,
            mICEIcon
        };

        mOnlyVisibleWhenSimNotOk = new View[] {
            mHeaderSimBad1,
            mHeaderSimBad2,
            mEmergencyCallButton
        };

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        refreshBatteryDisplay();
        refreshAlarmDisplay();
        refreshTimeAndDateDisplay();
        refreshUnlockIntructions();
        refreshViewsWRTSimOk();
        refreshSimOkHeaders(mUpdateMonitor.getTelephonyPlmn(), mUpdateMonitor.getTelephonySpn());

        updateMonitor.registerInfoCallback(this);
        updateMonitor.registerSimStateCallback(this);
        updateMonitor.registerConfigurationChangeCallback(this);


        if (!KeyguardViewMediator.Securitycheck) {
        if (mStatusmod()) {
            mStatusBar.disable(StatusBarManager.DISABLE_EXPAND);
            }
        else {
            mStatusBar.disable(StatusBarManager.DISABLE_NONE);
            }
            }
        if (mDisplaymod()) {
            KeyguardViewMediator.mRealPowerManager.enableUserActivity(true);
            }
        else {
            KeyguardViewMediator.mRealPowerManager.enableUserActivity(false);
            }
        if (mICEMod()) {
            WhatIsShowing = "ICE";
    }

        if (WhatIsShowing != "Nothing") {
                    Hideornottohide(false, true);
                if (WhatIsShowing == "Battery") {
                    mMusicGroup.setVisibility(View.GONE);
                    mSettingsGroup.setVisibility(View.GONE);
                    mICE.setVisibility(View.GONE);
                    mTheuhdowhat.setVisibility(View.GONE);
           }
                if (WhatIsShowing == "Music") {
                    mBatteryInfoGroup.setVisibility(View.GONE);
                    mSettingsGroup.setVisibility(View.GONE);
                    mICE.setVisibility(View.GONE);
                    mTheuhdowhat.setVisibility(View.GONE);
                    if (am.isMusicActive()) {
                        mpauseIcon.setVisibility(View.VISIBLE);
                        mplayIcon.setVisibility(View.GONE);
                        }
                    else {
                        mpauseIcon.setVisibility(View.GONE);
                        mSettingsGroup.setVisibility(View.GONE);
                        mplayIcon.setVisibility(View.VISIBLE);
                        } 
           }
                if (WhatIsShowing == "Settings") {
                    CheckSettings();
                    mBatteryInfoGroup.setVisibility(View.GONE);
                    mMusicGroup.setVisibility(View.GONE);
                    mICE.setVisibility(View.GONE);
                    mTheuhdowhat.setVisibility(View.GONE);
           }
                if (WhatIsShowing == "ICE") {
                    CheckICEInfo();
                    mMusicGroup.setVisibility(View.GONE);
                    mSettingsGroup.setVisibility(View.GONE);
                    mBatteryInfoGroup.setVisibility(View.GONE);
                    mTheuhdowhat.setVisibility(View.GONE);
           }
        }
        if (WhatIsShowing == "Nothing") {
        Showornottoshow(false);
        mBatteryInfoGroup.setVisibility(View.GONE);
        mMusicGroup.setVisibility(View.GONE);
        mSettingsGroup.setVisibility(View.GONE);
        mICE.setVisibility(View.GONE);
        mTheuhdowhat.setVisibility(View.GONE);
        Log.d(TAG, "Showing Content Spacer Adjustment");
        }

    }

    private void Showornottoshow(boolean yesorno) {
        if (!mAnimationMod()) {
        if (yesorno) {
        if (mSimInfoMod()) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        setSimVisibility(true, com.android.internal.R.anim.fade_in1_delay);
        }
        }
        else {
        if (mSimInfoMod()) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        mSIM.setVisibility(View.VISIBLE);
        }
        }
        if (!mSimInfoMod()) {
        mSIM.setVisibility(View.GONE);
        mContentSpaceAdjuster.setVisibility(View.VISIBLE);
        }
        }
        else {
        if (mSimInfoMod()) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        mSIM.setVisibility(View.VISIBLE);
        }
        if (!mSimInfoMod()) {
        mSIM.setVisibility(View.GONE);
        mContentSpaceAdjuster.setVisibility(View.VISIBLE);
        }
        }
        }

    private void Hideornottohide(boolean yesorno, boolean specialorno) {
        if (!mAnimationMod()) {
        if (yesorno) {
        if (mSimInfoMod()) {
        setSimVisibility(false, com.android.internal.R.anim.fade_out1);
        }        
        }
        else {
        if (mSimInfoMod()) {
        mSIM.setVisibility(View.GONE);
        }
        }
        if (!mSimInfoMod()) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        }
        }
        else {
        if (mSimInfoMod()) {
        mSIM.setVisibility(View.GONE);
        }
        if (!mSimInfoMod()) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        }
        }
        if (specialorno) {
        mContentSpaceAdjuster.setVisibility(View.GONE);
        mSIM.setVisibility(View.GONE);
        }
        }

    private void CheckICEInfo() {
        int id = 0;
        String verify = null;
        String s = null;
        String t = null;

	Cursor c = getContext().getContentResolver().query(People.CONTENT_URI,
			new String[] { People._ID, People.NUMBER, People.NOTES, People.NAME },
			People.NAME + "=?", new String[] { "Ice" },
			null);
        if (c.getCount() == 1) {
                        c.moveToFirst();
                        verify = c.getString(3);
                        if (verify != null) {
                        mICEnotfound.setVisibility(View.GONE);
                        id = c.getInt(0);
                        s = c.getString(1);
                        if (s != null) {
                        mICEcontact.setText(s);
                        }
                        if (s == null) {
                        mICEcontactmain.setVisibility(View.GONE);
                        }
                        t = c.getString(2);
                        mICEnotes.setText(t);
                 }
             }
             if (verify == null) {
             mICEfound.setVisibility(View.GONE);                
             }
             c.close();
          if (verify != null) {


 String where = Contacts.ContactMethods.PERSON_ID + " == " + id
                   + " AND "
                   + Contacts.ContactMethods.KIND + " == " 
                   + Contacts.KIND_POSTAL;

          Cursor cc = getContext().getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, 
                                                       null, where, null, null);

          int postalAddress = cc.getColumnIndex(Contacts.ContactMethodsColumns.DATA);
          String address = null;
          if (cc.getCount() == 0) {
             mICEownermain.setVisibility(View.GONE);
             }
          if (cc.getCount() == 1) {
             cc.moveToFirst();
             address = cc.getString(postalAddress);
             if (address != null) {
             mICEowner.setText(address);
             }
             }
          cc.close();
            }
           }

    private void CheckSettings() {
         if (!KeyguardViewMediator.Securitycheck) {
            if (mStatusmod()) {
               mModBox.setChecked(true);
               }
               }
            if (KeyguardViewMediator.Securitycheck) {
               mModBox.setChecked(true);
               }
            if (mDisplaymod()) {
               mModBox2.setChecked(true);
               }
            if (mAnimationMod()) {
               mModBox3.setChecked(true);
            } 
            if (mSimInfoMod()) {
               mModBox5.setChecked(true);
            } 
            if (mICEMod()) {
               mModBox6.setChecked(true);
            } 
          }

    private void landscapetransition() {
         if (fromportrait) {
            if (WhatIsShowing == "Battery") {
            mBatteryInfoGroup.setVisibility(View.GONE);
             }
            if (WhatIsShowing == "Music") {
            mMusicGroup.setVisibility(View.GONE);
             }
            if (WhatIsShowing == "Settings") {
            mSettingsGroup.setVisibility(View.GONE);
             }
            if (WhatIsShowing == "ICE") {
            mICE.setVisibility(View.GONE);
             }
            if (WhatIsShowing == "Nothing") {
             Hideornottohide(false, false);
             }
            mNotificationsGroup.setVisibility(View.GONE);
            mLockInstructions.setVisibility(View.GONE);
             }
         if (!fromportrait) {
            if (WhatIsShowing == "Battery") {
            mBatteryInfoGroup.setVisibility(View.VISIBLE);
             }
            if (WhatIsShowing == "Music") {
            mMusicGroup.setVisibility(View.VISIBLE);
             }
            if (WhatIsShowing == "ICE") {
            mICE.setVisibility(View.VISIBLE);
             }
            if (WhatIsShowing == "Settings") {
            CheckSettings();
            mSettingsGroup.setVisibility(View.VISIBLE);
             }
            if (WhatIsShowing == "Nothing") {
             Showornottoshow(false);
             }
            if (WhatIsShowing != "Nothing") {
            Hideornottohide(false, true);
             }
            mNotificationsGroup.setVisibility(View.VISIBLE);
            mLockInstructions.setVisibility(View.VISIBLE);
             }
    }


    private void setBatteryInfoVisibility(boolean visible, int anim) {
        mBatteryInfoGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBatteryInfoGroup.startAnimation(loadAnim(anim, null));
    }

    private void setMusicVisibility(boolean visible, int anim) {
        mMusicGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        mMusicGroup.startAnimation(loadAnim(anim, null));
    }

    private void setSettingsVisibility(boolean visible, int anim) {
        mSettingsGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        mSettingsGroup.startAnimation(loadAnim(anim, null));
    }

    private void setICEVisibility(boolean visible, int anim) {
        mICE.setVisibility(visible ? View.VISIBLE : View.GONE);
        mICE.startAnimation(loadAnim(anim, null));
    }

    private void setTheuhdowhatVisibility(boolean visible, int anim) {
        mTheuhdowhat.setVisibility(visible ? View.VISIBLE : View.GONE);
        mTheuhdowhat.startAnimation(loadAnim(anim, null));
    }

    private void setSimVisibility(boolean visible, int anim) {
        mSIM.setVisibility(visible ? View.VISIBLE : View.GONE);
        mSIM.startAnimation(loadAnim(anim, null));
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mCallback.goToUnlockScreen();
        }
        return false;
    }

    private void refreshViewsWRTSimOk() {
        if (mSimOk) {
            for (int i = 0; i < mOnlyVisibleWhenSimOk.length; i++) {
                final View view = mOnlyVisibleWhenSimOk[i];
                if (view == null) throw new RuntimeException("index " + i + " null");
                view.setVisibility(View.VISIBLE);
            }
            for (int i = 0; i < mOnlyVisibleWhenSimNotOk.length; i++) {
                final View view = mOnlyVisibleWhenSimNotOk[i];
                view.setVisibility(View.GONE);
            }
            refreshSimOkHeaders(mUpdateMonitor.getTelephonyPlmn(), 
                                    mUpdateMonitor.getTelephonySpn());
            refreshAlarmDisplay();
            refreshBatteryDisplay();
            landscapetransition();
        } else {
            for (int i = 0; i < mOnlyVisibleWhenSimOk.length; i++) {
                final View view = mOnlyVisibleWhenSimOk[i];
                view.setVisibility(View.GONE);
            }
            for (int i = 0; i < mOnlyVisibleWhenSimNotOk.length; i++) {
                final View view = mOnlyVisibleWhenSimNotOk[i];
                view.setVisibility(View.VISIBLE);
            }
            refreshSimBadInfo();
        }
    }

    private void refreshSimBadInfo() {
        final IccCard.State simState = mUpdateMonitor.getSimState();
        if (simState == IccCard.State.PUK_REQUIRED) {
            mHeaderSimBad1.setText(R.string.lockscreen_sim_puk_locked_message);
            mHeaderSimBad2.setText(R.string.lockscreen_sim_puk_locked_instructions);
        } else if (simState == IccCard.State.ABSENT) {
            mHeaderSimBad1.setText(R.string.lockscreen_missing_sim_message);
            mHeaderSimBad2.setVisibility(View.GONE);
            //mHeaderSimBad2.setText(R.string.lockscreen_missing_sim_instructions);
        } else {
            mHeaderSimBad1.setVisibility(View.GONE);
            mHeaderSimBad2.setVisibility(View.GONE);
        }
    }

    private void refreshUnlockIntructions() {
        if (mLockPatternUtils.isLockPatternEnabled()
                || mUpdateMonitor.getSimState() == IccCard.State.PIN_REQUIRED
                || mUpdateMonitor.getSimState() == IccCard.State.ABSENT) {
            mLockInstructions.setText(R.string.lockscreen_instructions_when_pattern_enabled);
        } else {
            mLockInstructions.setText(R.string.lockscreen_instructions_when_pattern_disabled);
        }
    }

    private void refreshAlarmDisplay() {
        final String nextAlarmText = mLockPatternUtils.getNextAlarm();

        // bug 1685880: if we are in landscape and showing plmn, the information can end up not
        // fitting on screen.  in this case, the alarm will get cut.
        final CharSequence plmn = mUpdateMonitor.getTelephonyPlmn();
        final boolean showingPlmn = plmn != null && !TextUtils.isEmpty(plmn);
        final boolean wontFit = !mUpdateMonitor.isInPortrait() && showingPlmn;
        if (nextAlarmText != null && mSimOk) {
            setAlarmInfoVisible(true);
            mSpaceAdjuster.setVisibility(View.GONE);
            mAlarmText.setText(nextAlarmText);
        } else {
            setAlarmInfoVisible(false);
            mSpaceAdjuster.setVisibility(View.VISIBLE);
        }
    }

    private void setAlarmInfoVisible(boolean visible) {
        final int visibilityFlag = visible ? View.VISIBLE : View.GONE;
        mNextAlarmGroup.setVisibility(visibilityFlag);
    }


    public void onRefreshBatteryInfo(boolean showBatteryInfo, boolean pluggedIn,
            int batteryLevel) {
        mShowingBatteryInfo = showBatteryInfo;
        mPluggedIn = pluggedIn;
        refreshBatteryDisplay();
    }


    private void lockbattimage() {
     /**
     * Below is the custom images I added for the lockscreen batt
     * image. If you need to change them just make sure that your
     * images start with zz so it doesnt throw the rest of the images
     * off.
     *
     *(Stericson)
     */

     if (mBatteryLevel <= 0)
     {         mBatteryInfoIcon.setImageResource(R.drawable.zzbatt0);
     } else if (mBatteryLevel <= 5)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt5);
     } else if (mBatteryLevel <= 10)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt10);
     } else if (mBatteryLevel <= 15)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt15);
     } else if (mBatteryLevel <= 20)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt20);
     } else if (mBatteryLevel <= 25)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt25);
     } else if (mBatteryLevel <= 30)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt30);
     } else if (mBatteryLevel <= 35)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt35);
     } else if (mBatteryLevel <= 40)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt40);
     } else if (mBatteryLevel <= 45)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt45);
     } else if (mBatteryLevel <= 50)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt50);
     } else if (mBatteryLevel <= 55)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt55);
     } else if (mBatteryLevel <= 60)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt60);
     } else if (mBatteryLevel <= 65)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt65);
     } else if (mBatteryLevel <= 70)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt70);
     } else if (mBatteryLevel <= 75)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt75);
     } else if (mBatteryLevel <= 80)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt80);
     } else if (mBatteryLevel <= 85)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt85);
     } else if (mBatteryLevel <= 90)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt90);
     } else if (mBatteryLevel <= 95)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt95);
     } else if (mBatteryLevel <= 100)
     {        mBatteryInfoIcon.setImageResource(R.drawable.zzbatt100);
     }
     }


    private void refreshBatteryDisplay() {

        lockbattimage();

        if (!mSimOk) {
            return;
        }
                mBatteryInfoText.setText(
                        getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel));
            }

    public void onTimeChanged() {
        refreshTimeAndDateDisplay();
    }

    private void refreshTimeAndDateDisplay() {
        Date now = new Date();
        mTime.setText(DateFormat.getTimeFormat(getContext()).format(now));
// Mod by nobunobu
    	mDate.setText(getLockScreenDateFormat().format(now));
//        mDate.setText(DateFormat.format("EEEE, MMMM dd, yyyy", now));
    }

    /**
     * @return A localized format like "Fri, Sep 18, 2009"
     */
    private java.text.DateFormat getLockScreenDateFormat() {
        SimpleDateFormat adjusted = null;
        try {
            // this call gives us the localized order
            final SimpleDateFormat dateFormat = (SimpleDateFormat)
                    java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL);
            adjusted = new SimpleDateFormat(dateFormat.toPattern()
                    .replace("MMMM", "MMM")    // we want "Sep", not "September"
                    .replace("EEEE", "EEE")     // we want "Fri", no "Friday"
        			.replaceAll("EEE$", "(EEE)"));
        } catch (ClassCastException e) {
            // in case the library implementation changes and this throws a class cast exception
            // or anything else that is funky
            Log.e("LockScreen", "couldn't finnagle our custom date format :(", e);
            return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM);
        }
        return adjusted;
    }

    public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn) {
        refreshSimOkHeaders(plmn, spn);
        refreshAlarmDisplay();  // in case alarm won't fit anymore
    }

    private void refreshSimOkHeaders(CharSequence plmn, CharSequence spn) {
        final IccCard.State simState = mUpdateMonitor.getSimState();
        if (simState == IccCard.State.READY) {
            if (plmn != null && !TextUtils.isEmpty(plmn)) {
                mHeaderSimOk1.setVisibility(View.VISIBLE);
                mHeaderSimOk1.setText(plmn);
            } else {
                mHeaderSimOk1.setVisibility(View.GONE);
            }

            if (spn != null && !TextUtils.isEmpty(spn)) {
                mHeaderSimOk2.setVisibility(View.VISIBLE);
                mHeaderSimOk2.setText(spn);
            } else {
                mHeaderSimOk2.setVisibility(View.GONE);
            }
        } else if (simState == IccCard.State.PIN_REQUIRED) {
            mHeaderSimOk1.setVisibility(View.VISIBLE);
            mHeaderSimOk1.setText(R.string.lockscreen_sim_locked_message);
            mHeaderSimOk2.setVisibility(View.GONE);
        } else if (simState == IccCard.State.ABSENT) {
            mHeaderSimOk1.setVisibility(View.VISIBLE);
            mHeaderSimOk1.setText(R.string.lockscreen_missing_sim_message_short);
            mHeaderSimOk2.setVisibility(View.GONE);
        } else if (simState == IccCard.State.NETWORK_LOCKED) {
            mHeaderSimOk1.setVisibility(View.VISIBLE);
            mHeaderSimOk1.setText(R.string.lockscreen_network_locked_message);
            mHeaderSimOk2.setVisibility(View.GONE);
        }
    }

    public void onSimStateChanged(IccCard.State simState) {
        mSimOk = isSimOk(simState);
        refreshViewsWRTSimOk();
        refreshUnlockIntructions();
    }

    /**
     * @return Whether the sim state is ok, meaning we don't need to show
     *   a special screen with the emergency call button and keep them from
     *   doing anything else.
     */
    private boolean isSimOk(IccCard.State simState) {
        boolean missingAndNotProvisioned = (!mUpdateMonitor.isDeviceProvisioned()
                && simState == IccCard.State.ABSENT);
        return !(missingAndNotProvisioned || simState == IccCard.State.PUK_REQUIRED);
    }

    public void onOrientationChange(boolean inPortrait) {
    }

    public void onKeyboardChange(boolean isKeyboardOpen) {
        if (!isKeyboardOpen) {
                landscapetransition();
                }
        if (isKeyboardOpen) {
                landscapetransition();
            mCallback.goToUnlockScreen();
        }
    }

    /** {@inheritDoc} */
    public boolean needsInput() {
        return false;
    }
    
    /** {@inheritDoc} */
    public void onPause() {

    }

    /** {@inheritDoc} */
    public void onResume() {

    }

    /** {@inheritDoc} */
    public void cleanUp() {
        mUpdateMonitor.removeCallback(this);
    }
}

