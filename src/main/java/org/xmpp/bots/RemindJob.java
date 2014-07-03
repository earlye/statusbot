package org.xmpp.bots;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class RemindJob implements Job {

    public void execute(JobExecutionContext context) {
	System.out.println( "RemindJob.execute()" );

	for ( ChatProcessor chatProcessor : StatusBot.instance.rooms ) {
	    List<String> response = new ArrayList<String>();
	    chatProcessor.processRemindCommand( null,null, response );
	    chatProcessor.sendResponse(response);
	}
    }

}
