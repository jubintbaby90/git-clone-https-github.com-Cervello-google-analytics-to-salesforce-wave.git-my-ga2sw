/**
 * This document is a part of the source code and related artifacts
 * for GA2SA, an open source code for Google Analytics to 
 * Salesforce Analytics integration.
 *
 * Copyright © 2015 Cervello Inc.,
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.ga2sa.scheduler;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import models.Job;
import models.dao.JobDAO;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
/**
 * 
 * Class for manage scheduled jobs. The one can start, stop and update existing jobs. 
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 * 
 */
public class SchedulerManager extends UntypedActor {
	
	/**
	 * Method for handle of message for actor. For start all jobs need to send 'start' work, for update status for all jobs need to send "update" word
	 * 
	 */
	@Override
	public void onReceive(Object obj) throws Exception {
		
		if (obj instanceof String) {
			String command = (String) obj;
			switch (command) {
				case "start" 	: 
					Logger.debug("******* START SCHEDULER *******"); 
					this.start();
					break;
				case "update" 	: 
					Logger.debug("******* UPDATE LIST JOBS *******"); 
					this.update();
					break;
			}
			
		} else if (obj instanceof Job) {
			Logger.debug("******* START NEW JOB *******"); 
			update((Job) obj);
		} else {
			unhandled(obj);
		}
	}
	
	private void update() {
		Job job = JobDAO.getLastJob();
		
		Logger.debug("JOB   " + job.getName());
		
		Calendar currentDate = Calendar.getInstance();
		
		runJob(job, currentDate);
		
	}
	
	private void update(Job job) {
		Logger.debug("JOB   " + job.getName());
		Calendar currentDate = Calendar.getInstance();
		runJob(job, currentDate);
		
	}

	private void start() {
		List<Job> jobs = JobDAO.getJobsForScheduler();
		Calendar currentDate = Calendar.getInstance();
		jobs.forEach(job -> runJob(job, currentDate));
	}
	
	private void runJob(Job job, Calendar currentDate) {
		
		Calendar scheduleDate = Calendar.getInstance();
		Calendar startDate = Calendar.getInstance();
		
		
		ActorRef backgroundJob = Akka.system().actorOf(Props.create(BackgroundJob.class));
		
		if (job.getNextStartTime() != null) {
			startDate.setTime(job.getNextStartTime());
			if (startDate.after(currentDate)) scheduleDate.setTime(job.getNextStartTime());
		} else {
			startDate.setTime(job.getStartTime() == null ? Calendar.getInstance().getTime() : job.getStartTime());
			if (startDate.after(currentDate)) {
				Logger.debug("AFTER");
				scheduleDate.setTime(job.getStartTime());
			}
		}
		
		long offset = (scheduleDate.getTimeInMillis() - currentDate.getTimeInMillis()) / 1000;
		
		Logger.debug("OFFSET " + String.valueOf(offset) );

		if (job.isRepeated()) {
			
			Integer duration = (job.getRepeatPeriod().equals("week")) ? 7 : 1;
			Integer timeUnit = (job.getRepeatPeriod().equals("week") || job.getRepeatPeriod().equals("day") ) ? Calendar.DATE : Calendar.MONTH;
			
			Calendar period = Calendar.getInstance();
			period.setTime(scheduleDate.getTime());
			
			period.add(timeUnit, duration);
			
			// for testing
			//period.add(Calendar.MINUTE, 1);
			
			Logger.debug("NEXT START " + period.getTime().toString());
			
			job.setNextStartTime(new Timestamp(period.getTimeInMillis()));
			
			long offsetPeriod = (period.getTimeInMillis() - scheduleDate.getTimeInMillis()) / 1000;
			
			Akka.system().scheduler().schedule(FiniteDuration.create(offset, TimeUnit.SECONDS), FiniteDuration.create(offsetPeriod, TimeUnit.SECONDS), backgroundJob, job, Akka.system().dispatcher(), ActorRef.noSender());
		
		} else {
			
			Akka.system().scheduler().scheduleOnce(FiniteDuration.create(offset, TimeUnit.SECONDS), backgroundJob, job, Akka.system().dispatcher(), ActorRef.noSender());
		}
	}

}