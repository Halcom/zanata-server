package org.zanata.hibernate.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;

/**
 * Index a list of strings in multiple fields, appending the string index to the
 * field name to produce unique fields.
 * 
 * e.g. For a field labeled 'fieldName' for a list of 3 strings
 * <ul>
 * <li>First string is indexed as 'fieldName0'</li>
 * <li>Second string is indexed as 'fieldName1'</li>
 * <li>Third string is indexed as 'fieldName2'</li>
 * </ul>
 * 
 * @author David Mason, damason@redhat.com
 * 
 */
public class StringListBridge implements FieldBridge, ParameterizedBridge
{

   @Logger
   Log log;

   private ConfigurableNgramAnalyzer analyzer;

   @Override
   public void setParameterValues(@SuppressWarnings("rawtypes") Map parameters)
   {
      if (parameters.containsKey("case"))
      {
         String caseBehaviour = (String) parameters.get("case");
         if ("fold".equals(caseBehaviour))
         {
            analyzer = new DefaultNgramAnalyzer();
         }
         else if ("preserve".equals(caseBehaviour))
         {
            analyzer = new CaseSensitiveNgramAnalyzer();
         }
         else
         {
            log.warn("invalid value for parameter \"case\": \"{0}\", default will be used", caseBehaviour);
            analyzer = new DefaultNgramAnalyzer();
         }
      }
   }

   @Override
   public void set(String name, Object value, Document luceneDocument, LuceneOptions luceneOptions)
   {
      if (analyzer == null)
      {
         analyzer = new DefaultNgramAnalyzer();
      }

      if (!(value instanceof List<?>))
      {
         throw new IllegalArgumentException("this bridge must be applied to a List");
      }
      List<String> strings = (List<String>) value;
      for (int i = 0; i < strings.size(); i++)
      {
         addStringField(name + i, strings.get(i), luceneDocument, luceneOptions);
      }
   }

   private void addStringField(String fieldName, String fieldValue, Document luceneDocument, LuceneOptions luceneOptions)
   {
      Field field = new Field(fieldName, fieldValue, luceneOptions.getStore(), luceneOptions.getIndex(), luceneOptions.getTermVector());
      field.setBoost(luceneOptions.getBoost());

      // manually apply token stream from analyzer, as hibernate search does not
      // apply the specified analyzer properly
      try
      {
         field.setTokenStream(analyzer.reusableTokenStream(fieldName, new StringReader(fieldValue)));
      }
      catch (IOException e)
      {
         log.error("Failed to get token stream from analyzer for field \"{0}\" with content \"{1}\"", e, fieldName, fieldValue);
      }
      luceneDocument.add(field);
   }

}
