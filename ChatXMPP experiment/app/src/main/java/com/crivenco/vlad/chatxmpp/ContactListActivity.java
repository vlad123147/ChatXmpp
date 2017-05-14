package com.crivenco.vlad.chatxmpp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crivenco.vlad.chatxmpp.Model.Contact;
import com.crivenco.vlad.chatxmpp.Model.ContactModel;

import java.util.List;

public class ContactListActivity extends AppCompatActivity implements AddUserFragment.EditNameDialogListener {
    public static final String TAG = "ChatContactListActivity";
    public static final String EXTRA_NEW_CONTACT = "com.crivenco.vlad.chatxmpp.extra_new_contact";
    public static final String LIST = "com.crivenco.vlad.chatxmpp.list";


    private static final String ADDDIALOG = "AddUserFragment";
    private RecyclerView mRecyclerView;
    private ContactAdapter adapter;
    private BroadcastReceiver receiver;

    @Override
    public void onFinishEditDialog(String id) {
        addContactToRosterAndList(id);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        mRecyclerView = (RecyclerView)findViewById(R.id.contact_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        final ContactModel model = ContactModel.get(getBaseContext());
        adapter = new ContactAdapter(model.getContacts());
        mRecyclerView.setAdapter(adapter);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(LIST)){
                    Log.e(TAG, "We will add some friends to the list");
                    String []ids = intent.getStringArrayExtra("list");
                    for (String id : ids) {
                        Log.e(TAG, "Added " + id);
                        model.addContact(new Contact(id));
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter filter = new IntentFilter(LIST);
        registerReceiver(receiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.contact_list_menu, menu);
        return true;
    }

    //do not permit user to go back to LoginActivity ,unless he presses log out
    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.contact_list_menu_logout:
                Log.e(TAG, "User pressed log out menu item");
                Intent i = new Intent(ChatConnectionService.LOG_OUT);
                sendBroadcast(i);
                finish();
                return true;
            case R.id.contact_list_menu_add:
                FragmentManager fragmentManager = getSupportFragmentManager();
                AddUserFragment fragment = new AddUserFragment();
                fragment.show(fragmentManager, ADDDIALOG);
            default:
                return false;
        }
    }

    private void addContactToRosterAndList(String id) {
        ContactModel model = ContactModel.get(this);
        model.addContact(new Contact(id));

        adapter.notifyDataSetChanged();

        Intent i = new Intent(ChatConnectionService.NEW_FRIEND);
        i.putExtra(ChatConnectionService.BUNDLE_TO, id);
        sendBroadcast(i);
    }

    private class ContactHolder extends RecyclerView.ViewHolder{
        private TextView mContactTextView;
        private Contact mContact;
        public ContactHolder(View itemView) {
            super(itemView);

            mContactTextView = (TextView)itemView.findViewById(R.id.list_item_contact_jid_textview);
            mContactTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(ContactListActivity.this, ChatActivity.class);
                    i.putExtra("EXTRA_JID", mContact.getJid());
                    startActivity(i);
                }
            });
        }

        public void bindContact(Contact contact){
            mContact = contact;

            if(contact != null)
                mContactTextView.setText(mContact.getJid());
        }
    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactHolder>{
        private List<Contact> mContacts;

        public ContactAdapter(List<Contact> list) {
            mContacts = list;
        }

        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            View v = inflater.inflate(R.layout.list_item_contact, parent, false);
            return new ContactHolder(v);
        }

        @Override
        public void onBindViewHolder(ContactHolder holder, int position) {
            Contact contact = mContacts.get(position);
            holder.bindContact(contact);
        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }
    }
}
