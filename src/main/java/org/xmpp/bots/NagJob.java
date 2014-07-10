package org.xmpp.bots;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class NagJob implements Job {

    public void execute(JobExecutionContext context) {
	System.out.println( "NagJob.execute()" );

	String roomName = context.getJobDetail().getJobDataMap().getString("RoomName");
	ChatProcessor chatProcessor = StatusBot.getInstance().rooms.get(roomName);
	if ( chatProcessor == null ) {
	    System.err.println( "Could not find room \"" + roomName + "\"" );
	    return;
	}

	List<String> response = new ArrayList<String>();
	chatProcessor.processNagCommand( null,null, response );
	chatProcessor.sendResponse(response);
    }

}
