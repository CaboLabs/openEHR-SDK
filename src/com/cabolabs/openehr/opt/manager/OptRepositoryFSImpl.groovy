package com.cabolabs.openehr.opt.manager

class OptRepositoryFSImpl implements OptRepository {

   private static String PS = File.separator

   // This is the path to the root repo location in the file system
   private String repoLocation

   def OptRepositoryFSImpl(String repoLocation)
   {
      def root = new File(repoLocation)

      if (!root.exists() || !root.canRead())
         throw new Exception(root.canonicalPath + " doesn't exists or can't be read")

      this.repoLocation = repoLocation
   }

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
      def location = addTrailingSeparator(this.repoLocation) +
                     addTrailingSeparator(namespace) +
                     normalizedTemplateId + '.opt'
      return getOptContents(location)
   }

   /**
   * Returns the contents of all the OPTs under the given namespace.
   * Returns an empty list if there are no OPTs.
   */
   List<String> getAllOptContents(String namespace)
   {
      def root = new File(addTrailingSeparator(this.repoLocation) + namespace)
      def result = []

      root.eachFileMatch groovy.io.FileType.FILES, ~/.*\.opt/, { optFile ->

         result << optFile.getText()
      }

      return result
   }

   private String getNormalizedTemplateId(String templateId)
   {
      // https://gist.github.com/ppazos/12f3efc4eb178e43ff73a0c989a2e1d7
      String normalized = java.text.Normalizer.normalize(templateId, java.text.Normalizer.Form.NFD).replaceAll(/\p{InCombiningDiacriticalMarks}+/, '')
      String camel = normalized.replaceAll( / ([A-Z])/, /$1/ ).replaceAll( /([A-Z])/, /_$1/ ).replaceAll(/\s/, '_').toLowerCase().replaceAll( /^_/, '' )
      return camel
   }

   private String addTrailingSeparator(String path)
   {
      if(path.charAt(path.length()-1) != File.separatorChar)
      {
         path += File.separator
      }

      return path
   }
}
