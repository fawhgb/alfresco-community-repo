/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.admin.patch.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.admin.patch.PatchExecuter;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.domain.DbAuthority;
import org.alfresco.repo.domain.control.ControlDAO;
import org.alfresco.repo.domain.hibernate.DbAuthorityImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.admin.PatchException;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Fixes <a href=https://issues.alfresco.com/jira/browse/ALF-478>ALF-478</a>.
 * Checks all CRC values for <b>alf_authorities</b>.
 * 
 * @author Andrew Hind
 * @since V3.3
 */
public class FixAuthoritiesCrcValuesPatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.fixAuthoritiesCrcValues.result";
    private static final String MSG_REWRITTEN = "patch.fixAuthoritiesCrcValues.fixed";
    private static final String MSG_UNABLE_TO_CHANGE = "patch.fixAuthoritiesCrcValues.unableToChange";
    
    private SessionFactory sessionFactory;
    private ControlDAO controlDAO;
    private RuleService ruleService;
    
    public FixAuthoritiesCrcValuesPatch()
    {
    }
    
    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param controlDAO        used to create Savepoints
     */
    public void setControlDAO(ControlDAO controlDAO)
    {
        this.controlDAO = controlDAO;
    }

    /**
     * @param ruleService the rule service
     */
    public void setRuleService(RuleService ruleService)
    {
        this.ruleService = ruleService;
    }

    @Override
    protected void checkProperties()
    {
        super.checkProperties();
        checkPropertyNotNull(sessionFactory, "sessionFactory");
        checkPropertyNotNull(applicationEventPublisher, "applicationEventPublisher");
    }

    @Override
    protected String applyInternal() throws Exception
    {
        // initialise the helper
        HibernateHelper helper = new HibernateHelper();
        helper.setSessionFactory(sessionFactory);
        
        try
        {
            String msg = helper.fixCrcValues();
            // done
            return msg;
        }
        finally
        {
            helper.closeWriter();
        }
    }
    
    private class HibernateHelper extends HibernateDaoSupport
    {
        private File logFile;
        private FileChannel channel;
        
        private HibernateHelper() throws IOException
        {
            // put the log file into a long life temp directory
            File tempDir = TempFileProvider.getLongLifeTempDir("patches");
            logFile = new File(tempDir, "FixAuthorityCrcValuesPatch.log");
            
            // open the file for appending
            RandomAccessFile outputFile = new RandomAccessFile(logFile, "rw");
            channel = outputFile.getChannel();
            // move to the end of the file
            channel.position(channel.size());
            // add a newline and it's ready
            writeLine("").writeLine("");
            writeLine("FixAuthorityCrcValuesPatch executing on " + new Date());
        }
        
        private HibernateHelper write(Object obj) throws IOException
        {
            channel.write(ByteBuffer.wrap(obj.toString().getBytes("UTF-8")));
            return this;
        }
        private HibernateHelper writeLine(Object obj) throws IOException
        {
            write(obj);
            write("\n");
            return this;
        }
        private void closeWriter()
        {
            try { channel.close(); } catch (Throwable e) {}
        }

        public String fixCrcValues() throws Exception
        {
            // get the association types to check
            BatchProcessor<Long> batchProcessor = new BatchProcessor<Long>(
                    "FixAuthorityCrcValuesPatch",
                    transactionService.getRetryingTransactionHelper(),
                    findMismatchedCrcs(),
                    2, 20,
                    applicationEventPublisher,
                    logger, 1000);

            // Precautionary flush and clear so that we have an empty session
            getSession().flush();
            getSession().clear();

            int updated = batchProcessor.process(new BatchProcessWorker<Long>()
            {
                public String getIdentifier(Long entry)
                {
                    return entry.toString();
                }
                
                public void beforeProcess() throws Throwable
                {
                    // Switch rules off
                    ruleService.disableRules();
                    // Authenticate as system
                    String systemUsername = AuthenticationUtil.getSystemUserName();
                    AuthenticationUtil.setFullyAuthenticatedUser(systemUsername);
                }

                public void process(Long authorityId) throws Throwable
                {
                    DbAuthority authority = (DbAuthority) getHibernateTemplate().get(DbAuthorityImpl.class, authorityId);
                    if (authority == null)
                    {
                        // Missing now ...
                        return;
                    }
                    // Get the old CRCs
                    long oldCrc = authority.getCrc();
                    String authorityName = authority.getAuthority();
                    
                    // Update the CRCs
                    long updatedCrc = getCrc(authorityName);
                    authority.setCrc(updatedCrc);
                  
                    // Persist
                    Savepoint savepoint = controlDAO.createSavepoint("FixAuthorityCrcValuesPatch");
                    try
                    {
                        getSession().flush();
                        controlDAO.releaseSavepoint(savepoint);
                    }
                    catch (Throwable e)
                    {
                        controlDAO.rollbackToSavepoint(savepoint);
                        
                        String msg = I18NUtil.getMessage(MSG_UNABLE_TO_CHANGE, authority.getId(), authority.getAuthority(), oldCrc,
                                updatedCrc, e.getMessage());
                        // We just log this and add details to the message file
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(msg, e);
                        }
                        else
                        {
                            logger.warn(msg);
                        }
                        writeLine(msg);
                    }
                    getSession().clear();
                    // Record
                    writeLine(I18NUtil.getMessage(MSG_REWRITTEN,authority.getId(), authority.getAuthority(), oldCrc,
                            updatedCrc));
                }
                
                public void afterProcess() throws Throwable
                {
                    ruleService.enableRules();
                }
            }, true);

            
            String msg = I18NUtil.getMessage(MSG_SUCCESS, updated, logFile);
            return msg;
        }
        
        private List<Long> findMismatchedCrcs() throws Exception
        {
            final List<Long> authorityIds = new ArrayList<Long>(1000);
            HibernateCallback callback = new HibernateCallback()
            {
                public Object doInHibernate(Session session)
                {
                    SQLQuery query = session
                            .createSQLQuery(
                                    " SELECT " +
                                    "    au.id AS authority_id," +
                                    "    au.authority AS authority," +
                                    "    au.crc as crc" +
                                    " FROM" +
                                    "    alf_authority au");
                    query.addScalar("authority_id", new LongType());
                    query.addScalar("authority", new StringType());
                    query.addScalar("crc", new LongType());
                    return query.scroll(ScrollMode.FORWARD_ONLY);
                }
            };
            ScrollableResults rs = null;
            try
            {
                rs = (ScrollableResults) getHibernateTemplate().execute(callback);
                while (rs.next())
                {
                    // Compute child name crc
                    Long authorityId = (Long) rs.get(0);
                    String authority = (String) rs.get(1);
                    Long crc = (Long) rs.get(2);
                    long calculatedCrc = 0;
                    if (authority != null)
                    {
                        calculatedCrc = getCrc(authority);
                    }

                    // Check
                    if (crc != null && crc.equals(calculatedCrc))
                    {
                        // It is a match, so ignore
                        continue;
                    }
                    authorityIds.add(authorityId);
                }
            }
            catch (Throwable e)
            {
                logger.error("Failed to query for authority CRCs", e);
                writeLine("Failed to query for authority CRCs: " + e.getMessage());
                throw new PatchException("Failed to query for authority CRCs", e);
            }
            finally
            {
                if (rs != null)
                {
                    try { rs.close(); } catch (Throwable e) { writeLine("Failed to close resultset: " + e.getMessage()); }
                }
            }
            return authorityIds;
        }
        
        /**
         * @param str           the name that will be kept as is
         * @return              the CRC32 calculated on the exact case sensitive version of the string
         */
        private long getCrc(String str)
        {
            CRC32 crc = new CRC32();
            try
            {
                crc.update(str.getBytes("UTF-8"));              // https://issues.alfresco.com/jira/browse/ALFCOM-1335
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException("UTF-8 encoding is not supported");
            }
            return crc.getValue();
        }
    }
}
