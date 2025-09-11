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
package com.mysite.core.models;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.day.cq.dam.api.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Model(adaptables = Resource.class)
public class RowModel {

    @SlingObject
    private Resource currentResource;
    private static Logger logger = LoggerFactory.getLogger(RowModel.class);

 
    
    public String getPath() {
        logger.debug("Inside Row Model class");
        return currentResource.getPath();
    }

    public String getTitle() {
        Asset asset = currentResource.adaptTo(Asset.class);
        String title = asset.getMetadataValue("dc:title");
        if(StringUtils.isBlank(title)) {
            title = asset.getName();
        }
        return title;
        
    }

    public Date getAssetExpiration() {
        logger.debug("Inside getExpiration function");
        Asset asset = currentResource.adaptTo(Asset.class);
        String expirationDateStr = asset.getMetadataValue("prism:expirationDate");
        logger.debug("DATE {}",expirationDateStr);
        if (StringUtils.isNotBlank(expirationDateStr)) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(expirationDateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

  

    

}
