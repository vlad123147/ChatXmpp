package com.crivenco.vlad.chatxmpp.Model;

/**
 * Created by vlad on 4/24/17.
 */

public class Contact {

    private String jid;

    public Contact(String contactJid){
        jid = contactJid;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }
}
