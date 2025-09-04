package com.mysite.core.servlets;

import javax.servlet.Servlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.AssetMetadata;

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
@SlingServletResourceTypes(resourceTypes = "/apps/nikitagargprogram/expireSelectedAssets", methods = HttpConstants.METHOD_POST, extensions = "json", selectors = "archive")
@ServiceDescription("Archive Asset servlet")
public class ArchiveAssets extends SlingAllMethodsServlet {

    private static Logger LOGGER = LoggerFactory.getLogger(ArchiveAssets.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        LOGGER.debug("************POST Servlet called **********");

        String folderPath = request.getParameter("archivePath");

        String[] paths = request.getParameterValues("path");

        ResourceResolver resourceResolver = request.getResourceResolver();

        Resource archiveFolder = resourceResolver.getResource(folderPath);
        AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

        if (assetManager == null) {
            LOGGER.error("Failed to get AssetManager from ResourceResolver");
            return;
        }

        if (archiveFolder == null) {
            LOGGER.error("Archive folder not found at path: {}", folderPath);
            return;
        }

        for (String path : paths) {

            if (assetManager.assetExists(path)) {

                Asset asset = assetManager.getAsset(path);
              

                try {
                    String assetName = asset.getName();
                    String newAssetPath = folderPath + "/" + assetName;

                    assetManager.moveAsset(path, newAssetPath);

                    Resource metadataResource = resourceResolver.getResource(newAssetPath+"/jcr:content/metadata");
                    if (metadataResource != null) {
                        ModifiableValueMap properties = metadataResource.adaptTo(ModifiableValueMap.class);
                        if (properties != null) {
                            // Update the desired metadata properties
                            properties.put("originalPath", path);
    
                            LOGGER.debug("Metadata updated for path: {}", path);
                        } else {
                            LOGGER.error("Failed to adapt metadata resource to ModifiableValueMap for path: {}", path);
                        }
                    }
                    LOGGER.debug("Moved asset from {} to {}", path, newAssetPath);
                } catch (Exception e) {
                    LOGGER.error("Failed to move asset from {} to {}", path, folderPath, e);
                }
            }else{
                LOGGER.error("Asset not found at path: {}", path);
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
