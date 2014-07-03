package org.xmpp.bots;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class NagJob implements Job {

    public void execute(JobExecutionContext context) {
	System.out.println( "NagJob.execute()" );

	for ( ChatProcessor chatProcessor : StatusBot.instance.rooms ) {
	    List<String> response = new ArrayList<String>();
	    chatProcessor.processNagCommand( null,null, response );
	    chatProcessor.sendResponse(response);
	}
    }

}
