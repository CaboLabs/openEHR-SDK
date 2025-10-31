package com.cabolabs.openehr.rm_1_0_2.support.identification

abstract class UIDBasedId extends ObjectId {

   // NOTE: this is similar to UID.UUID_OR_OID_OR_INTERNET_ID_PATTERN but has non capturing groups and ^/$ start/finish
   // This checks UUID or OID or InternetID formats
   static def UID_PATTERN = /(?:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|[0-9]+(?:\.[0-9]+)*|[a-zA-Z0-9_](?:[a-zA-Z0-9_-]{0,61}[a-zA-Z0-9_])?(?:\.[a-zA-Z0-9_](?:[a-zA-Z0-9_-]{0,61}[a-zA-Z0-9_])?)*)/

   static def VERSION_TREE_PATTERN = /[0-9]+(?:\.[0-9]+\.[0-9]+)?/

   static def OBJECT_VERSION_PATTERN = /^${UID_PATTERN}::${UID_PATTERN}::${VERSION_TREE_PATTERN}$/

   static def HIER_OBJECT_PATTERN = /^${UID_PATTERN}(?::[^:]+)?$/ // do not match :: in the extension part so it doesn't match OBJECT_VERSION_PATTERN

   UIDBasedId()
   {
      super()
   }

   UIDBasedId(String uid)
   {
      if (isUIDBasedId(uid))
      {
         this.value = uid
      }
      else
      {
         throw new IllegalArgumentException("Invalid UID: " + uid)
      }
   }

   UID getRoot()
   {
      def parts = this.value.split("::", 2)
      return UID.build(parts[0])
   }

   String getExtension()
   {
      def parts = this.value.split("::", 2)
      if (parts.length > 1)
      {
         return parts[1]
      }

      return null // no extension
   }

   boolean hasExtension()
   {
      this.getExtension() != null
   }

   static boolean isUIDBasedId(String input)
   {
      isObjectVersionId(input) || isHierObjectId(input)
   }

   static boolean isObjectVersionId(String input)
   {
      input?.matches(OBJECT_VERSION_PATTERN) ?: false
   }
   
   static boolean isHierObjectId(String input)
   {
      input?.matches(HIER_OBJECT_PATTERN) ?: false
   }

}
