package org.xmpp.bots;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

public class Participant {
    private final String xmppName;
    private final String mentionName;
    private boolean isCheckedIn;

    public Participant( ) {
	this.xmppName = null;
	this.mentionName = null;
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

    @Override
    public String toString() {
	return toStringHelper().toString();
    }
    
    public ToStringHelper toStringHelper() {
	return Objects.toStringHelper(this).add("xmppName", xmppName)
	    .add("mentionName", mentionName)
	    .add("isCheckedIn", isCheckedIn)
	    ;
    }

}
