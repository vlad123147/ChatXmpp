package com.crivenco.vlad.chatxmpp.Model;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;



import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 4/24/17.
 */

public class ContactModel {
    public static final String TAG = "ChatContactModel";
    private static ContactModel sContactModel;
    private List<Contact> mContacts;

    public static ContactModel get(Context context){
        if (sContactModel == null){
            sContactModel = new ContactModel(context);
        }

        return sContactModel;
    }

    private ContactModel(Context context){
        mContacts = new ArrayList<>();

        //addDefContacts();
    }

    public void addContact(Contact contact){
        for (Contact c: mContacts){
            if (c.getJid().equals(contact.getJid())) {
                Log.e(TAG, "Failed to add " + contact.getJid() + " to list of contacts");
                return;
            }
        }
        mContacts.add(contact);
    }

    private void addDefContacts() {
        Contact contact1 = new Contact("vlad.crivenco@xmpp.jp");
        mContacts.add(contact1);
        Contact contact2 = new Contact("radu.crivenco@xmpp.jp");
        mContacts.add(contact2);
    }

    public List<Contact> getContacts(){
        return mContacts;
    }
}
