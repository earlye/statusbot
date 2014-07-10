package org.xmpp.bots;

import java.util.List;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

public class RoomConfig {

    private String name;
    private List<Participant> participants;
    private List<EventConfig> eventConfigs;
    
    public void setName( String value ) {
	name = value;
    }

    public String getName() {
	return name;
    }
    
    public void setParticipants( List<Participant> value ) {
	participants = value;
    }

    public List<Participant> getParticipants() {
	return participants;
    }

    public void setEventConfigs( List<EventConfig> value ) {
	eventConfigs = value;
    }

    public List<EventConfig> getEventConfigs() {
	return eventConfigs;
    }

    @Override
    public String toString() {
	return toStringHelper().toString();
    }
    
    public ToStringHelper toStringHelper() {
	return Objects.toStringHelper(this).add("name", name)
	    .add("participants", participants)
	    .add("eventConfigs", eventConfigs)
	    ;
    }

}
