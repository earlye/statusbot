package org.xmpp.bots;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Class;
import java.lang.ClassNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Affiliate;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.DateBuilder;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

public class ChatProcessor implements PacketListener {
    protected RoomConfig roomConfig;
    protected Map<String,Participant> participants;
    protected Map<String,Participant> participantsByMention;
    protected MultiUserChat room;

    public ChatProcessor(RoomConfig roomConfig) throws SchedulerException {
	this.roomConfig = roomConfig;
	participants = new HashMap<String,Participant>();
	participantsByMention = new HashMap<String,Participant>();

	if ( null != roomConfig && null != roomConfig.getParticipants() ) {
	    for( Participant participant : roomConfig.getParticipants() ) {
		addParticipant(participant);
	    }
	}
	setupSchedules();
    }

    public String getConfRoomName() {
	return roomConfig.getName() + "@" + StatusBot.getBotConfig().getConference();
    }

    public void connect() throws XMPPException {
	String confRoomName = getConfRoomName();
	System.out.println("Joining room: " + confRoomName);

	MultiUserChat room = new MultiUserChat(StatusBot.getConnection(),confRoomName);
	DiscussionHistory history = new DiscussionHistory();
	history.setMaxStanzas(0);
	room.addMessageListener(this);
	room.join(StatusBot.getBotConfig().getNick(),
		  StatusBot.getBotConfig().getPassword(), 
		  history,
		  SmackConfiguration.getPacketReplyTimeout());
	this.room = room;
    }

    public void addParticipant( Participant participant ) {
	System.out.println( "Adding participant:" + participant.toString() );
	participants.put( participant.getXmppName() , participant );
	participantsByMention.put(participant.getMentionName(), participant);
    }
		
    public void processPacket(Packet packet) {
	System.out.println("Packet received:" + packet.toXML());

	if (packet instanceof Message)
	    processMessage((Message)packet);
    }

    public void processMessage(Message message ) {
	List<String> response = new ArrayList<String>();
	String msg = message.getBody();
	String sender = message.getFrom();
        	
	System.out.println("Received message: " + msg);
        
	if (msg.matches("@[Ss][Tt][Aa][Tt][Uu][Ss][ \t\n\r]*[Bb][Oo][Tt][ \t\n\r]*.*")) {

	    String[] parts = msg.split(" ");
	    String command = "help";
	    if ( parts.length >= 3 ) {
		command = parts[2];
	    }

	    System.out.println( "Received command:" + command );

	    // Statusbot commands.
	    // It would be nice to implement this using a Map<String,StatusBotCommand>, 
	    // where StatusBotCommand is an interface containing one method to deal
	    // with that command.  For now, an ugly if/else tree will suffice.
	    if ( command.equalsIgnoreCase("list") ) {
		processListCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "start" )) {
		processStartCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "remind" )) {
		processRemindCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "nag" )) {
		processNagCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "help" )) {
		processHelpCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "excuse" )) {
		processExcuseCommand(msg,sender,response);
	    } else if ( command.equalsIgnoreCase( "forget" )) {
		processForgetCommand(msg,sender,response);
	    }
	    
	} else if (msg.matches("@[Ss][Tt][Aa][Tt][Uu][Ss][ \t\n\r]*.*")) {

	    processStatusMessage(msg,sender,response);

        }

	sendResponse(response);
    }

    public void sendResponse(List<String> response) {
	if ( null == room ) {
	    System.err.println( "I was asked to send a response, but I'm not connected to a room." );
	    for( String responseStr : response ) {
		System.err.println( responseStr );
	    }
	    return;
	}
        if ( !response.isEmpty() ) {
	    try {
		// need delay before sending msg, or things appear out of order
		try {
		    Thread.sleep(400);
		    for( String responseStr : response ) {
			Thread.sleep(100);
			room.sendMessage(responseStr);
		    }
		}
		catch (InterruptedException ex) {
		    System.err.println("Caught Interrupted Exception");
		    ex.printStackTrace();
		}
	    } catch (XMPPException ex) {
		System.err.println("Caught XMPP Exception");
		ex.printStackTrace();
	    }
        }
    }

    public void processSetIsCheckedIn(String status, boolean isCheckedIn) {
	String[] parts = status.split(" ");
	int i = 2; // 0-> @status 1-> bot 2-> forget/excuse...
	while( ++i < parts.length ) {
	    String mention = parts[i].substring(1);
	    Participant participant = participantsByMention.get(mention);
	    if ( participant != null ) {
		participant.setIsCheckedIn(isCheckedIn);
	    }
	}
    }

    public void processForgetCommand(String status , String sender, List<String> response ) {
	processSetIsCheckedIn(status,false);	
    }

    public void processExcuseCommand(String status , String sender, List<String> response ) {
	processSetIsCheckedIn(status,true);
    }
	
    public void processStatusMessage(String status, String sender, List<String> response) {
	Participant participant = participants.get( sender );
	if ( participant != null ) {
	    participant.setIsCheckedIn( true );
	    response.add( "Glad to hear it, @" + participant.getMentionName() + "." );
	}
    }
	
    public void processListCommand( String message , String sender , List<String> response ) {
	List<Participant> checkedIn = new ArrayList<Participant>();
	List<Participant> notCheckedIn = new ArrayList<Participant>();
	for ( Participant participant : participants.values() ) {		
	    if ( participant.getIsCheckedIn() ) {
		checkedIn.add(participant);
	    } else {
		notCheckedIn.add(participant);
	    }
	}

	if ( !checkedIn.isEmpty() ) {
	    String msg = "The following users have checked in:";
	    for( Participant participant : checkedIn ) {
		msg += " " + participant.getMentionName();
	    }
	    response.add(msg);
	}

	if ( !notCheckedIn.isEmpty() ) {
	    String msg = "The following users have not checked in:";
	    for( Participant participant : notCheckedIn ) {
		msg += " " + participant.getMentionName();
	    }
	    response.add(msg);	    
	}	   
    }

    public void processRemindCommand( String message , String sender , List<String> response ) {
	// Ideally this will generate private messages.
	processNagCommand(message,sender,response);
    }

    public void processNagCommand( String message, String sender, List<String> response ) {
	List<Participant> notCheckedIn = new ArrayList<Participant>();
	for( Participant participant : participants.values() ) {
	    if (!participant.getIsCheckedIn()) {
		notCheckedIn.add(participant);
	    }
	}

	if (!notCheckedIn.isEmpty()) {
	    String msg = "The following users haven't checked in yet:";
	    for ( Participant participant : notCheckedIn ) {
		msg += " @" + participant.getMentionName();
	    }
	    response.add(msg);
	}
    }

    public void processStartCommand( String message, String sender, List<String> response ) {
	for( Participant participant : participants.values() ) {
	    participant.setIsCheckedIn(false);
	}
	response.add("/quote ------------------------------------------------------------\n"
		     +"Status bot is now accepting status messages\n"
		     +"------------------------------------------------------------\n");
    }


    public void processHelpCommand( String message , String sender, List<String> response ) {
	response.add( "/quote start: start the process of reminding people to post status.\n"
		      +"remind: send a private reminder to everybody that they need to check in (coming soon - for now this is an alias for nag.\n"
		      +"nag: send a public reminder to everybody who hasn't checked in that they need to check in.\n"
		      +"list: list the check-in of all participants.\n"
		      +"excuse: excuse specified team members from providing status (i.e., set isCheckedIn = true)\n"
		      +"forget: forget that specified team members have provided status (i.e., set isCheckedIn = false)\n" );
    }

    public void scheduleJob( Class klass , String cronString ) throws SchedulerException {
	JobDetail job = newJob(klass)
	    .usingJobData("RoomName",getConfRoomName())
	    .build();
	
	Trigger trigger = newTrigger()
	    .startNow()
	    .withSchedule(cronSchedule(cronString))
	    .build();
	
	System.out.println( "Scheduling job \"" + klass.getName() + "\":" + cronString + " for room:" + getConfRoomName());
	StatusBot.getScheduler().scheduleJob(job,trigger);
    }

    public void setupSchedules() throws SchedulerException {
	if ( null != roomConfig.getEventConfigs()) {
	    for( EventConfig eventConfig : roomConfig.getEventConfigs() ) {
		try {
		    Class klass = Class.forName( eventConfig.getCommandType() );
		    scheduleJob( klass , eventConfig.getCron() );
		} catch( ClassNotFoundException e ) {
		    System.err.println( "Could not find class while setting up schedule." );
		    e.printStackTrace();
		}
	    }
	}
    }
}
 
