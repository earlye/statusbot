package org.xmpp.bots;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

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

public class ChatProcessor implements PacketListener {
    protected StatusBot bot;
    protected MultiUserChat room;
	
    public ChatProcessor(StatusBot bot, MultiUserChat chatRoom) {
	this.bot = bot;
	room = chatRoom;
    }
		
    public void processPacket(Packet packet) {
	if (packet instanceof Message)
	    processMessage((Message)packet);
    }

    public void processMessage(Message message ) {
	List<String> response = new ArrayList<String>();
	String msg = message.getBody();
	String sender = message.getFrom();
        	
	System.out.println("Received message: " + msg);
        
	if (msg.matches("[Ss][Tt][Aa][Tt][Uu][Ss]:.*")) {
	    System.out.println("Msg starts with status:");
	    response.add( "Glad to hear it, " + sender );
	    response.add( "/code " + message );
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
	
	
	
}
 
