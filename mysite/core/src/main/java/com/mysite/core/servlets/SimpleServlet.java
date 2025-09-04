/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mysite.core.servlets;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.methods=" + "get",
                "sling.servlet.paths=" + "/bin/myreportservlet",
        }
)
public class SimpleServlet<GenerateReportServlet> extends SlingAllMethodsServlet {
    
	private static final String DATERANGE_LOWER_BOUND = "0_daterange.lowerBound";
	private static final String DATERANGE_LOWER_OPERATION = "0_daterange.lowerOperation";
	private static final String DATERANGE_UPPER_BOUND = "0_daterange.upperBound";
	private static final String DATERANGE_UPPER_OPERATION = "0_daterange.upperOperation";
	private static final String DATERANGE_PROPERTY = "0_daterange.property";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final long serialVersionUID = 1L;
    
    private static final Map<String, String> reporttypeDate = new HashMap<String, String>() {private static final long serialVersionUID = 1L;

	{
        put("assetadditionreport", "jcr:created");
        put("assetpublishreport", "jcr:content/cq:lastReplicated");
        put("assetmodificationreport", "jcr:content/jcr:lastModified");
        put("assetdownloadreport", "jcr:created");
    }};
    protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp) throws ServletException, IOException {
        final String jobIds = "/var/dam/reports/"+req.getParameter("id");
        logger.info("SchedulerAssetReportServlet is now running, jobId='{}'", jobIds);
        
        int CONNECTION_TIMEOUT_MS = 10 * 1000; // Timeout in millis.
        
        // URL for report generation
        String requestPath = "http://localhost:4502/libs/dam/gui/content/reports/generatereport.export.json";
        String reportTypeSuffix = "/libs/dam/content/reports/availablereports/";
        String url = requestPath + reportTypeSuffix;
        
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MS).setSocketTimeout(CONNECTION_TIMEOUT_MS).build();
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ResourceResolver resourceResolver = req.getResourceResolver();
            Resource resource = resourceResolver.getResource(jobIds);
			if(resource != null) {
				Node node = resource.adaptTo(Node.class);
				String jobTitle = node.getProperty("jobTitle").getString();
				String type = node.getProperty("reportType").getString();
				String desc = node.getProperty("jobDescription").getString();
				String path = node.getProperty("path").getString();
				String startDate = node.getProperty("startDate").getString();
				String endDate = node.getProperty("endDate").getString();
				String reportDate = reporttypeDate.get(type).toString();
	            String encoding = Base64.getEncoder().encodeToString(("admin" + ":" + "admin").getBytes());
	            HttpPost postRequest = new HttpPost(url+type);
	            postRequest.setConfig(requestConfig);
	            postRequest.addHeader(HttpHeaders.AUTHORIZATION, "basic " + encoding);
	            
	            String[] columns = { "title", "path", "type", "size", "added" };
	            
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
	                    .addParameter(DATERANGE_LOWER_BOUND,startDate)
	                    .addParameter(DATERANGE_UPPER_BOUND, endDate)
		                .addParameter(DATERANGE_LOWER_OPERATION, "'>='")
		                .addParameter(DATERANGE_UPPER_OPERATION, "'<='")
		                .addParameter(DATERANGE_PROPERTY, reportDate)
	                    .addParameter("mainasset", "true")
	                    .addParameter("p.guessTotal", "100")
	                    .addParameter("p.limit", "100")
	                    .addParameter("type", "dam:Asset");
	            
	            if(type.equalsIgnoreCase("assetdownloadreport")) {
	            	builder.addParameter("1_property", "verb");
	            	builder.addParameter("1_property.value", "DOWNLOADED");
	            }
	            for (String column : columns) {
	                builder.addParameter("column", column);
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
		}
        
        resp.getWriter().write("Report xyz generation triggered for jobId=" + jobIds);
    }
}
