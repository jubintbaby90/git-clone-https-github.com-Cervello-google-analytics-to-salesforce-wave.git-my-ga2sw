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

/**
 * 
 * Check user access for tabs and pages
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 *
 */
public class CheckAccesTemplates {
	
	/**
	 * Method uses for check access to element on UI for current user.
	 * 
	 * @return true or false
	 */
	public static boolean hasAccess() {
		return ApplicationSecurity.isAdmin();
	}
	
}