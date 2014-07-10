package org.xmpp.bots;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

public class BotConfig {

    private String host;
    private Integer port;
    private String user;
    private String nick;
    private String password;
    private String agent;
    private String conference;
    private List<RoomConfig> rooms;
    private Boolean SASLAuthenticationEnabled;

    public BotConfig() {
	SASLAuthenticationEnabled = true;
    }

    static public BotConfig parseJson( InputStream inputStream ) throws IOException {
	if ( inputStream == null ) {
	    throw new NullPointerException();
	}

	ObjectMapper objectMapper = new ObjectMapper();
	objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	BotConfig result = (BotConfig)(objectMapper.readValue(inputStream, BotConfig.class));
	System.out.println( result.toString() );
	return result;
    }

    static public BotConfig parseJsonResource( String path ) throws IOException {
	return parseJson( Thread.currentThread().getContextClassLoader().getResourceAsStream(path) );
    }

    public String getHost() {
	return host;
    }

    public void setHost(String value) {
	host = value;
    }

    public Integer getPort() {
	return port;
    }

    public void setPort(Integer value) {
	port = value;
    }

    public String getUser() {
	return user;
    }

    public void setUser(String value) {
	user = value;
    }

    public String getNick() {
	return nick;
    }

    public void setNick(String value) {
	nick = value;
    }

    public String getPassword() {
	return password;
    }

    public void setPassword(String value) {
	password = value;
    }

    public String getAgent() {
	return agent;
    }

    public void setAgent(String value) {
	agent = value;
    }

    public String getConference() {
	return conference;
    }

    public void setConference(String value) {
	conference = value;
    }

    public List<RoomConfig> getRooms() {
	return rooms;
    }

    public void setRoomConfig(List<RoomConfig> value ) {
	rooms = value;
    }

    public Boolean getSASLAuthenticationEnabled() {
	return SASLAuthenticationEnabled;
    }

    public void setSASLAuthenticationEnabled(Boolean value) {
	SASLAuthenticationEnabled = value;
    }

    @Override
    public String toString() {
	return toStringHelper().toString();
    }
    
    public ToStringHelper toStringHelper() {
	return Objects.toStringHelper(this).add("host", host)
	    .add("port", port)
	    .add("user", user)
	    .add("nick", nick)
	    .add("password", password)
	    .add("agent", agent)
	    .add("conference", conference)
	    .add("SASLAuthenticationEnabled",SASLAuthenticationEnabled)
	    .add("rooms", rooms)
	    ;
    }

};
