package com.mysite.core.servlets;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;

import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.osgi.framework.Constants;
import java.util.*;

@Component(service = Servlet.class, property = {
        Constants.SERVICE_DESCRIPTION + "= Asset List for Expiration",
        "sling.servlet.resourceTypes=" + "nikitagargprogram/components/utility/assetlistdatasource"
})
public class AssetListDatasource extends SlingSafeMethodsServlet {

    private static Logger LOGGER = LoggerFactory.getLogger(AssetListDatasource.class);

    @Reference
    ExpressionResolver expressionResolver;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource resource = request.getResource();

            Config dsCfg = new Config(resource.getChild(Config.DATASOURCE));
            ExpressionHelper ex = new ExpressionHelper(expressionResolver, request);
            //Default Values
            String path = "/content/dam";
            String limit = "40";
            String offset = "0";

            if(ex!=null){
                String assetPath = ex.getString(dsCfg.get("assetPath",String.class));
                
                if(StringUtils.isNotBlank(assetPath)){
                    path = assetPath;
                }
                limit = ex.getString(dsCfg.get("limit",String.class));
                offset = ex.getString(dsCfg.get("offset",String.class)) !=null ? ex.getString(dsCfg.get("offset",String.class)) : "0";
                LOGGER.debug("limit {} offset {} ", limit , offset);
            }

            String itemResourceType = dsCfg.get("itemResourceType", String.class);

            LOGGER.debug("path {} itemResource type {} ", path, itemResourceType);

            if (StringUtils.isBlank(path)) {
                LOGGER.debug("Inside blank path , thereby setting the Empty datasource");
                DataSource ds = EmptyDataSource.instance();
                request.setAttribute(DataSource.class.getName(), ds);
            } else {

                LOGGER.debug("Inside path {} , thereby setting the Abstract datasource ", path);
                //Resource rootResource = resourceResolver.getResource(path);

                SearchResult searchResult = getFilteredAssets(resourceResolver, path, limit, offset);

                List<Resource> unrefList = findUnreferencedNodes(resourceResolver, searchResult.getResources());

                
                final Iterator<Resource> resourceIterator = unrefList.iterator();

                DataSource datasource = new AbstractDataSource() {
                    @Override
                    public Iterator<Resource> iterator() {
                        return new TransformIterator<>(resourceIterator, new Transformer<Resource, Resource>() {
                            @Override
                            public Resource transform(Resource resource) {
                                return new ResourceWrapper(resource) {
                                    @Override
                                    public String getResourceType() {
                                        return itemResourceType; // Change to the desired resource type
                                    }
                                };
                            }
                        });
                    }
                };

                request.setAttribute(DataSource.class.getName(), datasource);
            }

        } catch (Exception e) {
            LOGGER.error("Error while getting Unreferenced assets", e);
        }
    }

    private SearchResult getFilteredAssets(ResourceResolver resourceResolver, String path, String limit, String offset) {
        Map<String, String> map = new HashMap<String,String>();
        map.put("type", "dam:Asset");
        map.put("path", path);
        
        // Exclude content fragments
        map.put("1_property", "jcr:content/contentFragment");
        map.put("1_property.operation", "exists");
        map.put("1_property.value", "false");
        
        map.put("p.limit", limit);
        map.put("p.offset", offset);

        Session session = resourceResolver.adaptTo(Session.class);
        QueryBuilder builder =resourceResolver.adaptTo(QueryBuilder.class);
        Query query = builder.createQuery(PredicateGroup.create(map), session);

        SearchResult searchResult = query.getResult();
        LOGGER.debug("Search Result query is {}" , searchResult.getQueryStatement());
        return searchResult;
    }

    private List<Resource> findUnreferencedNodes(ResourceResolver resourceResolver, Iterator<Resource> resources) {
        List<Resource> resourceList = new ArrayList<>();
        String nodePath = "/content";
        Node node = resourceResolver.getResource(nodePath).adaptTo(Node.class);

        while (resources.hasNext()) {
            Resource currentRes = resources.next();
            if (DamUtil.isAsset(currentRes)) {
                String assetPath = currentRes.getPath();
                AssetReferenceSearch assetReferenceSearch = new AssetReferenceSearch(node, assetPath, resourceResolver);
                Map<String, Asset> references = assetReferenceSearch.search();
                if ( references.isEmpty()) {
                    // These are the nodes/assets not referenced anywhere
                    resourceList.add(currentRes);
                }
            }
        }
        LOGGER.info("Unreferenced Assetss: {}",resourceList.toString() );
        return resourceList;
    }   
}
