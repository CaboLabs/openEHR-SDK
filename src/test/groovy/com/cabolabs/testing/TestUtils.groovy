package com.cabolabs.testing

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*

/**
 * Common operations for unit tests.
 */
class TestUtils {

   static OperationalTemplate loadTemplate(String path)
   {
      //println path
      //println Class.class.getResource(path)
      //println Class.class.getResourceAsStream(path)
      //println this.class.getClass().getResource(path)
      // println getClassLoader().getSystemResource(path) // this works on JDK 11
      // println getClassLoader().getResource(path)
      // println getClass().getResource(path)

      // works on JDK8
      //def optFile = new File(getClass().getResource(path).toURI())

      // works on JDK11
      def optFile = new File(getClassLoader().getSystemResource(path).toURI())

      if (!optFile.exists()) throw new Exception("Template at ${path} doesn't exist")

      def text = optFile.getText()

      def parser = new OperationalTemplateParser()
      return parser.parse(text)
   }
}