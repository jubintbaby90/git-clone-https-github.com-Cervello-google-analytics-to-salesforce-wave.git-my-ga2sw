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

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import models.GoogleAnalyticsReport;
import models.Job;
import models.JobStatus;
import models.dao.GoogleAnalyticsReportDAO;
import models.dao.JobDAO;

import org.apache.commons.lang3.StringEscapeUtils;

import play.Logger;
import akka.actor.UntypedActor;

import com.ga2sa.google.Report;
import com.ga2sa.salesforce.SalesforceDataManager;
import com.google.common.io.Files;
//import org.apache.commons.io.IOUtils;

/**
 * Class for background job
 * 
 * @author Igor Uvarov
 * @editor	Sergey Legostaev
 * 
 */

public class BackgroundJob extends UntypedActor{
	
	/**
	 * Method for handle message for actor. Message is instance of BackroundJob class. 
	 */
	@Override
	public void onReceive(Object obj) throws Exception {
		if (obj instanceof Job) {
			
			Job job = JobDAO.findById(((Job) obj).id);
			
			if (job.getStatus().equals(JobStatus.CANCELED)) return;
			
			Report report = null;
			File csvReport = null;
			Logger.debug("Job started: " + job.getName());

			GoogleAnalyticsReport previousReport = GoogleAnalyticsReportDAO.getReportByJobId(job.getId());
			try {
				if (job.isRepeated()) {
					
					Integer duration = (job.getRepeatPeriod().equals("week")) ? 7 : 1;
					Integer timeUnit = (job.getRepeatPeriod().equals("week") || job.getRepeatPeriod().equals("day") ) ? Calendar.DATE : Calendar.MONTH;
						
					if (job.needIncludePreviousData() && previousReport != null) {
						
						Calendar startDateForReport = Calendar.getInstance();
						Calendar endDateForReport = Calendar.getInstance();
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
						
						endDateForReport.setTime(sdf.parse(job.gaEndDate));
						startDateForReport.setTime(endDateForReport.getTime());
						startDateForReport.add(Calendar.DATE, 1);
						
						endDateForReport.add(timeUnit, duration);
						job.gaStartDate = sdf.format(startDateForReport.getTime());
						job.gaEndDate = sdf.format(endDateForReport.getTime());
						
						report = Report.getReport(job);
						csvReport = report.addToCSV(previousReport.data);
					
					} 
				}
				
				if (csvReport == null)  {
					report = Report.getReport(job);
					csvReport = report.toCSV();
				}
			
				if (job.isRepeated() && job.needIncludePreviousData() && previousReport != null) {
					previousReport.data = Files.toByteArray(csvReport);
					GoogleAnalyticsReportDAO.update(previousReport);
				} else {
					GoogleAnalyticsReportDAO.save(new GoogleAnalyticsReport(job.getId(), Files.toByteArray(csvReport)));
				}
				
				SalesforceDataManager.uploadData(job.getSalesforceAnalyticsProfile(), csvReport);
				job.setStatus(JobStatus.OK);
				job.setMessages(report.getData().size() + " rows have been loaded.");
				
			} catch (Exception e) {
				e.printStackTrace();
				job.setStatus(JobStatus.FAIL);
				job.setMessages( StringEscapeUtils.escapeHtml4(e.getMessage()));
			}
			
			job.setEndTime(new Timestamp(new Date().getTime()));
			
			try {
				JobDAO.update(job);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (csvReport != null) csvReport.delete();
		} else {
			unhandled(obj);
		}
	}
	
}