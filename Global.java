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

import java.util.TimeZone;

import models.User;
import models.UserGroup;
import models.dao.UserDAO;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import akka.actor.ActorRef;

import com.ga2sa.scheduler.Scheduler;
import com.ga2sa.security.PasswordManager;

/**
 * Load global settings and initialization of scheduled jobs 
 * @Author Igor Uvarov
 * @Editor Sergey Legostaev
 **/

public class Global extends GlobalSettings {
	/**
	 * Method for creation default admin user, user will be created if admin does not exist yet. 
	 * This method was implemented for fast deploy to Heroku instance (Heroku Button)
	 */
	private void createDefaultAdmin() {
		Logger.info("Creation default user.");
		User user = UserDAO.getUserByUsername("admin");
		if (user == null) {
			user = new User();
			user.username = "admin";
			user.firstName = "admin";
			user.lastName = "admin";
			user.emailAddress = "ga2sa@mycervello.com";
			user.isActive = true;
			user.password = PasswordManager.encryptPassword("cervello");
			user.role = UserGroup.ADMIN;
			try {
				UserDAO.save(user);
				Logger.info("Default user has been created.");
			} catch (Exception e) {
				Logger.error("Default user has not been created.", e.getMessage());
			}
		} else {
			Logger.info("Default user already exists.");
		}
	}
	
	/**
	 * Method for change application settings. On start application will be update default time zone, 
	 * create default admin user and start background jobs if they exist.
	 */
	
	@Override
	public void onStart(Application app) {
		Logger.info("Application has been started");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		createDefaultAdmin();
		Scheduler.getInstance().tell("start", ActorRef.noSender());
	}
	
}