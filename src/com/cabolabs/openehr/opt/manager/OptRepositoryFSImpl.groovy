package com.cabolabs.openehr.opt.manager

class OptRepositoryFSImpl implements OptRepository {

   private static String PS = File.separator

   /**
   * location is a key used to reference one OPT, could be an absolute file path,
   * or an S3 object key. The namespace is already included in the location as a
   * prefix.
   * Returns null if no OPT was found.
   */
   String getOptContents(String location)
   {
      def optFile = new File(location)

      if (!optFile.exists() || !optFile.canRead())
         throw new Exception(optFile.canonicalPath + " doesn't exists or can't be read")

      return optFile.getText()
   }

   /**
   * Does a search in the namespace and finds the first OPT that matches the templateId.
   * Returns null if no OPT was found.
   */
   String getOptContentsByTemplateId(String templateId, String namespace)
   {
      // The file path should be:
      // namespace + / + normalized(templateId) + .opt
      // normalized(templateId) adds undescores for spaces and is all lower case

      def normalizedTemplateId = getNormalizedTemplateId(templateId)
      def location = namespace + PS + normalizedTemplateId + '.opt'
      return getOptContents(location)
   }

   /**
   * Returns the contents of all the OPTs under the given namespace.
   * Returns an empty list if there are no OPTs.
   */
   List<String> getAllOptContents(String namespace)
   {

   }

   private String getNormalizedTemplateId(String templateId)
   {
      // TODO: https://gist.github.com/ppazos/12f3efc4eb178e43ff73a0c989a2e1d7
   }
}
