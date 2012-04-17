/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.action;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HProjectIteration;
import org.zanata.service.VersionGroupService;

@Name("versionGroupAction")
@Scope(ScopeType.PAGE)
public class VersionGroupAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   @In
   private VersionGroupService versionGroupServiceImpl;

   @Logger
   Log log;

   private HIterationGroup group;
   private String searchTerm;
   private List<HProjectIteration> searchResults;

   public void init(String slug)
   {
      group = versionGroupServiceImpl.getBySlug(slug);
   }

   public String getSearchTerm()
   {
      return searchTerm;
   }

   public void setSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
   }

   public List<HProjectIteration> getSearchResults()
   {
      return searchResults;
   }

   public boolean isVersionInGroup(Long projectIterationId)
   {
      for (HProjectIteration iteration : group.getProjectIterations())
      {
         if (iteration.getId().equals(projectIterationId))
         {
            return true;
         }
      }
      return false;
   }

   public void joinVersionGroup(Long projectIterationId)
   {
      versionGroupServiceImpl.joinVersionGroup(group, projectIterationId);
   }

   /*
    * @Transactional
    * 
    * @Restrict("#{s:hasRole('admin')}") public void joinGroup() {
    * log.debug("starting join tribe"); if (authenticatedAccount == null) {
    * log.error("failed to load auth person"); return; } try {
    * languageTeamServiceImpl.joinLanguageTeam(this.language,
    * authenticatedAccount.getPerson().getId());
    * Events.instance().raiseEvent("personJoinedTribe");
    * log.info("{0} joined tribe {1}", authenticatedAccount.getUsername(),
    * this.language);
    * FacesMessages.instance().add("You are now a member of the {0} language team"
    * , this.locale.retrieveNativeName()); } catch (Exception e) {
    * FacesMessages.instance().add(Severity.ERROR, e.getMessage()); } }
    * 
    * @Transactional public void leaveGroup() {
    * log.debug("starting leave tribe"); if (authenticatedAccount == null) {
    * log.error("failed to load auth person"); return; }
    * languageTeamServiceImpl.leaveLanguageTeam(this.language,
    * authenticatedAccount.getPerson().getId());
    * Events.instance().raiseEvent("personLeftTribe");
    * log.info("{0} left tribe {1}", authenticatedAccount.getUsername(),
    * this.language);
    * FacesMessages.instance().add("You have left the {0} language team",
    * this.locale.retrieveNativeName()); }
    * 
    * @Restrict("#{languageTeamAction.checkPermission('manage-language-team')}")
    * public void saveTeamCoordinator(HLocaleMember member) {
    * this.localeDAO.makePersistent(this.locale); this.localeDAO.flush(); if
    * (member.isCoordinator()) {
    * FacesMessages.instance().add("{0} has been made a Team Coordinator",
    * member.getPerson().getAccount().getUsername()); } else {
    * FacesMessages.instance
    * ().add("{0} has been removed from Team Coordinators",
    * member.getPerson().getAccount().getUsername()); } }
    * 
    * @Restrict("#{languageTeamAction.checkPermission('manage-language-team')}")
    * public void addTeamMember(final Long personId) {
    * this.languageTeamServiceImpl.joinLanguageTeam(this.language, personId); //
    * reload the locale for changes this.locale =
    * localeServiceImpl.getByLocaleId(new LocaleId(language)); }
    * 
    * @Restrict("#{languageTeamAction.checkPermission('manage-language-team')}")
    * public void removeMembership(HLocaleMember member) {
    * this.languageTeamServiceImpl.leaveLanguageTeam(this.language,
    * member.getPerson().getId()); this.locale.getMembers().remove(member); }
    * 
    * public boolean isPersonInTeam(final Long personId) { for (HLocaleMember lm
    * : this.locale.getMembers()) { if (lm.getPerson().getId().equals(personId))
    * { return true; } } return false; }
    */

   public void searchProjectAndVersion()
   {
      try
      {
         this.searchResults = versionGroupServiceImpl.searchBySlugAndProjectSlug(this.searchTerm);
      }
      catch (ParseException e)
      {
         return;
      }
   }
}