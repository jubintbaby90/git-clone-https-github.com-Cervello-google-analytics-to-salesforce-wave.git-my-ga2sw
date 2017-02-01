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

import models.User;
import models.UserGroup;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * 
 * Action class for handle requests to page. If user has access to page then request will be allowed, if user has not access then will be returned error code.
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 *
 */

public class AccessAction extends Action<Access> {
	
	/**
	 * Method is executed every time when try to open a page of application, if a user has not access to the page the user will get error message. 
	 * If user was not authorized this request will be redirected to login page. 
	 */

	@Override
	public Promise<Result> call(Context ctx) throws Throwable {
		User user = ApplicationSecurity.getCurrentUser();
		if (user == null) return F.Promise.pure((Result) redirect(controllers.routes.Authorization.ga2saSignIn()));
		if (user.role.equals(UserGroup.ADMIN)) return delegate.call(ctx);
		
		UserGroup userGroup = configuration.allowFor();
		
		if (userGroup.equals(UserGroup.ADMIN)) {
			ObjectNode result = Json.newObject();
			result.put("error", 401);
			result.put("message", "You don't have permissions");
			return F.Promise.pure((Result) unauthorized(result));
		}
		
		return delegate.call(ctx);
	}

}