
package com.mysite.core.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo for cron-job like tasks that get executed regularly. It also
 * demonstrates how property values can be set. Users can set the property
 * values in /system/console/configMgr
 * 
 * @param <GenerateReportServlet>
 */
@Designate(ocd = SchedulerAssetReport.Config.class)
@Component(service = Runnable.class)
public class SchedulerAssetReport<GenerateReportServlet> implements Runnable {

	@Reference
	private Scheduler scheduler;

	String FOLDER_SIZE_AND_STRENGTH_REPORT = "foldersizeandstrengthreport";
	String FOLDER_CONTENT_REPORT = "foldercontentreport";
	String LINK_SHARE_REPORT = "linksharereport";
	String DOWNLOAD_REPORT = "assetdownloadreport";
	String SMART_TAGS_TRAINING_REPORT = "smarttagstrainingreport";

	private static final String DATERANGE_LOWER_BOUND = "0_daterange.lowerBound";
	private static final String DATERANGE_LOWER_OPERATION = "0_daterange.lowerOperation";
	private static final String DATERANGE_UPPER_BOUND = "0_daterange.upperBound";
	private static final String DATERANGE_UPPER_OPERATION = "0_daterange.upperOperation";
	private static final String DATERANGE_PROPERTY = "0_daterange.property";
	private static final String requestPath = "http://localhost:4502/libs/dam/gui/content/reports/generatereport.export.json";
	private static final String reportTypeSuffix = "/libs/dam/content/reports/availablereports/";

	@ObjectClassDefinition(name = "SchedulerAssetReport for Asset Report", description = "This scheduler let you control your asset report concurrency")
	public static @interface Config {

		/*
		 * Define paramenters which are needed to pass through scheduler, for now we've
		 * JOB IDs which should match with existing job ids at /var/dam/report Also,
		 * cron expression and checkboxes for enabling the scheduler and making it
		 * concurrent
		 * 
		 */

		@AttributeDefinition(name = "Enabled", description = "True, if scheduler service is enabled", type = AttributeType.BOOLEAN)
		public boolean enabled() default false;

		@AttributeDefinition(name = "Cron-job expression")
		String scheduler_expression() default "* * * * * ?";

		@AttributeDefinition(name = "Concurrent task", description = "Whether or not to schedule this task concurrently")
		boolean scheduler_concurrent() default false;

		@AttributeDefinition(name = "Job ID", description = "Can be configured in /system/console/configMgr")
		String[] jobIds() default "";
	}

	@Reference
	ResourceResolverFactory factory;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private boolean concurrent;

	private String scheduler_expression;

	private String[] jobIds;
	
	private static final Map<String, String> reporttypeDate = new HashMap<String, String>() {private static final long serialVersionUID = 1L;

	{
        put("assetadditionreport", "jcr:created");
        put("assetpublishreport", "jcr:content/cq:lastReplicated");
        put("assetmodificationreport", "jcr:content/jcr:lastModified");
        put("assetdownloadreport", "jcr:created");
    }};

	public void run() {

		int CONNECTION_TIMEOUT_MS = 10 * 1000; // Timeout in millis.

		// EStablishing a connection to GenerateReportServlet at individual server,
		// please follow AssetReportingIT.java as standard practice

		String url = requestPath + reportTypeSuffix;
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
				.setConnectTimeout(CONNECTION_TIMEOUT_MS).setSocketTimeout(CONNECTION_TIMEOUT_MS).build();
		CloseableHttpClient httpClient = HttpClients.createDefault();

		// Preparing post call and respective parameters and configurations

		
		final Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "writeruser");

		try (ResourceResolver resolver = factory.getServiceResourceResolver(authInfo)) {
			Session session = resolver.adaptTo(Session.class);
			for (int i = 0; i < jobIds.length; i++) {
				Node node = session.getNode("/var/dam/reports/" + jobIds[i]);
				logger.info("inside the servlet ", jobIds[i]);
				if (node != null) {
					String jobTitle = node.getProperty("jobTitle").getString()+"-ScheduledReport";
					String type = node.getProperty("reportType").getString();
					String desc = node.getProperty("jobDescription").getString();
					String path = node.getProperty("path").getString();
					String startDate = node.getProperty("startDate").getString();
					String endDate = node.getProperty("endDate").getString();
					String reportDate = reporttypeDate.get(type).toString();
					String encoding = Base64.getEncoder().encodeToString(("admin" + ":" + "admin").getBytes());
					HttpPost postRequest = new HttpPost(url + type);
					postRequest.setConfig(requestConfig);
					postRequest.addHeader(HttpHeaders.AUTHORIZATION, "basic " + encoding);
					Property propVal = node.getProperty("reportColumns");     
			        Value[] values = propVal.getValues();

					// Building form entity for the POST request
					FormEntityBuilder builder = FormEntityBuilder.create()
							.addParameter("jobTitle", jobTitle)
							.addParameter("dam-asset-report-type", type)
							.addParameter("charset_", "UTF-8")
							.addParameter("wizard", "/mnt/overlay/dam/gui/content/reports/steps/assetreport.html")
							.addParameter("reporttype", type)
							.addParameter("reportdesc", desc)
							.addParameter("jobDescription", "Reports Created by Scheduler")
							.addParameter("path", path)
							.addParameter("createdUserID", "admin")
							.addParameter("reportSchedule", "now")
							.addParameter(DATERANGE_LOWER_BOUND, startDate)
							.addParameter(DATERANGE_UPPER_BOUND, endDate)
							.addParameter(DATERANGE_LOWER_OPERATION, "'>='")
							.addParameter(DATERANGE_UPPER_OPERATION, "'<='")
							.addParameter(DATERANGE_PROPERTY, reportDate)
							.addParameter("mainasset", "true")
							.addParameter("p.guessTotal", "100")
							.addParameter("p.limit", "100")
							.addParameter("type", "dam:Asset");

					for (Value column : values) {
						builder.addParameter("column", column.getString());
					}
					HttpEntity entity = builder.build();
					postRequest.setEntity(entity);
					try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
						int statusCode = response.getStatusLine().getStatusCode();
						logger.info("Response status: {}", statusCode);
						if (statusCode == 200 || statusCode == 201) {
							String responseBody = EntityUtils.toString(response.getEntity());
							logger.info("Response body: {}", responseBody);
						} else {
							logger.error("Request failed with status: {}", statusCode);
						}
					}

				}
			}
		} catch (UnsupportedEncodingException ex) {
			logger.error("UnsupportedEncodingException", ex);
		} catch (IOException ex) {
			logger.error("IOException", ex);
		} catch (ValueFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PathNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LoginException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Activate
	protected void activate(final SchedulerAssetReport.Config config) {
		/*
		 * Try to extend it in a way that based on job id you look for the needed
		 * parameters and pass it from here instead of current hardcoded values in run()
		 * method
		 */
		jobIds = config.jobIds();
		scheduler_expression = config.scheduler_expression();
		concurrent = config.scheduler_concurrent();
	}

	@Modified
	protected void modified(SchedulerAssetReport.Config config) {

		/**
		 * Removing the scheduler
		 */
		removeScheduler();

		/**
		 * Updating the params
		 */
		jobIds = config.jobIds();
		scheduler_expression = config.scheduler_expression();
		concurrent = config.scheduler_concurrent();

		/**
		 * Again adding the scheduler
		 */
		addScheduler(config);
	}

	/**
	 * This method deactivates the scheduler and removes it
	 * 
	 * @param config
	 */
	@Deactivate
	protected void deactivate(SchedulerAssetReport.Config config) {

		/**
		 * Removing the scheduler
		 */
		removeScheduler();
	}

	/**
	 * This method removes the scheduler
	 */
	private void removeScheduler() {
		scheduler.unschedule(String.valueOf(jobIds[0]));
	}

	/**
	 * This method adds the scheduler
	 * 
	 * @param config
	 */
	private void addScheduler(SchedulerAssetReport.Config config) {

		/**
		 * Check if the scheduler is enabled
		 */
		if (config.enabled()) {

			/**
			 * Scheduler option takes the cron expression as a parameter and run accordingly
			 */
			ScheduleOptions scheduleOptions = scheduler.EXPR(config.scheduler_expression());

			/**
			 * Adding some parameters
			 */
			scheduleOptions.name(config.jobIds()[0]);
			scheduleOptions.canRunConcurrently(concurrent);

			/**
			 * Scheduling the job
			 */
			scheduler.schedule(this, scheduleOptions);

			logger.info("Scheduler added");

		} else {

			logger.info("Scheduler is disabled");

		}
	}

}
