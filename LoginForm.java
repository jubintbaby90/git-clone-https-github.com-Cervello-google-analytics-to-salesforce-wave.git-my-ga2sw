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

package com.ga2sa.helpers.forms;
/**
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 */
public class LoginForm {
	
	private String username;
	private String password;
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String validate() {
		if (username == null || username.isEmpty()) {
			return "Username is empty.";
		}
		
		if (password == null || password.isEmpty()) {
			return "Password is empty.";
		}
		
		return null;
	}

}