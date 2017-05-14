package com.crivenco.vlad.chatxmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crivenco.vlad.chatxmpp.Model.Contact;
import com.crivenco.vlad.chatxmpp.Model.ContactModel;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class ChatConnection implements ConnectionListener {

    private static final String TAG = "ChatConnection";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;
    private AbstractXMPPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private ChatManager chatManager;
    private Roster roster;
    private MamManager manager;


    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public ChatConnection( Context context)
    {
        Log.e(TAG,"RoosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid",null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_pass",null);

        if( jid != null)
        {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        }else
        {
            mUsername ="";
            mServiceName="";
        }
        //System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
    }


    public void connect() throws InterruptedException, IOException, SmackException, XMPPException {
        Log.e(TAG, "Connecting to server " + mServiceName);
        SmackConfiguration.DEBUG = true;

        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setUsernameAndPassword(mUsername, mPassword);
        configBuilder.setResource("Android");
        configBuilder.setXmppDomain(mServiceName);
        configBuilder.setPort(5222);
        configBuilder.setDebuggerEnabled(true);
        configBuilder.setKeystoreType(null);
        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(configBuilder.build());
        mConnection.addConnectionListener(this);
        mConnection.connect();
        mConnection.login();


        chatManager = ChatManager.getInstanceFor(mConnection);
        chatManager.addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                Log.e(TAG,"message.getBody() :"+message.getBody());
                Log.e(TAG,"message.getFrom() :"+from.toString());
                Intent i = new Intent(ChatConnectionService.NEW_MESSAGE);
                i.putExtra(ChatConnectionService.BUNDLE_FROM_JID, from.toString());
                i.putExtra(ChatConnectionService.BUNDLE_MESSAGE_BODY, message.getBody());

                mApplicationContext.sendBroadcast(i);
            }
        });


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }

    private Roster getRoster(){
        Roster r = Roster.getInstanceFor(mConnection);

        r.setSubscriptionMode(Roster.SubscriptionMode.manual);
        r.setRosterLoadedAtLogin(true);
        r.addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceAvailable(FullJid address, Presence availablePresence) {
                super.presenceAvailable(address, availablePresence);
                Log.e(TAG, "User (available) " + address.toString() +  " is " + availablePresence.toString());
            }

            @Override
            public void presenceUnavailable(FullJid address, Presence presence) {
                super.presenceUnavailable(address, presence);
                Log.e(TAG, "User (unavailable) " + address.toString() +  " is " + presence.toString());
            }

            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                super.presenceSubscribed(address, subscribedPresence);
                Log.e(TAG, "User (subscribed) " + address.toString() +  " is " + subscribedPresence.toString());
            }

            @Override
            public void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence) {
                super.presenceUnsubscribed(address, unsubscribedPresence);
                Log.e(TAG, "User (unsubscribed) " + address.toString() +  " is " + unsubscribedPresence.toString());
            }
        });
        r.addSubscribeListener(new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                Log.e(TAG, "We got subscribeRequest from"  + from.toString());
                return SubscribeAnswer.Approve;
            }
        });

        r.addRosterLoadedListener(new RosterLoadedListener() {
            @Override
            public void onRosterLoaded(Roster roster) {
                Log.e(TAG, "Just loaded roaster");
                Log.e(TAG,"Show entries");

                Set<RosterEntry> entries = roster.getEntries();
                String ids[] = new String[entries.size()];
                int i  = 0;
                for (RosterEntry entry : entries){
                    Log.e(TAG,"entry = " +  entry.toString());
                    ids[i] = entry.getJid().toString();
                    i++;
                }
                Intent intent = new Intent(ContactListActivity.LIST);
                intent.putExtra("list", ids);
                mApplicationContext.sendBroadcast(intent);
            }

            @Override
            public void onRosterLoadingFailed(Exception exception) {
                Log.e(TAG, "Failed to load roster");
            }
        });
        return r;
    }




    private void setupUiThreadBroadCastMessageReceiver()
    {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if( action.equals(ChatConnectionService.SEND_MESSAGE))
                {
                    //Send the message.
                    sendMessage(intent.getStringExtra(ChatConnectionService.BUNDLE_MESSAGE_BODY), intent.getStringExtra(ChatConnectionService.BUNDLE_TO));
                } else if (action.equals(ChatConnectionService.NEW_FRIEND)){
                    Log.e(TAG, "action New friend ");
                    String id = intent.getStringExtra(ChatConnectionService.BUNDLE_TO);
                    try {
                        Log.e(TAG, "set subscribe to " + id);
                        Presence presence = new Presence(Presence.Type.subscribe);
                        presence.setMode(Presence.Mode.available);
                        presence.setPriority(24);
                        presence.setTo(JidCreate.bareFrom(id));
                        mConnection.sendStanza(presence);
                    } catch (Exception e) {
                        Log.e(TAG, "FAiled to add a friend");
                        e.printStackTrace();
                     }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ChatConnectionService.SEND_MESSAGE);
        filter.addAction(ChatConnectionService.LOG_OUT);
        filter.addAction(ChatConnectionService.NEW_FRIEND);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

    }

    private void sendMessage(String message, String id){
        try {
            Chat chat = chatManager.chatWith(JidCreate.entityBareFrom(id));
            chat.send(message);
            Message m;
            MamManager.MamQueryResult mamQueryResult = manager.queryArchive(500);
            Log.e(TAG, "MAM size =" + mamQueryResult.forwardedMessages.size());
            for (Forwarded g :mamQueryResult.forwardedMessages){
                m = (Message) g.getForwardedStanza();
                Log.e(TAG, "Got message " + m.getBody());
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to create EntityBareId");
        }
    }


    public void disconnect()
    {
        Log.e(TAG,"Disconnecting from serser "+ mServiceName);
        if (mConnection != null){
            mConnection.disconnect();
        }
        mConnection = null;
        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        ChatConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.e(TAG,"Connected Successfully");

    }


    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        ChatConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.e(TAG,"Authenticated Successfully");
        roster = getRoster();
        if (roster != null && !roster.isLoaded()) {
            try {
                roster.reloadAndWait();
            } catch (Exception e) {
                Log.e(TAG, "FAil to reload roster");
            }
        }
        Log.e(TAG, "Just loaded roaster");
        Log.e(TAG,"Show entries");

        Set<RosterEntry> entries = roster.getEntries();
//        String ids[] = new String[entries.size()];
        ContactModel model = ContactModel.get(mApplicationContext);
//        int i  = 0;
        List<Jid> list = new ArrayList<>();
        for (RosterEntry entry : entries){
            Log.e(TAG,"entry = " +  entry.toString());
            model.addContact(new Contact(entry.getJid().toString()));
            list.add(entry.getJid());
        }
//        Intent intent = new Intent(ContactListActivity.LIST);
//        intent.putExtra("list", ids);
//        mApplicationContext.sendBroadcast(intent);
        showContactListActivityWhenAuthenticated();

        manager = MamManager.getInstanceFor(mConnection);

        try {
            manager.updateArchivingPreferences(list, null, MamPrefsIQ.DefaultBehavior.always);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update ArchivePrefs");
        }

    }

    @Override
    public void connectionClosed() {
        ChatConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.e(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        ChatConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.e(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        ChatConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.e(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        ChatConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.e(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        ChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.e(TAG,"ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(ChatConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.e(TAG,"Sent the broadcast that we are authenticated");
    }
}