/*
 * Copyright 2012, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.service.impl;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.TextFlowDAO;
import org.zanata.exception.ZanataServiceException;
import org.zanata.hibernate.search.CaseSensitiveNgramAnalyzer;
import org.zanata.hibernate.search.DefaultNgramAnalyzer;
import org.zanata.hibernate.search.IndexFieldLabels;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.search.FilterConstraints;
import org.zanata.service.LocaleService;
import org.zanata.service.TextFlowSearchService;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.WorkspaceId;
import org.zanata.webtrans.shared.util.TextFlowFilter;

/**
 * @author David Mason, damason@redhat.com
 */
@Name("textFlowSearchServiceImpl")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class TextFlowSearchServiceImpl implements TextFlowSearchService
{


   @Logger
   Log log;

   @In
   TextFlowDAO textFlowDAO;

   @In
   private LocaleService localeServiceImpl;

   @In
   DocumentDAO documentDAO;

   @In
   TextFlowFilter textFlowFilterImpl;

   @In
   private FullTextEntityManager entityManager;


   @Override
   public List<HTextFlowTarget> findTextFlowTargets(WorkspaceId workspace, FilterConstraints constraints)
   {
      LocaleId localeId = workspace.getLocaleId();
      String projectSlug = workspace.getProjectIterationId().getProjectSlug();
      String iterationSlug = workspace.getProjectIterationId().getIterationSlug();

      // TODO consider whether to allow null and empty search strings.
      // May want to fork to use a different method to retrieve all targets if
      // empty targets are required.

      //check that locale is valid for the workspace
      try
      {
         localeServiceImpl.validateLocaleByProjectIteration(localeId, projectSlug, iterationSlug);
      }
      catch (ZanataServiceException e)
      {
         throw new ZanataServiceException("Failed to validate locale", e);
      }

      String searchFieldPrefix;
      Analyzer ngramAnalyzer;
      if (constraints.isCaseSensitive())
      {
         searchFieldPrefix = IndexFieldLabels.CONTENT_CASE_PRESERVED;
         ngramAnalyzer = new CaseSensitiveNgramAnalyzer();
      }
      else
      {
         searchFieldPrefix = IndexFieldLabels.CONTENT_CASE_FOLDED;
         ngramAnalyzer = new DefaultNgramAnalyzer();
      }

      String[] searchFields = new String[6];
      for (int i = 0; i < 6; i++)
      {
         searchFields[i] = searchFieldPrefix + i;
      }

      Query searchPhraseQuery;
      QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_29, searchFields, ngramAnalyzer);
      try
      {
         searchPhraseQuery = parser.parse("\"" + QueryParser.escape(constraints.getSearchString()) + "\"");
      }
      catch (ParseException e)
      {
         throw new ZanataServiceException("Failed to parse query", e);
      }

      TermQuery projectQuery = new TermQuery(new Term(IndexFieldLabels.PROJECT_FIELD, projectSlug));
      TermQuery iterationQuery = new TermQuery(new Term(IndexFieldLabels.ITERATION_FIELD, iterationSlug));
      TermQuery localeQuery = new TermQuery(new Term(IndexFieldLabels.LOCALE_ID_FIELD, localeId.getId()));

      BooleanQuery mustMatchAllQuery = new BooleanQuery();
      mustMatchAllQuery.add(projectQuery, Occur.MUST);
      mustMatchAllQuery.add(iterationQuery, Occur.MUST);
      mustMatchAllQuery.add(localeQuery, Occur.MUST);
      mustMatchAllQuery.add(searchPhraseQuery, Occur.MUST);

      FullTextQuery ftQuery = entityManager.createFullTextQuery(mustMatchAllQuery, HTextFlowTarget.class);

      @SuppressWarnings("unchecked")
      List<HTextFlowTarget> matches = (List<HTextFlowTarget>) ftQuery.getResultList();

      return  matches;
   }


   @Override
   public List<HTextFlow> findTextFlows(WorkspaceId workspace, DocumentId doc, FilterConstraints constraints)
   {
      // TODO Implement findTextFlows within document
      return null;
   }
}
