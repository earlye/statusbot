package org.xmpp.bots;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;


public class StatusBot {
	
    protected Connection connection;
    protected Map<String,ChatProcessor> rooms; // Name => Room
    private Scheduler scheduler;

    
    protected boolean shouldRun = true;
    static private StatusBot instance = null;
    private BotConfig botConfig = null;

    private StatusBot() {
	Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
		@Override
		public void uncaughtException(Thread arg0, Throwable arg1) {
		    System.err.println("Exception caught");
		    arg1.printStackTrace();
		}
	    });
    }

    static public StatusBot getInstance() {
	return instance;
    }

    static public Scheduler getScheduler() {
	return instance.scheduler;
    }
	
    public void disconnect() {
	if (rooms != null) {
	    for (ChatProcessor room: rooms.values() ) {
		try {
		    room.room.leave();
		} catch( IllegalStateException exc ) {
		    System.err.println("Ignoring IllegalStateException. If we're not connected, we can't exactly leave a room...");
		    exc.printStackTrace();
		}
	    }
	}
    }
	
    public void connect() {
	disconnect();
	try {
	    botConfig = BotConfig.parseJsonResource( "config.json" );
	} catch( IOException e ) {
	    System.out.println( "IOException reading bot config." );
	    e.printStackTrace();
	    return;
	}

	ConnectionConfiguration config = new ConnectionConfiguration( botConfig.getHost() , botConfig.getPort() );
	//config.setCompressionEnabled(true);
	config.setSASLAuthenticationEnabled(botConfig.getSASLAuthenticationEnabled());
		
	System.out.println("Creating XMPP connection");
	connection = new XMPPConnection(config);	
	try {
	    connection.connect();
	    SASLAuthentication.supportSASLMechanism("PLAIN", 0);
	    connection.login(botConfig.getUser(), botConfig.getPassword());
	}
	catch (XMPPException ex) {
	    System.err.println("Caught XMPP Exception while connecting and logging in");
	    ex.printStackTrace();
	}
		
	try {
	    rooms = new HashMap<String,ChatProcessor>();
	    for( RoomConfig roomConfig : botConfig.getRooms() ) {
		String confRoomName = roomConfig.getName() + "@" + botConfig.getConference();

		System.out.println("Joining room: " + confRoomName);
		MultiUserChat room = new MultiUserChat(connection,confRoomName);
		DiscussionHistory history = new DiscussionHistory();
		history.setMaxStanzas(0);
		ChatProcessor chatProcessor = new ChatProcessor(room,roomConfig);
		chatProcessor.setupSchedules();
		rooms.put(confRoomName,chatProcessor);
		room.addMessageListener(chatProcessor);
		room.join(botConfig.getNick(),botConfig.getPassword(), history, SmackConfiguration.getPacketReplyTimeout());
	    }

	}
	catch (XMPPException ex) {
	    System.err.println("Caught XMPP Exception while joining room");
	    ex.printStackTrace();
	}
	catch(IllegalStateException ex) {
	    System.err.println("Caught IllegalStateException while joining room");
	    ex.printStackTrace();			
	}
	catch(SchedulerException ex) {
	    System.err.println("Caught SchedulerException while joining room");
	}
    }
	
    public void quit() {
	System.out.println("Disconnecting XMPP connection");
	connection.disconnect();	
    }
	
    public boolean running() {
	return shouldRun;
    }
	
    public void stopRunning() {
	stopScheduler();
	shouldRun = false;
    }


    public void startScheduler() {
        try {
	    System.out.println( "starting scheduler" );
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException se) {
            se.printStackTrace();
	    return;
        }
    }

    public void stopScheduler() {
	try {
	    if ( scheduler != null )
		scheduler.shutdown();
	    scheduler = null;
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }    
	
    public static void main(String[] args) {
	instance = new StatusBot();
	instance.startScheduler();
	instance.connect();

	while (instance.running()) {
	    try {
		// pause briefly
		Thread.sleep(500);
		if ( !instance.connection.isConnected()) {
		    instance.connect();
		}
	    }
	    catch (InterruptedException ex) {
		System.err.println("Caught Interrupted Exception");
		ex.printStackTrace();
	    } 
		
	}
	    
	// another delay, so clean up can occur (eg Wismar msg).
	try {
	    // pause briefly
	    Thread.sleep(2000);
	}
	catch (InterruptedException ex) {
	    System.err.println("Caught Interrupted Exception");
	    ex.printStackTrace();
	}

	instance.quit();
    }
    
}
