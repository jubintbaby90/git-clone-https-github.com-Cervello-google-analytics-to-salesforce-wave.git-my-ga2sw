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
package com.ga2sa.scheduler;

import play.libs.Akka;
import akka.actor.ActorRef;
import akka.actor.Props;
/**
 * 
 * Class for manage scheduler
 * 
 * @author Igor Ivarov
 * @editor Sergey Legostaev
 * 
 */
public class Scheduler {
	
	private static ActorRef scheduler = Akka.system().actorOf(Props.create(SchedulerManager.class));
	
	/**
	 * Get current scheduler actorref
	 * @return
	 */
	public static ActorRef getInstance() {
		return scheduler;
	}
}