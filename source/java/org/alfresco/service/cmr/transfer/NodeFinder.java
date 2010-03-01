/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.service.cmr.transfer;

import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author brian
 * 
 * NodeFinders find nodes related to the current node.
 * The NodeCrawler will first initialise this filter by calling the 
 * setServiceRegistry and init methods.  Then the findFrom method will be called to find 
 * other nodes.
 * 
 * @see org.alfresco.repo.transfer.ChildAssociatedNodeFinder
 */
public interface NodeFinder
{

    /**
     * @param thisNode
     * @param serviceRegistry 
     * @return
     */
    Set<NodeRef> findFrom(NodeRef thisNode);

    /**
     * called by the node crawler to initialise this class.
     */
    void init();

    /**
     * 
     * @param serviceRegistry
     */
    void setServiceRegistry(ServiceRegistry serviceRegistry);
}
