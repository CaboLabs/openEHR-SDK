package com.cabolabs.openehr.rm_1_0_2.support.identification

abstract class UIDBasedId extends ObjectId {

   UIDBasedId()
   {
      super()
   }

   UIDBasedId(String uid)
   {
      if (UID.isUID(uid))
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

}
