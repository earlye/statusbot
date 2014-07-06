package org.xmpp.bots;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
    protected StatusBot bot;
    protected MultiUserChat room;

    public ChatProcessor(StatusBot bot, MultiUserChat chatRoom) {
	this.bot = bot;
	room = chatRoom;
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
        
	if (msg.matches("[Ss][Tt][Aa][Tt][Uu][Ss][Bb][Oo][Tt].*")) {

	    String[] parts = msg.split(" ");
	    String command = "help";
	    if ( parts.length >= 2 ) {
		command = parts[1];
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
	    }
	    
	    
	} else if (msg.matches("[Ss][Tt][Aa][Tt][Uu][Ss]:.*")) {
	    processStatusMessage(msg,sender,response);
        }

	sendResponse(response);
    }

    public void sendResponse(List<String> response) {
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
	
    public void processStatusMessage(String status, String sender, List<String> response) {
	Participant participant = bot.participants.get( sender );
	if ( participant != null ) {
	    participant.setIsCheckedIn( true );
	    response.add( "Glad to hear it, @" + participant.getMentionName() + "." );
	}
    }
	
    public void processListCommand( String message , String sender , List<String> response ) {
	List<Participant> checkedIn = new ArrayList<Participant>();
	List<Participant> notCheckedIn = new ArrayList<Participant>();
	for ( Participant participant : bot.participants.values() ) {		
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
	String msg = "The following users haven't checked in yet:";
	for ( Participant participant : bot.participants.values() ) {
	    if ( participant.getIsCheckedIn() ) {
		continue;
	    }
	    msg += " @" + participant.getMentionName();
	}
	response.add(msg);
    }

    public void processStartCommand( String message, String sender, List<String> response ) {
	for( Participant participant : bot.participants.values() ) {
	    participant.setIsCheckedIn(false);
	}
    }


    public void processHelpCommand( String message , String sender, List<String> response ) {
	response.add( "/quote start: start the process of reminding people to post status.\n"
		      +"remind: send a private reminder to everybody that they need to check in (coming soon - for now this is an alias for nag.\n"
		      +"nag: send a public reminder to everybody who hasn't checked in that they need to check in.\n"
		      +"list: list the check-in of all participants.\n" );
    }

    public void setupScheduleStartCollection() {
	try {
	    JobDetail job = newJob(StartCollectionJob.class).build();

	    // Trigger the job to run now, and then repeat every 40 seconds
	    Trigger trigger = newTrigger()
		.startNow()
		.withSchedule(cronSchedule("0 0 12 ? * MON-FRI"))
		.build();

	    // Tell quartz to schedule the job using our trigger
	    System.out.println( "scheduler:" + bot.scheduler );
	    bot.scheduler.scheduleJob(job, trigger);
	} catch( SchedulerException se ) {
	    se.printStackTrace();
	    return;
	}

    }

    public void setupScheduleRemindAfternoon() {
	try {
	    JobDetail job = newJob(RemindJob.class).build();

	    // Trigger the job to run now, and then repeat every 40 seconds
	    Trigger trigger = newTrigger()
		.startNow()
		.withSchedule(cronSchedule("0 45 16 ? * MON-FRI"))
		.build();

	    // Tell quartz to schedule the job using our trigger
	    System.out.println( "scheduler:" + bot.scheduler );
	    bot.scheduler.scheduleJob(job, trigger);
	} catch( SchedulerException se ) {
	    se.printStackTrace();
	    return;
	}
    }

    public void setupScheduleRemindMorning() {
	try {
	    JobDetail job = newJob(RemindJob.class).build();

	    // Trigger the job to run now, and then repeat every 40 seconds
	    Trigger trigger = newTrigger()
		.startNow()
		.withSchedule(cronSchedule("8 30 16 ? * MON-FRI"))
		.build();

	    // Tell quartz to schedule the job using our trigger
	    System.out.println( "scheduler:" + bot.scheduler );
	    bot.scheduler.scheduleJob(job, trigger);
	} catch( SchedulerException se ) {
	    se.printStackTrace();
	    return;
	}
    }

    public void setupScheduleNag() {
	try {
	    JobDetail job = newJob(NagJob.class).build();

	    // Trigger the job to run now, and then repeat every 40 seconds
	    Trigger trigger = newTrigger()
		.startNow()
		.withSchedule(cronSchedule("8 45 16 ? * MON-FRI"))
		.build();

	    // Tell quartz to schedule the job using our trigger
	    System.out.println( "scheduler:" + bot.scheduler );
	    bot.scheduler.scheduleJob(job, trigger);
	} catch( SchedulerException se ) {
	    se.printStackTrace();
	    return;
	}
    }

    public void setupSchedules() {
	setupScheduleStartCollection();
	setupScheduleRemindAfternoon();
	setupScheduleRemindMorning();
	setupScheduleNag();
    }
}
 
