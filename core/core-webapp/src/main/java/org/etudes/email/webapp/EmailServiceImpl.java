/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/email/webapp/EmailServiceImpl.java $
 * $Id: EmailServiceImpl.java 8439 2014-08-06 21:52:36Z ggolden $
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

package org.etudes.email.webapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.email.api.EmailService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.user.api.User;

public class EmailServiceImpl implements EmailService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(EmailServiceImpl.class);

	/** The from address for all email. */
	protected String from = "<no-reply@myetudes.org>";

	/** The email server host. */
	protected String host = "localhost";

	/** The email server port. */
	protected String port = "25";

	/** Test mode: where mail is logged, not sent. */
	protected boolean testMode = true;

	public EmailServiceImpl()
	{
		M_log.info("EmailServiceImpl: construct");

		// get configured once all services are started
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				host = configService().getString("EmailService.host", "localhost");
				port = configService().getString("EmailService.port", "25");
				testMode = configService().getBoolean("EmailService.test", false);
				from = configService().getString("EmailService.from", "<no-reply@myetudes.org>");
				M_log.info("EmailServiceImpl: host: " + host + " port: " + port + " from: " + from + " test: " + Boolean.toString(testMode));
			}
		});
	}

	@Override
	public void send(String textMessage, String htmlMessage, String subject, List<User> toUsers)
	{
		if (this.testMode)
		{
			sendTest(textMessage, htmlMessage, subject, toUsers);
		}
		else
		{
			sendEmail(textMessage, htmlMessage, subject, toUsers);
		}
	}

	@Override
	public boolean start()
	{
		M_log.info("EmailServiceImpl: start");
		return true;
	}

	protected void sendEmail(String textMessage, String htmlMessage, String subject, List<User> toUsers)
	{
		try
		{
			// to avoid Caused by: javax.activation.UnsupportedDataTypeException: no object DCH for MIME type multipart/mixed; 
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

			Properties props = new Properties();
			props.setProperty("mail.smtp.host", this.host);
			props.setProperty("mail.smtp.port", this.port);
			Session session = Session.getDefaultInstance(props);

			MimeMessage message = new MimeMessage(session);

			// the from
			message.setFrom(new InternetAddress(this.from));

			// the to
			List<InternetAddress> sendTo = sendTo(toUsers);
			for (InternetAddress a : sendTo)
			{
				message.addRecipient(Message.RecipientType.TO, a);
			}

			// subject
			message.setSubject(subject);

			// multipart, text and html
			Multipart body = new MimeMultipart("alternative");

			// the text part
			BodyPart textPart = new MimeBodyPart();
			textPart.setText(textMessage);

			// the html part
			BodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(htmlMessage, "text/html");

			// put it all together
			body.addBodyPart(textPart);
			body.addBodyPart(htmlPart);
			message.setContent(body);

			// send
			Transport.send(message);
		}
		catch (MessagingException e)
		{
			M_log.warn("sendEmail: ", e);
		}
	}

	protected void sendTest(String textMessage, String htmlMessage, String subject, List<User> toUsers)
	{
		List<InternetAddress> sendTo = sendTo(toUsers);
		M_log.info("sendTest: subject: " + subject + " To: " + sendTo + " text:\n" + textMessage + "\nhtml:\n" + htmlMessage + "\n");
	}

	/**
	 * Select the email addresses to send to based on the users list and their settings.
	 * 
	 * @param toUsers
	 *        The users list.
	 * @return A List of InternetAddress for each recipient of the email.
	 */
	protected List<InternetAddress> sendTo(List<User> toUsers)
	{
		List<InternetAddress> rv = new ArrayList<InternetAddress>();

		// the to, all the user who have a usable email address
		for (User u : toUsers)
		{
			String email = u.getEmailUser();
			if (email == null) email = u.getEmailOfficial();

			// TODO: other preferences to check? Official and user email?
			if (email != null)
			{
				try
				{
					rv.add(new InternetAddress(email));
				}
				catch (MessagingException e)
				{
					M_log.warn("sendTo: unable to use email address: " + email);
				}
			}
		}

		return rv;
	}

	protected void test()
	{
		// Recipient's email ID needs to be mentioned.
		String to = "abcd@gmail.com";

		// Sender's email ID needs to be mentioned
		String from = "web@gmail.com";

		// Assuming you are sending email from localhost
		String host = "localhost";

		// Get system properties
		Properties properties = System.getProperties();
		// TODO: maybe just new Properties()

		// Setup mail server
		properties.setProperty("mail.smtp.host", host);
		// TODO: diff between setProperty and put?

		// Get the default Session object.
		Session session = Session.getDefaultInstance(properties);
		// TODO: diff between getDefaultInstance and getInstance()?

		try
		{
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

			// Set Subject: header field
			message.setSubject("This is the Subject Line!");

			// TODO: content type? content encoding?

			// Now set the actual message
			message.setText("This is actual message");

			// TODO: Send the actual HTML message, as big as you like
			message.setContent("<h1>This is actual message</h1>", "text/html");

			// TODO: for attachment
			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();

			// Fill the message
			messageBodyPart.setText("This is message body");

			// Create a multipart message
			Multipart multipart = new MimeMultipart();

			// Set text message part
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachment
			messageBodyPart = new MimeBodyPart();
			String filename = "file.txt";
			DataSource source = new FileDataSource(filename);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(filename);
			multipart.addBodyPart(messageBodyPart);

			// Send the complete message parts
			message.setContent(multipart);

			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");
		}
		catch (MessagingException mex)
		{
			mex.printStackTrace();
		}
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}
}
