package com.crivenco.vlad.chatxmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import co.devcenter.androiduilibrary.ChatView;
import co.devcenter.androiduilibrary.ChatViewEventListener;
import co.devcenter.androiduilibrary.SendButton;
import co.devcenter.androiduilibrary.models.ChatMessage;

public class ChatActivity extends AppCompatActivity {
    public static final String TAG = "ChatActivity";


    private ChatView chatView;
    private String contactJid;
    private SendButton mSendButton;
    private BroadcastReceiver receiver;

    @Override
    protected void onResume() {
        super.onResume();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case ChatConnectionService.NEW_MESSAGE:

                        String from = intent.getStringExtra(ChatConnectionService.BUNDLE_FROM_JID);
                        String message = intent.getStringExtra(ChatConnectionService.BUNDLE_MESSAGE_BODY);

                        if (from.equals(contactJid)){
                            Log.e(TAG, "Received a message from jid :"+from);
                            chatView.receiveMessage(message);
                        } else {
                            Log.e(TAG, "Got a message from jid :"+from);
                        }
                        return;

                    default:
                        Log.e(TAG, "NEW_MESSAGE is not the case");
                        return;
                }
            }
        };
        IntentFilter filter = new IntentFilter(ChatConnectionService.NEW_MESSAGE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatView = (ChatView)findViewById(R.id.chat_view);
        chatView.setEventListener(new ChatViewEventListener() {
            @Override
            public void userIsTyping() {

            }

            @Override
            public void userHasStoppedTyping() {

            }
        });

        mSendButton = chatView.getSendButton();
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ChatConnectionService.getState().equals(ChatConnection.ConnectionState.CONNECTED)){
                    Log.e(TAG, "The client is connected to server and we a sending message");

                    Intent i = new Intent(ChatConnectionService.SEND_MESSAGE);
                    i.putExtra(ChatConnectionService.BUNDLE_MESSAGE_BODY, chatView.getTypedString());
                    i.putExtra(ChatConnectionService.BUNDLE_TO, contactJid);
                    sendBroadcast(i);

                    chatView.sendMessage();
                } else {
                    Toast.makeText(getApplicationContext(), "Client not connected to server ,Message not sent!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        Intent intent = getIntent();
        contactJid = intent.getStringExtra("EXTRA_JID");
        setTitle(contactJid);
        populateFromArchive();

    }

    private class FetchArchive extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {

            return null;
        }
    }

    private void populateFromArchive() {

    }
}
