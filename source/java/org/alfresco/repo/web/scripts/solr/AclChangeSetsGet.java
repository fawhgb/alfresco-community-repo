package org.alfresco.repo.web.scripts.solr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.solr.AclChangeSet;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Support for SOLR: Track ACL Change Sets
 *
 * @since 4.0
 */
public class AclChangeSetsGet extends DeclarativeWebScript
{
    protected static final Log logger = LogFactory.getLog(AclChangeSetsGet.class);

    private SOLRTrackingComponent solrTrackingComponent;
    
    public void setSolrTrackingComponent(SOLRTrackingComponent solrTrackingComponent)
    {
        this.solrTrackingComponent = solrTrackingComponent;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status)
    {
        String fromIdParam = req.getParameter("fromId");
        String fromTimeParam = req.getParameter("fromTime");
        String toIdParam = req.getParameter("toId");
        String toTimeParam = req.getParameter("toTime");
        String maxResultsParam = req.getParameter("maxResults");

        Long fromId = (fromIdParam == null ? null : Long.valueOf(fromIdParam));
        Long fromTime = (fromTimeParam == null ? null : Long.valueOf(fromTimeParam));
        Long toId = (toIdParam == null ? null : Long.valueOf(toIdParam));
        Long toTime = (toTimeParam == null ? null : Long.valueOf(toTimeParam));
        int maxResults = (maxResultsParam == null ? 1024 : Integer.valueOf(maxResultsParam));
        
        List<AclChangeSet> changesets = solrTrackingComponent.getAclChangeSets(fromId, fromTime, toId, toTime, maxResults);
        
        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
        model.put("aclChangeSets", changesets);

        Long maxChangeSetCommitTime = solrTrackingComponent.getMaxChangeSetCommitTime();
        if(maxChangeSetCommitTime != null)
        {
            model.put("maxChangeSetCommitTime", maxChangeSetCommitTime);
        }
        
        Long maxChangeSetId = solrTrackingComponent.getMaxChangeSetId();
        if(maxChangeSetId != null)
        {
            model.put("maxChangeSetId", maxChangeSetId);
        }

        
        if (logger.isDebugEnabled())
        {
            logger.debug("Result: \n\tRequest: " + req + "\n\tModel: " + model);
        }
        
        return model;
    }
}
