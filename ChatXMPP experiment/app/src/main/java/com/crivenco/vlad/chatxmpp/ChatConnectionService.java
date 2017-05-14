package com.crivenco.vlad.chatxmpp;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

public class ChatConnectionService extends Service {
    private static final String TAG = "ChatConnectionService";
    public static final String UI_AUTHENTICATED = "com.xmpp.chat.uiauthenticated";
    public static final String SEND_MESSAGE = "com.xmpp.chat.sendmessage";
    public static final String NEW_MESSAGE = "com.xmpp.chat.newmessage";
    public static final String LOG_OUT = "com.xmpp.chat.logout";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_TO = "b_to";
    public static final String BUNDLE_FROM_JID = "b_from";
    public static final String NEW_FRIEND = "com.xmpp.chat.new_friend";
    public static final int MESSAGE_ARCHIVE = 1;

    private boolean mActive;
    private Thread mThread;
    private Handler mHandler;

    public static ChatConnection.ConnectionState sConnectionState;
    public static ChatConnection.LoggedInState sLoggedInState;


    private ChatConnection mConnection;
    private BroadcastReceiver mreceiver;

    //return null because we don't want this service to get binded
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //can do some init here, it's first method called after startService
    @Override
    public void onCreate() {
        super.onCreate();

        mreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ChatConnectionService.LOG_OUT)){
                    stopSelf();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ChatConnectionService.LOG_OUT);
        registerReceiver(mreceiver, filter);
        Log.e(TAG,"OnCreate()");
    }

    //start a new thread
    public void start(){
        if (!mActive){
            mActive = true;
            if (mThread == null || !mThread.isAlive()){
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // initial thread doesn't have a looper(message stocker)
                        Looper.prepare();

                        mHandler = new Handler(){
                            @Override
                            public void handleMessage(Message msg) {
                                if (msg.what == MESSAGE_ARCHIVE){
                                    Log.e(TAG, "WE received a message")
                                }
                            }
                        };
                        initConnection();
                        //Code to run in Background

                        Looper.loop();
                    }
                });
            }
            mThread.start();
        }
    }

    public void stop(){
        Log.e(TAG,"stop()");
        mActive = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mConnection != null)
                    mConnection.disconnect();
            }
        });
        unregisterReceiver(mreceiver);
    }

    //here we can init a new thread
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"onStartCommand()");
        start();
        return Service.START_STICKY;
        //RETURNING START_STICKY CAUSES OUR CODE TO STICK AROUND WHEN THE APP ACTIVITY HAS DIED.
    }

    //it's called when service should be destroyed, perfect place to stop our connectivity thread
    @Override
    public void onDestroy() {
        Log.e(TAG,"onDestroy()");
        super.onDestroy();
        stop();
    }


    private void initConnection(){
        Log.e(TAG,"initConnection()");
        if (mConnection == null)
            mConnection = new ChatConnection(this);

        try {
            mConnection.connect();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }

    }

    public static ChatConnection.ConnectionState getState(){
        if (sConnectionState == null)
            return ChatConnection.ConnectionState.DISCONNECTED;

        return sConnectionState;
    }

    public static ChatConnection.LoggedInState getLoggedInState(){
        if (sLoggedInState == null)
            return ChatConnection.LoggedInState.LOGGED_OUT;
        return sLoggedInState;
    }

    public ChatConnection getConnection() {
        return mConnection;
    }



}
