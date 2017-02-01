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
import java.util.Collections;

import models.GoogleAnalyticsProfile;
import models.dao.GoogleAnalyticsProfileDAO;

import com.ga2sa.security.ApplicationSecurity;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.analytics.AnalyticsScopes;
/**
 * 
 * Class for manage connections to GA
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 */
public class GoogleConnector {
	
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	public static final String CACHE_CREDENTIAL_PREFIX = "cache_credential_";
	
	
	/**
	 * Method for selecting redirection url, if application was started on local pc will be used first url from list.
	 * 
	 * @param GA profile
	 * @return
	 */
	private static GoogleAuthorizationCodeFlow getFlow(GoogleAnalyticsProfile profile)  {
		return new GoogleAuthorizationCodeFlow
			.Builder(HTTP_TRANSPORT, JSON_FACTORY, profile.getClientId(), profile.getClientSecret(), Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
			.setAccessType("offline")
			.setApprovalPrompt("force")
			.build();
	}
	
	/**
	 * Get redirect url from GA profile
	 * 
	 * @param GA profile
	 * @return redirect url
	 */
	public static String getAuthURL(GoogleAnalyticsProfile profile) {
		return getFlow(profile).newAuthorizationUrl().setRedirectUri(ApplicationSecurity.getRedirectURL()).toURI().toString();
	}
	
	/**
	 * Execute code from redirected url that was got from GA profile 
	 * @param profile
	 * @param authorizationCode
	 */
	public static void exchangeCode(GoogleAnalyticsProfile profile, String authorizationCode)  {
		
		try {
			GoogleAuthorizationCodeFlow flow = getFlow(profile);
			GoogleTokenResponse response = flow.newTokenRequest(authorizationCode).setRedirectUri(ApplicationSecurity.getRedirectURL()).execute();
			storeCredentials(profile, flow.createAndStoreCredential(response, null));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update GA profile in database, changed connected flag to true value
	 * @param profile
	 * @param credential
	 */
	private static void storeCredentials(GoogleAnalyticsProfile profile, Credential credential) {
		
		profile.setAccessToken(credential.getAccessToken());
		profile.setRefreshToken(credential.getRefreshToken());
		profile.setConnected(true);
		
		try {
			GoogleAnalyticsProfileDAO.update(profile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get Google Credential for GA profile
	 * @param GA profile
	 * @return Google Credential
	 */
	public static GoogleCredential getCredentials(GoogleAnalyticsProfile profile) {
		GoogleCredential credential = new GoogleCredential.Builder()
	    	.setTransport(HTTP_TRANSPORT)
	    	.setJsonFactory(JSON_FACTORY)
	    	.setClientSecrets(profile.getClientId(), profile.getClientSecret())
	    	.build()
			.setAccessToken(profile.getAccessToken())
			.setRefreshToken(profile.getRefreshToken());
		
		return credential;
	}
	
}