/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/email/api/EmailService.java $
 * $Id: EmailService.java 8436 2014-08-06 20:55:30Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.email.api;

import java.util.List;

import org.etudes.user.api.User;

public interface EmailService
{
	/**
	 * Send an email to a bunch of users.
	 * 
	 * @param textMessage
	 *        The plain text message.
	 * @param htmlMessage
	 *        The html message.
	 * @param subject
	 *        The subject line.
	 * @param toUsers
	 *        The list of Users to send to.
	 */
	void send(String textMessage, String htmlMessage, String subject, List<User> toUsers);
}
