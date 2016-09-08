/*
 * Copyright (C) 2009-2011 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.module.org_alfresco_module_rm.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Base records management job implementation.  
 * <p>
 * Delegates job execution and ensures locking
 * is enforced.
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementJob implements Job
{
    private static long DEFAULT_TIME = 30000L;
    
    private JobLockService jobLockService;
    
    private RecordsManagementJobExecuter jobExecuter;
    
    private String jobName;
    
    private QName getLockQName()
    {
        return QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, jobName);
    }
    
    /**
     * Attempts to get the lock.  If the lock couldn't be taken, then <tt>null</tt> is returned.
     * 
     * @return          Returns the lock token or <tt>null</tt>
     */
    private String getLock()
    {
        try
        {
            return jobLockService.getLock(getLockQName(), DEFAULT_TIME);
        }
        catch (LockAcquisitionException e)
        {
            return null;
        }
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        // get the job lock service
        jobLockService = (JobLockService)context.getJobDetail().getJobDataMap().get("jobLockService");
        if (jobLockService == null)
        {
            throw new AlfrescoRuntimeException("Job lock service has not been specified.");
        }
        
        // get the job executer
        jobExecuter = (RecordsManagementJobExecuter)context.getJobDetail().getJobDataMap().get("jobExecuter");
        if (jobExecuter == null)
        {
            throw new AlfrescoRuntimeException("Job executer has not been specified.");
        }
        
        // get the job name
        jobName = (String)context.getJobDetail().getJobDataMap().get("jobName");
        if (jobName == null)
        {
            throw new AlfrescoRuntimeException("Job name has not been specified.");
        }
        
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            public Void doWork() throws Exception
            {        
                // try and get the lock
                String lockToken = getLock();
                if (lockToken != null)
                {                               
                    try
                    {
                        // do work
                        jobExecuter.execute();
                    }
                    finally
                    {
                        jobLockService.releaseLock(lockToken, getLockQName());
                    }
                }
            
                // return 
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }
}
