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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Job;

//import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;

import com.ga2sa.security.ApplicationSecurity;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.GaData.ColumnHeaders;
import com.google.common.io.Files;

/**
 * 
 * 
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 */
public class Report {

	private String name;
	private List<String> headers;
	private List<List<String>> data;
	private List<Integer> dateColumns;

	public List<List<String>> getData() {
		return data;
	}

	public void setData(List<List<String>> data) {
		this.data = data;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//	public Report(String name, GaData data, JsonNode metrics, JsonNode dimensions) {
//
//		this.name = name;
//		this.headers = new ArrayList<String>();
//		this.dateColumns = new ArrayList<Integer>();
//		if (data != null) {
//			for (ColumnHeaders item : data.getColumnHeaders()) {
//				if (item.getColumnType().equals("DIMENSION")) {
//					dimensions.forEach(
//							(dimension) -> {
//								if (dimension.get("id").asText().equals(item.getName())) 
//									this.headers.add(dimension.get("uiName").asText());
//							}
//					);
//				} else {
//					metrics.forEach(
//							(metric) -> {
//								if (metric.get("id").asText().equals(item.getName())) 
//									this.headers.add(metric.get("uiName").asText());
//							}
//					);
//				}
//			
//				// Save index date column for formatting in CSV file
//				if (item.getName().equals("ga:date")) this.dateColumns.add(data.getColumnHeaders().indexOf(item));
//			
//			}
//		}
//		
//		this.data = data == null ? null : data.getRows();
//		
//	}
	
	public Report(String name, GaData data, List<Map<String, String>> metrics, List<Map<String, String>> dimensions) {
		this.name = name;
		this.headers = new ArrayList<String>();
		this.dateColumns = new ArrayList<Integer>();
		if (data != null) {
			for (ColumnHeaders item : data.getColumnHeaders()) {
				if (item.getColumnType().equals("DIMENSION")) {
					dimensions.forEach(dimension -> {
								if (dimension.get("id").equals(item.getName())) 
									this.headers.add(dimension.get("uiName"));
							});
				} else {
					metrics.forEach(metric -> {
								if (metric.get("id").equals(item.getName())) 
									this.headers.add(metric.get("uiName"));
							});
				}
			
				// Save index date column for formatting in CSV file
				if (item.getName().equals("ga:date")) this.dateColumns.add(data.getColumnHeaders().indexOf(item));
			
			}
		}
		
		this.data = data == null ? null : data.getRows();
	}

	public File toCSV() {
		
		String root = Play.isDev() ? "" : "/app/target/universal/stage/";
		File csv = new File(root + this.name + ".csv");
		
		try {
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(csv));

			String header = StringUtils.join(this.headers, ",");

			bw.write(header);
			bw.newLine();
			if (data != null) {
				this.data.forEach((row) -> {
					
					// formatting all date columns
					this.dateColumns.forEach((indexColumn) -> {
						row.set(indexColumn, convertToIsoDate(row.get(indexColumn)));
					});
					
					try {
						bw.write(StringUtils.join(row.toArray(), ","));
						bw.newLine();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				});
			}
			
			bw.flush();
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return csv;
	}
	
	public File addToCSV(byte[] prevCSV) {
		
		String root = Play.isDev() ? "" : "/app/target/universal/stage/";
		File csv = new File(root + this.name + ".csv");
		
		try {
			
			Files.write(prevCSV, csv);
			//Files.writeByteArrayToFile(csv, prevCSV);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
			if (data != null) {
				this.data.forEach((row) -> {
					
					// formatting all date columns
					this.dateColumns.forEach((indexColumn) -> {
						row.set(indexColumn, convertToIsoDate(row.get(indexColumn)));
					});
					
					try {
						bw.write(StringUtils.join(row.toArray(), ","));
						Logger.debug("# " + StringUtils.join(row.toArray(), ","));
						bw.newLine();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				});
			}
			
			bw.flush();
			bw.close();
	
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return csv;
	}
	
	private String convertToIsoDate(String gaDate) {
		String year = gaDate.substring(0, 4);
		String month = gaDate.substring(4, 6);
		String day = gaDate.substring(6, 8);
		return year + "-" + month + "-" + day;
	}
	
	public static Report getReport(Job job) throws Exception {
		final String nameReport = job.getName();
		final String profileId = job.getGoogleAnalyticsProfile().getId().toString();
		
		GoogleCredential credential = ApplicationSecurity.getGoogleCredential(profileId);
		
		return new Report( nameReport, GoogleAnalyticsDataManager.getReport(credential, job), 
				GoogleAnalyticsDataManager.getMetrics(credential), GoogleAnalyticsDataManager.getDimensions(credential));
	}
}