package org.xmpp.bots;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class StartCollectionJob implements Job {

    public void execute(JobExecutionContext context) {
	System.out.println( "StartCollectionJob.execute()" );

	for ( ChatProcessor chatProcessor : StatusBot.instance.rooms ) {
	    List<String> response = new ArrayList<String>();
	    chatProcessor.processStartCommand( null,null, response );
	    chatProcessor.sendResponse(response);
	}
    }

}
