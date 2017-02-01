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

package com.ga2sa.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Job;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.Column;
import com.google.api.services.analytics.model.Columns;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;
/**
 * 
 * Wrapper class for work with Google Analytics Api
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 */
public class GoogleAnalyticsDataManager {
	
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	private static final Integer LIMIT_RECORDS = 1000;
	
	
	private static Analytics getAnalytics(GoogleCredential credential) {
		return new Analytics.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
	}
	
	/**
	 * Get all accounts from GA profile
	 * 
	 * @param Google Credential
	 * @return GA accounts
	 */
	
	public static Accounts getAccounts(GoogleCredential credential) {
		try {
			return getAnalytics(credential).management().accounts().list().execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get web properties from GA profile
	 * 
	 * @param c
	 * @param GA account id
	 * @return GA webproperies
	 */
	
	public static Webproperties getProperties(GoogleCredential credential, String accountId) {
		try {
			return getAnalytics(credential).management().webproperties().list(accountId).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get profiles from GA profle
	 * 
	 * @param Google Credential
	 * @param GA account id
	 * @param GA property id
	 * @return analytics profiles
	 */

	public static Profiles getProfiles(GoogleCredential credential, String accountId, String propertyId) {
		try {
			return  getAnalytics(credential).management().profiles().list(accountId, propertyId).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * List of dimensions from GA profile
	 * 
	 * @param Google Credential
	 * @return GA dimensions
	 */
	public static List<Map<String, String>> getDimensions(GoogleCredential credential) {
		return getObject(credential, "DIMENSION");
	}
	/**
	 * List of metrics from GA profile
	 * @param credential
	 * @return GA metrics
	 */
	public static List<Map<String, String>> getMetrics(GoogleCredential credential) {
		return getObject(credential, "METRIC");
	}
	
	private static List<Map<String, String>> getObject(GoogleCredential credential, String type) {
		List<Map<String, String>> filteredColumns = new ArrayList<Map<String, String>>();
		try {
			Columns columns = getAnalytics(credential).metadata().columns().list("ga").execute();
			for (Column c : columns.getItems()) {
				if (c.getAttributes().get("type").equals(type) && c.getAttributes().get("status").equals("PUBLIC")) {
					c.getAttributes().put("id", c.getId());
					filteredColumns.add(c.getAttributes());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return filteredColumns;
	}
	
	/**
	 * 
	 * Get report from GA
	 * 
	 * @param Google Credential
	 * @param Job
	 * @return GA report
	 * @throws IOException 
	 */
	public static GaData getReport(GoogleCredential credential, Job job) throws IOException {
		if (job != null) { 
			Get query = getAnalytics(credential).data().ga().get(
				"ga:" + job.gaProfile,
				job.gaStartDate,
				job.gaEndDate,
				job.gaMetrics
			).setDimensions(job.gaDimensions);
			
			if (job.gaSorting != null) {
				query.setSort(job.gaSorting);
			}
			Logger.debug("QUERY    " + query.toString());
			GaData gaData = query.execute();
			
			if (gaData.getTotalResults() > LIMIT_RECORDS) {
				Logger.debug("TOTAL RESULTS : " + gaData.getTotalResults());
				List<List<String>> records = new ArrayList<List<String>>(gaData.getRows());
				do {
					Logger.debug(gaData.getSelfLink());
					gaData = query.setStartIndex(records.size() + 1).execute();
					records.addAll(gaData.getRows());
				} while (gaData.getNextLink() != null);
					
				Logger.debug("ROWS SIZE : " + records.size());
				gaData.setRows(records);
			}
			
			return gaData;
		}
		return null;
	}
	
	/**
	 * 
	 * Get report from GA
	 * 
	 * @param Google Credential
	 * @param googleAnalyticsProperties
	 * @return GA report
	 * @throws Exception
	 */
	@Deprecated
	public static GaData getReport(GoogleCredential credential, String googleAnalyticsProperties) throws Exception {
		if (googleAnalyticsProperties != null) { 
			JsonNode params = Json.parse(googleAnalyticsProperties);
			String metrics = params.get("metrics").isArray() ? StringUtils.join(params.get("metrics").elements(), ",").replace("\"", "") : params.get("metrics").asText().replace("\"", "");
			String dimensions = params.get("dimensions").isArray() ? StringUtils.join(params.get("dimensions").elements(), ",").replace("\"", "") : params.get("dimensions").asText().replace("\"", "");
			
			Logger.debug("QUERY    " + googleAnalyticsProperties);
			
			Get query = getAnalytics(credential).data().ga().get(
				"ga:" + params.get("analyticsProfile").textValue(),
				params.get("startDate").textValue(),
				params.get("endDate").textValue(),
				metrics
			).setDimensions(dimensions);
			
			if (!params.get("sort").asText().isEmpty()) {
				String sort = params.get("sort").isArray() ? StringUtils.join(params.get("sort").elements(), ",").replace("\"", "") : params.get("sort").asText().replace("\"", "");
				query.setSort(sort);
			}
			
			return query.execute();
		}
		return null;
	}
	
	/*public GaData getAnalyticData(Query query) throws IOException {
		GaData result = null;
		try {
			result = this.analytics.data().ga().get("ga:" + query.profile, query.startDate, query.endDate, StringUtils.join(query.metrics, ","))
					.setDimensions(StringUtils.join(query.dimensions, ",")).setMaxResults(25).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}*/

}