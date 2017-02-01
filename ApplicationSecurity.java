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
package com.ga2sa.security;

import java.util.UUID;

import models.GoogleAnalyticsProfile;
import models.Session;
import models.User;
import models.UserGroup;
import models.dao.GoogleAnalyticsProfileDAO;
import models.dao.SessionDAO;
import models.dao.UserDAO;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.core.j.HttpExecutionContext;
import play.libs.HttpExecution;
import play.mvc.Http;

import com.ga2sa.google.GoogleConnector;
import com.ga2sa.helpers.forms.LoginForm;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import controllers.SessionManager;
import controllers.routes;
/**
 * 
 * Class for manage application security. Responsible for authentication and authorization users. 
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 *
 */
public class ApplicationSecurity {
	
	
	public static final String SESSION_ID_KEY = "session_id";
	
	/**
	 * Method for generation of redirect url for Google Analytics profile.
	 * 
	 * @return url for GA profile
	 */
	public static String getRedirectURL() {
		return routes.Authorization.googleSignIn().absoluteURL(Play.isProd(), Http.Context.current()._requestHeader());
	}
	
	/**
	 * Method for authenticate user in the application.
	 * 
	 * @param loginForm from Login page
	 * @return true or false
	 */
	public static Boolean authenticate(LoginForm loginForm) {
		User user = UserDAO.getUserByUsername(loginForm.getUsername());
		if (user == null) {
			Logger.error("User not found ");
		} else {
			if (user.isActive) {
				if (PasswordManager.checkPassword(loginForm.getPassword(), user.password)) {				
					final String sessionId = UUID.randomUUID().toString();
					SessionManager.set(SESSION_ID_KEY, sessionId);
//					CookieManager.set(SESSION_ID_KEY, sessionId, true);
					try {
//						user.lastLoginDateTime = new Date();
//						UserDAO.update(user);
						SessionDAO.save(new Session(sessionId, user.id));
					} catch (Exception e) {
						e.printStackTrace();
					}
					return true;
				} else {
					Logger.debug("Password is not correct.");
				}
			}
		}
		return false;
	}
	
	/**
	 * Method for getting session id from Application session
	 * 
	 * 
	 * @return true or false
	 */
	public static String getSessionId() {
//		Cookie cookie = CookieManager.get(SESSION_ID_KEY);
//		return cookie == null ? null : cookie.value();
		return SessionManager.get(SESSION_ID_KEY);
	}
	
	/**
	 * Method for getting Session object from database for current session
	 * 
	 * @return Session
	 */
	public static Session getCurrentSession() {
		return SessionDAO.getSession(getSessionId());
	}
	
	/**
	 * Get current user that loggined in the application
	 * 
	 * @return User
	 */
	public static User getCurrentUser() {
	
		final String sessionId = getSessionId();
		Session session = getCurrentSession();
		
		if (session == null && sessionId == null) {
			Logger.debug("User is not loggined.");
			return null;
		}
		
		return session == null ? null : UserDAO.getUserById(session.getUserId());
	}
	
	/**
	 * Check admin role for current user
	 * 
	 * @return true or false
	 */
	public static boolean isAdmin() {
		User user = getCurrentUser();
		return user == null ? false : user.role.equals(UserGroup.ADMIN);
	}
	
	/**
	 * Method for logout from application
	 */
	public static void logout() {
//		Cookie cookie = CookieManager.get(SESSION_ID_KEY);
//		if (cookie != null && cookie.value() != null) {
//			SessionDAO.deleteById(cookie.value());
//			CookieManager.remove(SESSION_ID_KEY, true);
//		}
		final String sessionId = getSessionId();
		if (sessionId != null) {
			SessionDAO.deleteById(sessionId);
			SessionManager.clear();
		}
	}
	
	/**
	 * Get Google Credential from cache for current user
	 * 
	 * @param Google Analytics Profile Id from database
	 * 
	 * @return Google Credential from Google Analytics Profile
	 */
	public static GoogleCredential getGoogleCredential(String profileId) {
		final String cacheId = GoogleConnector.CACHE_CREDENTIAL_PREFIX + profileId;
		GoogleCredential credential = (GoogleCredential)Cache.get(cacheId);
		if (credential == null) {
			GoogleAnalyticsProfile profile = GoogleAnalyticsProfileDAO.getProfileById(Long.valueOf(profileId));
			credential = GoogleConnector.getCredentials(profile);
			Cache.set(cacheId, credential);
		}
		return credential;
	}
}