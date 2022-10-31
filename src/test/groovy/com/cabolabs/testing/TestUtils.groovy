package com.cabolabs.testing

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*

/**
 * Common operations for unit tests.
 */
class TestUtils {

   static public String PS = System.getProperty("file.separator")

   static OperationalTemplate loadTemplate(String path)
   {
      def optFile = new File(getClass().getResource(path).toURI())

      if (!optFile.exists()) throw new Exception("Template at ${path} doesn't exist")

      def text = optFile.getText()

      def parser = new OperationalTemplateParser()
      return parser.parse(text)
   }
}