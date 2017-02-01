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

package com.ga2sa.utils;

import java.util.Collection;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * 
 * Class for work with json
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 */
public class JsonUtil {
	
	/**
	 * Method for exclude fields from json object, it's need to use separate logic for client side and server side.
	 * @param source json object
	 * @param excluded fields 
	 * @return json object with excluded fields
	 */
	public static JsonNode excludeFields(JsonNode source, Collection<String> fields) {
		
		JsonNode result = null;
		
		if (source.isArray()) {
			result = new ObjectMapper(new JsonFactory()).createArrayNode();
			for (JsonNode node : source) {
				((ArrayNode)result).add(((ObjectNode)node).without(fields));
			}
		} else {
			result = ((ObjectNode)source).without(fields);
		}
		
		return result;
	}
}