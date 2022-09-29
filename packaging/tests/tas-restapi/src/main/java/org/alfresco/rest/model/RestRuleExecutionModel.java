/*-
 * #%L
 * alfresco-tas-restapi
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.alfresco.rest.core.IRestModel;
import org.alfresco.utility.model.TestModel;

/**
 * Generated by 'krystian' on '2022-09-28 11:33' from 'Alfresco Content Services REST API' swagger file 
 * Generated from 'Alfresco Content Services REST API' swagger file
 * Base Path {@linkplain /alfresco/api/-default-/public/alfresco/versions/1}
 */
public class RestRuleExecutionModel extends TestModel implements IRestModel<RestRuleExecutionModel>
{
    @JsonProperty(value = "entry")
    RestRuleExecutionModel model;

    @Override
    public RestRuleExecutionModel onModel()
    {
        return model;
    }

    /**
    Whether the rules were executed also against sub-folders
    */	        

    @JsonProperty(required = true)    
    private boolean isEachSubFolderIncluded;	    
    /**
    Whether the inherited rules were also executed
    */	        

    @JsonProperty(required = true)    
    private boolean isEachInheritedRuleExecuted;	    

    public boolean getIsEachSubFolderIncluded()
    {
        return this.isEachSubFolderIncluded;
    }

    public void setIsEachSubFolderIncluded(boolean isEachSubFolderIncluded)
    {
        this.isEachSubFolderIncluded = isEachSubFolderIncluded;
    }				

    public boolean getIsEachInheritedRuleExecuted()
    {
        return this.isEachInheritedRuleExecuted;
    }

    public void setIsEachInheritedRuleExecuted(boolean isEachInheritedRuleExecuted)
    {
        this.isEachInheritedRuleExecuted = isEachInheritedRuleExecuted;
    }

    @Override
    public String toString()
    {
        return "RestRuleExecutionModel{" + "isEachSubFolderIncluded=" + isEachSubFolderIncluded + ", isEachInheritedRuleExecuted=" + isEachInheritedRuleExecuted + '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RestRuleExecutionModel that = (RestRuleExecutionModel) o;
        return isEachSubFolderIncluded == that.isEachSubFolderIncluded && isEachInheritedRuleExecuted == that.isEachInheritedRuleExecuted;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isEachSubFolderIncluded, isEachInheritedRuleExecuted);
    }
}
 
