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

import org.mindrot.jbcrypt.BCrypt;

import play.Logger;
/**
 * 
 * Util class for work with password, the one uses for encrypt and check user password 
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 * 
 */
public class PasswordManager {
	
	public static final String PASSWORD_TMP = "password";
	
	/**
	 * Method for compare plain password and encrypted, this method uses for check password when user try to login.
	 * 
	 * @param plainPassword
	 * @param encryptedPassword
	 * @return true or false
	 */
	public static boolean checkPassword(String plainPassword, String encryptedPassword) {
		try {
			return BCrypt.checkpw(plainPassword, encryptedPassword);
		} catch (IllegalArgumentException e) {
			Logger.error("Check password error: ", e) ;
		}
		return false;
	}
	
	/**
	 * Encrypt password use default setting for BCrypt
	 * 
	 * @param plain password
	 * @return encrypted password
	 */
	
	public static String encryptPassword(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

}