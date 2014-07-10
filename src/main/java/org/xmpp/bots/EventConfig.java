package org.xmpp.bots;

import java.util.List;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

public class EventConfig {

    private String cron;
    private String commandType;

    public void setCron( String value ) {
	cron = value;
    }

    public String getCron() {
	return cron;
    }

    public void setCommandType( String value ) {
	commandType = value;
    }

    public String getCommandType() {
	return commandType;
    }
    
    @Override
    public String toString() {
	return toStringHelper().toString();
    }
    
    public ToStringHelper toStringHelper() {
	return Objects.toStringHelper(this)
	    .add("cron",cron)
	    .add("commandType", commandType)
	    ;
    }

}
