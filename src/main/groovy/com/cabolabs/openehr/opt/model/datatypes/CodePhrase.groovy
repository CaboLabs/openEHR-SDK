package com.cabolabs.openehr.opt.model.datatypes

class CodePhrase {

   String codeString
   String terminologyId

   /* the spec is still inconsistent about these...
   String terminologyIdName()
   {
      def tidPattern = ~/(\w+)\s*(?:\(?(\w*)\)?.*)?/
      def result = tidPattern.matcher(terminologyId)
      return result[0][1]
   }

   String terminologyIdVersion()
   {
      def tidPattern = ~/(\w+)\s*(?:\(?(\w*)\)?.*)?/
      def result = tidPattern.matcher(terminologyId)
      return result[0][2]
   }
   */
}
