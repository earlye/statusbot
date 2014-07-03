package org.xmpp.bots;

public class Participant {
    private final String xmppName;
    private final String mentionName;
    private boolean isCheckedIn;

    public Participant( String xmppName , String mentionName ) {
	this.xmppName = xmppName;
	this.mentionName = mentionName;
	this.isCheckedIn = false;
    }

    public String getXmppName() {
	return xmppName;
    }

    public String getMentionName() {
	return mentionName;
    }

    public boolean getIsCheckedIn() {
	return isCheckedIn;
    }

    public void setIsCheckedIn(boolean value) {
	isCheckedIn = value;
    }
}
