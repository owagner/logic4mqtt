package com.tellerulam.logic4mqtt.api;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.*;
import javax.mail.internet.*;

import com.tellerulam.logic4mqtt.*;

/**
 * The Mail class supports sending E-Mail.
 *
 * It utilizes the Java Mail subsystem: https://java.net/projects/javamail/pages/Home
 *
 * Parameters are specified using key=value pairs (or an object in Javascript).
 * Default parameters can be specified using setDefaultParameters() and will
 * be inherited by all subsequent sendMail() calls.
 *
 * The following parameters are supported:
 * <ul>
 * <li>from - sender address
 * <li>to - receiving address
 * <li>host - SMTP relay host (defaults to localhost)
 * <li>user - SMTP server login
 * <li>password - SMTP server password
 * <li>priority - X-Priority value (1..5) (highest to lowest)
 * </ul>
 *
 * Additionally, all of the standard Java Mail API properties are supported:
 * https://javamail.java.net/nonav/docs/api/
 *
 * Since mail sending can take an arbitrary amount of time, sending
 * is asynchronous. This means that network errors or rejects will not be
 * returned by the sendEmail function; they will be logged, however.
 *
 */
public class Mail
{
	static final Mail instance=new Mail();

	private final Properties defaultProperties=new Properties();

	/* Keep private */
	protected Mail()
	{
		// add handlers for main MIME types (this normally comes from mail.jar's META-INF, but fails
		// if we have a fat jar
		MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);
	}

	/**
	 * Set the default parameters for all subsequent sendMail calls.
	 *
	 * @param params Default parameters. See class description for list of supported properties.
	 */
	public void setDefaultParameters(Map<String,Object> params)
	{
		defaultProperties.clear();
		for(Map.Entry<String,Object> me:params.entrySet())
			defaultProperties.put(me.getKey(),me.getValue().toString());
		System.out.println(defaultProperties);
	}

	private final Executor mailExecutor=Executors.newSingleThreadExecutor();

	private Properties mergeAndCheckProperties(Map<String,Object> params)
	{
		Properties p=new Properties(defaultProperties);
		if(params!=null)
		{
			for(Map.Entry<String,Object> me:params.entrySet())
				p.put(me.getKey(),me.getValue().toString());
		}
		if(p.getProperty("to")==null)
			throw new IllegalArgumentException("no to address specified");
		return p;
	}

	static class MailJob implements Runnable
	{
		String subject;
		String body;
		Properties params;
		String attachments[];
		public MailJob(String subject,String body,Properties params,String attachments[])
		{
			this.subject=subject;
			this.body=body;
			this.params=params;
			this.attachments=attachments;
		}

		@Override
		public void run()
		{
			try
			{
				String host=params.getProperty("host");
				if(host!=null)
					params.put("mail.host",host);
				String user=params.getProperty("user");
				String password=params.getProperty("password");
				Session session=Session.getInstance(params,null);
				//session.setDebug(true);
				Message msg=new MimeMessage(session);
				String from=params.getProperty("from");
				if(from!=null)
					msg.setFrom(new InternetAddress(from));
				else
					msg.setFrom();
				String to=params.getProperty("to");
			    msg.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to, false));
			    msg.setSubject(subject);

			    String pri=params.getProperty("priority");
			    if(pri!=null)
			    	msg.addHeader("X-Priority",pri);

			    if(attachments!=null)
			    {
			    	MimeMultipart mp=new MimeMultipart();
			    	MimeBodyPart textpart=new MimeBodyPart();
			    	textpart.setText(body);
			    	mp.addBodyPart(textpart);
			    	for(String attachment:attachments)
			    	{
			    		MimeBodyPart mbp=new MimeBodyPart();
			    		mbp.attachFile(new File(attachment));
			    		mp.addBodyPart(mbp);
			    	}

			    	msg.setContent(mp);
			    }
			    else
			    {
				    msg.setText(body);
			    }

			    msg.setSentDate(new Date());
			    msg.addHeader("X-Mailer", "logic4mqtt "+Main.getVersion());

			    if(user!=null)
			    	Transport.send(msg,user,password);
			    else
			    	Transport.send(msg);
			}
			catch(Exception e)
			{
				L.log(Level.WARNING, "Sending mail ["+subject+"] failed",e);
			}
		}
	}

	/**
	 * Send an email. Sending is asynchronous, so this function will not return transport errors
	 * or other failures.
	 *
	 * @param subject Subject of the mail
	 * @param body Body text of the mail
	 * @param params Additional parameters, overwriting or amending the setDefaultProperties() default properties
	 * @param attachments List of files to attach to the mail
	 */
	public void sendMail(String subject,String body,Map<String,Object> params,String... attachments)
	{
		mailExecutor.execute(new MailJob(subject,body,mergeAndCheckProperties(params),attachments));
	}

	/**
	 * Send an email. Sending is asynchronous, so this function will not return transport errors
	 * or other failures.
	 *
	 * @param subject Subject of the mail
	 * @param body Body text of the mail
	 * @param params Additional parameters, overwriting or amending the setDefaultProperties() default properties
	 */
	public void sendMail(String subject,String body,Map<String,Object> params)
	{
		mailExecutor.execute(new MailJob(subject,body,mergeAndCheckProperties(params),null));
	}

	/**
	 * Send an email. Sending is asynchronous, so this function will not return transport errors
	 * or other failures.
	 *
	 * @param subject Subject of the mail
	 * @param body Body text of the mail
	 */
	public void sendMail(String subject,String body)
	{
		mailExecutor.execute(new MailJob(subject,body,mergeAndCheckProperties(null),null));
	}

	/**
	 * Send an email. Sending is asynchronous, so this function will not return transport errors
	 * or other failures.
	 *
	 * @param subject Subject of the mail
	 * @param body Body text of the mail
	 * @param attachments List of files to attach to the mail
	 */
	public void sendMail(String subject,String body,String... attachments)
	{
		mailExecutor.execute(new MailJob(subject,body,mergeAndCheckProperties(null),attachments));
	}

	private static Logger L=Logger.getLogger(Mail.class.getName());
}
