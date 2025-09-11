package com.mysite.core.servlets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.commons.ReferenceSearch;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(
		service = { Servlet.class },
		property = {
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes=" + "mysite/components/utility/orphaned-assets/_jcr_content/rails/search/items",
        "sling.servlet.selectors=" + "result",
        "sling.servlet.extensions=" + "html"

})
public class FindRefServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	ResourceResolver resourceResolver;
	private String path;
	private ReferenceSearch referenceSearch = new ReferenceSearch();
	private final Logger logger = LoggerFactory.getLogger(FindRefServlet.class);
	
	JsonObject unrefasset;

	@Override
	protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("Inside ref servlet");
		resourceResolver = req.getResourceResolver();
		path = req.getParameter("path");
		if(path.equals(StringUtils.EMPTY)){
			path="/content/dam";
		}
		logger.info("Inside ref servlet and path is" + path);
		unrefasset = new JsonObject();
		try {
			findUnreferencedNodes(path, resourceResolver);
		} catch (Exception e) {
			logger.error("Exception occurred while retrieved data", e);
		}
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		String json = new Gson().toJson(unrefasset);
		resp.getWriter().write(json);
	}

	private void findUnreferencedNodes(String path, ResourceResolver resourceResolver)  {
		this.path = path;

		try {
			Node root = resourceResolver.getResource(path).adaptTo(Node.class);
			NodeIterator nodeIterator = root.getNodes();
			logger.info("Inside Node iterator");

			while (nodeIterator.hasNext()) {
				Node currentNode = nodeIterator.nextNode();
				logger.info("Currrent Node is" + currentNode.getPath());
				if (currentNode.getProperty("jcr:primaryType").getString().equals("dam:Asset")) {
					@SuppressWarnings("deprecation")
					Map<String, ReferenceSearch.Info> searchResult = referenceSearch.search(resourceResolver,
							currentNode.getPath());
					if (searchResult.isEmpty()) {
						// These are the nodes/assets not referenced anywhere
						logger.info("Unreferenced Asset: " + currentNode.getPath());
						/* Code to set expiration date 
						 * Node metadata = currentNode.getNode("jcr:content/metadata");
						 * if(!metadata.hasProperty("prism:expirationDate")) { logger.info("time: " +
						 * getCurrentDateString()); metadata.setProperty("prism:expirationDate",
						 * getCurrentDateString()); metadata.save(); }
						 */
						unrefasset.addProperty(currentNode.getName(), currentNode.getPath());
					}
				}
				// Recursively iterate through all the assets from root path
				findUnreferencedNodes(currentNode.getPath(), resourceResolver);
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
	
	
	private static String getCurrentDateString() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String currentDateString = dateFormat.format(currentDate);
        return currentDateString;
    }
}
