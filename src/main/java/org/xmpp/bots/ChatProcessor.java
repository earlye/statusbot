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

	    // Statusbot commands.  Maybe:

	    // start - re-read user list - all users get set to not chimed in.
	    // remind - send friendly reminder (private message) to users.
	    // nag - send public @notification to all users who haven't chimed in.
	    // list - list participant states.

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
	for ( Participant participant : bot.participants.values() ) {		
	    response.add( participant.getMentionName() + " isCheckedIn:" + participant.getIsCheckedIn() );
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
	response.add( "Started" );
	processRemindCommand( message, sender, response );
    }


    public void processHelpCommand( String message , String sender, List<String> response ) {
	response.add( "/quote start: start the process of reminding people to post status.\n"
		      +"remind: send a private reminder to everybody that they need to check in (coming soon - for now this is an alias for nag.\n"
		      +"nag: send a public reminder to everybody who hasn't checked in that they need to check in.\n"
		      +"list: list the check-in of all participants.\n" );
    }
}
 
