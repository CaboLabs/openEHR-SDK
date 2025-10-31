package com.cabolabs.openehr.rm_1_0_2.support.identification

class ObjectVersionId extends UIDBasedId {

   ObjectVersionId()
   {
      super()
   }

   ObjectVersionId(String value)
   {
      super(value)
   }

   UID getObjectId()
   {
      getRoot()
   }

   UID getCreatingSystemId()
   {
      def extension = getExtension()
      if (extension != null && extension.contains("::"))
      {
         def parts = extension.split("::", 2)
         return UID.build(parts[0]) // the first part is the system id
      }

      return null // no creating system id
   }

   VersionTreeId getVersionTreeId()
   {
      def extension = getExtension()
      if (extension != null && extension.contains("::"))
      {
         def parts = extension.split("::", 2)
         if (parts.length > 1)
         {
            return new VersionTreeId(parts[1]) // the second part is the version tree id
         }
      }

      return null // no creating system id
   }

   Boolean isBranch()
   {
      getVersionTreeId()?.isBranch()
   }

   @Override
   ObjectId clone()
   {
      return new ObjectVersionId(this.value)
   }
}
