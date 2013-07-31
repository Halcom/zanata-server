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
package org.zanata.tmx;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.dbunit.operation.DatabaseOperation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.ZanataDbunitJpaTest;
import org.zanata.dao.TransMemoryDAO;
import org.zanata.model.tm.TMMetadataType;
import org.zanata.model.tm.TMXMetadataHelper;
import org.zanata.model.tm.TransMemory;
import org.zanata.model.tm.TransMemoryUnit;
import org.zanata.model.tm.TransMemoryUnitVariant;
import org.zanata.seam.SeamAutowire;

import nu.xom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class TMXParserTest extends ZanataDbunitJpaTest
{
   private SeamAutowire seam = SeamAutowire.instance();

   @Override
   protected void prepareDBUnitOperations()
   {
      beforeTestOperations.add(new DataSetOperation("org/zanata/test/model/ClearAllTables.dbunit.xml", DatabaseOperation.DELETE_ALL));
      afterTestOperations.add(new DataSetOperation("org/zanata/test/model/ClearAllTables.dbunit.xml", DatabaseOperation.DELETE_ALL));
   }

   @BeforeMethod
   public void initializeSeam()
   {
      seam.reset()
          .ignoreNonResolvable()
          .use("entityManager", getEm())
          .use("session", getSession());
   }

   private TransMemory parseAndSaveTMFromFile(String file) throws XMLStreamException
   {
      TransMemoryDAO transMemoryDAO = seam.autowire(TransMemoryDAO.class);
      TransMemory tm = new TransMemory();
      tm.setSlug("new-tm");
      tm.setDescription("New test tm");
      transMemoryDAO.makePersistent(tm);

      return parseAndSaveTMFromFile(tm, file);
   }

   private TransMemory parseAndSaveTMFromFile(TransMemory tm, String file) throws XMLStreamException
   {
      TMXParser parser = seam.autowire(TMXParser.class);
      InputStream is = getClass().getResourceAsStream(file);

      parser.parseAndSaveTMX(is, tm);
      return tm;
   }

   private TransMemoryUnit findInCollection(Collection<TransMemoryUnit> col, final String tuid)
   {
      return (TransMemoryUnit)CollectionUtils.find(col, new Predicate()
      {
         @Override
         public boolean evaluate(Object o)
         {
            return ((TransMemoryUnit)o).getTransUnitId().equals(tuid);
         }
      });
   }

   @Test
   public void parseTMX() throws Exception
   {
      // Create a TM
      TransMemory tm = parseAndSaveTMFromFile("/tmx/default-valid-tm.tmx");

      // Make sure everything is stored properly
      getEm().flush();
      getEm().refresh(tm);
      assertThat(tm.getTranslationUnits().size(), is(4));

      // Dates were modified to match the TM header in the file
      Calendar cal = Calendar.getInstance();
      cal.setTime(tm.getCreationDate());
      assertThat(cal.get(Calendar.YEAR), is(2013));
      assertThat(cal.get(Calendar.MONTH), is(4));
      assertThat(cal.get(Calendar.DATE), is(10));

      assertThat(tm.getSourceLanguage(), equalTo("en"));

      // TM metadata
      assertThat(tm.getMetadata().size(), greaterThan(0));
      assertThat(tm.getMetadata().get(TMMetadataType.TMX14), notNullValue());

      // Translation Units
      for(TransMemoryUnit tu : tm.getTranslationUnits())
      {
         assertThat(tu.getTransUnitVariants().size(), greaterThan(0));
      }
   }

   @Test
   public void parseTMXWithMetadata() throws Exception
   {
      // Create a TM
      TransMemory tm = parseAndSaveTMFromFile("/tmx/valid-tmx-with-metadata.tmx");

      // Make sure everything is stored properly
      getEm().flush();
      getEm().refresh(tm);

      // Metadata at the header level
      Map<String,String> tmAtts = TMXMetadataHelper.getAttributes(tm);
      assertThat(tmAtts.size(), is(9));
      assertThat(tmAtts, hasEntry("segtype", "paragraph"));
      assertThat(tmAtts, hasEntry("creationtoolversion", "unknown"));
      assertThat(tmAtts, hasEntry("creationtool", "Zanata TransMemoryExportTMXStrategy"));
      assertThat(tmAtts, hasEntry("datatype", "unknown"));
      assertThat(tmAtts, hasEntry("adminlang", "en"));
      assertThat(tmAtts, hasEntry("o-tmf", "unknown"));
      assertThat(tmAtts, hasEntry("srclang", "*all*"));
      assertThat(tmAtts, hasKey("creationdate"));
      assertThat(tmAtts, hasKey("changedate"));

      List<Element> tmChildren = TMXMetadataHelper.getChildren(tm);
      assertThat(tmChildren.size(), is(2));
      assertThat(tmChildren.get(0).getLocalName(), is("prop"));
      assertThat(tmChildren.get(0).getValue(), is("Header Prop value"));
      assertThat(tmChildren.get(1).getLocalName(), is("note"));
      assertThat(tmChildren.get(1).getValue(), is("Header Note value"));

      // Metadata at the TU level
      TransMemoryUnit tu0 = findInCollection(tm.getTranslationUnits(), "doc0:resId0");
      Map<String,String> tu0Atts = TMXMetadataHelper.getAttributes(tu0);
      assertThat(tu0Atts.size(), is(4));
      assertThat(tmAtts, hasEntry("tuid", "doc0:resId0"));
      assertThat(tmAtts, hasEntry("srclang", "en"));
      assertThat(tmAtts, hasKey("creationdate"));
      assertThat(tmAtts, hasKey("changedate"));

      List<Element> tu0Children = TMXMetadataHelper.getChildren(tu0);
      assertThat(tu0Children.size(), is(2));
      assertThat(tu0Children.get(0).getLocalName(), is("prop"));
      assertThat(tu0Children.get(0).getValue(), is("Custom prop0 value"));
      assertThat(tu0Children.get(1).getLocalName(), is("note"));
      assertThat(tu0Children.get(1).getValue(), is("Custom note"));

      TransMemoryUnit tu1 = findInCollection(tm.getTranslationUnits(), "doc0:resId1");
      Map<String,String> tu1Atts = TMXMetadataHelper.getAttributes(tu1);
      assertThat(tu1Atts.size(), is(4));
      assertThat(tmAtts, hasEntry("tuid", "doc0:resId1"));
      assertThat(tmAtts, hasEntry("srclang", "en"));
      assertThat(tmAtts, hasKey("creationdate"));
      assertThat(tmAtts, hasKey("changedate"));

      List<Element> tu1Children = TMXMetadataHelper.getChildren(tu1);
      assertThat(tu1Children.size(), is(4));
      assertThat(tu1Children.get(0).getLocalName(), is("prop"));
      assertThat(tu1Children.get(0).getValue(), is("Custom prop0 value"));
      assertThat(tu1Children.get(1).getLocalName(), is("prop"));
      assertThat(tu1Children.get(1).getValue(), is("Custom prop1 value"));
      assertThat(tu1Children.get(2).getLocalName(), is("note"));
      assertThat(tu1Children.get(2).getValue(), is("Custom note0"));
      assertThat(tu1Children.get(3).getLocalName(), is("note"));
      assertThat(tu1Children.get(3).getValue(), is("Custom note1"));

      // Metadata at the TUV level
      TransMemoryUnitVariant tuv0 = tu0.getTransUnitVariants().get("en");
      Map<String, String> tuv0Atts = TMXMetadataHelper.getAttributes(tuv0);
      assertThat(tuv0Atts.size(), is(3));

      assertThat(tmAtts, hasEntry("lang", "en"));
      assertThat(tmAtts, hasKey("creationdate"));
      assertThat(tmAtts, hasKey("changedate"));

      List<Element> tuv0Children = TMXMetadataHelper.getChildren(tuv0);
      assertThat(tuv0Children.size(), is(2));
      assertThat(tuv0Children.get(0).getLocalName(), is("prop"));
      assertThat(tuv0Children.get(0).getValue(), is("Custom prop0 value on tuv"));
      assertThat(tuv0Children.get(1).getLocalName(), is("note"));
      assertThat(tuv0Children.get(1).getValue(), is("Custom note on tuv"));
   }

   @Test(expectedExceptions = RuntimeException.class)
   public void undiscernibleSourceLang() throws Exception
   {
      // Create a TM
      parseAndSaveTMFromFile("/tmx/invalid-tmx-no-discernible-srclang.xml");
   }

   @Test
   public void mergeSameTM() throws Exception
   {
      // Initial load
      TransMemory tm = parseAndSaveTMFromFile("/tmx/default-valid-tm.tmx");

      // Make sure everything is stored properly
      getEm().flush();
      getEm().refresh(tm);
      assertThat(tm.getTranslationUnits().size(), is(4));

      // Second load (should yield the same result)
      parseAndSaveTMFromFile(tm, "/tmx/default-valid-tm.tmx");

      getEm().flush();
      getEm().refresh(tm);
      assertThat(tm.getTranslationUnits().size(), is(4));
   }

   @Test
   public void mergeComplementaryTM() throws Exception
   {
      // Initial load
      TransMemory tm = parseAndSaveTMFromFile("/tmx/default-valid-tm.tmx");

      // Make sure everything is stored properly
      getEm().flush();
      getEm().refresh(tm);
      assertThat(tm.getTranslationUnits().size(), is(4));

      // Second load (should add all new tuids)
      parseAndSaveTMFromFile(tm, "/tmx/valid-tm-with-tuids.tmx");

      getEm().flush();
      getEm().refresh(tm);
      assertThat(tm.getTranslationUnits().size(), is(8));
   }
}
