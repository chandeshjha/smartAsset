package com.mysite.core.servlets;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.Servlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "/apps/mysite/expireSelectedAssets",
     methods = HttpConstants.METHOD_POST, 
     extensions = "json",
      selectors = "update-expiration")
@ServiceDescription("Expiration date servlet")
public class UpdateExpirationDate extends SlingAllMethodsServlet {

    private static Logger LOGGER = LoggerFactory.getLogger(UpdateExpirationDate.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        LOGGER.debug("************POST Servlet called **********");

        String[] paths = request.getParameterValues("path");

        ResourceResolver resourceResolver = request.getResourceResolver();
        String currentDate = getCurrentDateString();
        for (String path : paths) {
            LOGGER.debug("Path: {}", path);

            Resource resource = resourceResolver.getResource(path);
            if (resource == null) {
                LOGGER.error("Resource not found for path: {}", path);
                continue;
            } else {
                LOGGER.debug("Resource found for path: {}", path);
                try {
                    // Get the jcr:content/metadata node of the asset
                    Resource metadataResource = resource.getChild("jcr:content/metadata");
                    if (metadataResource != null) {
                        ModifiableValueMap properties = metadataResource.adaptTo(ModifiableValueMap.class);
                        if (properties != null) {
                            // Update the desired metadata properties
                            properties.put("prism:expirationDate", currentDate);

                            LOGGER.debug("Metadata updated for path: {}", path);
                        } else {
                            LOGGER.error("Failed to adapt metadata resource to ModifiableValueMap for path: {}", path);
                        }
                    } else {
                        LOGGER.error("jcr:content/metadata node not found for path: {}", path);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to update metadata for path: {}", path, e);
                }

               
            }
             // Commit all changes at the end
             try {
                resourceResolver.commit();
                LOGGER.debug("All changes committed successfully.");
            } catch (PersistenceException e) {
                LOGGER.error("Failed to commit changes.", e);
            }
        }

    }

    private static String getCurrentDateString() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String currentDateString = dateFormat.format(currentDate);
        return currentDateString;
    }
}
