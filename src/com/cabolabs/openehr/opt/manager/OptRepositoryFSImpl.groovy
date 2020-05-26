package com.cabolabs.openehr.opt.manager

import net.pempek.unicode.UnicodeBOMInputStream
import org.apache.log4j.Logger

class OptRepositoryFSImpl implements OptRepository {

   private static String PS = File.separator
   private Logger log = Logger.getLogger(getClass())

   // This is the path to the root repo location in the file system
   private String repoLocation

   def OptRepositoryFSImpl(String repoLocation)
   {
      def root = new File(repoLocation)

      if (!root.exists() || !root.canRead())
         throw new Exception(root.canonicalPath + " doesn't exists or can't be read")

      this.repoLocation = repoLocation
   }

   boolean storeOptContents(String fileLocation, String fileContents)
   {
      // creates parent subfolders if dont exist
      def containerFolder = new File(new File(fileLocation).getParent())
      containerFolder.mkdirs()

      File fileDest = new File(fileLocation)

      if (fileDest.exists())
      {
         log.warn "File "+ fileLocation +" already exists, overwriting"
      }

      fileDest.write(this.removeBOM(fileContents.getBytes())) // overwrites if file exists

      return true // TODO check errors
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

      return this.removeBOM(optFile.bytes)
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

      if (!isNormalizedTemplateId(templateId))
      {
         templateId = normalizeTemplateId(templateId)
      }

      def location = addTrailingSeparator(this.repoLocation) +
                     addTrailingSeparator(namespace) +
                     templateId + '.opt'

      return getOptContents(location)
   }

   /**
    * Returns the contents of all the OPTs under the given namespace.
    * Returns an empty list if there are no OPTs.
    */
   List<String> getAllOptContents(String namespace)
   {
      def result = []
      def root = new File(addTrailingSeparator(this.repoLocation) + namespace)

      root.eachFileMatch groovy.io.FileType.FILES, ~/.*\.opt/, { optFile ->

         result << this.removeBOM(optFile.bytes)
      }

      return result
   }

   /**
    * Similar to getAllOptContents, but returns the key (location) of each OPT.
    */
   Map<String, String> getAllOptKeysAndContents(String namespace)
   {
      def result = [:]
      def root = new File(addTrailingSeparator(this.repoLocation) + namespace)

      root.eachFileMatch groovy.io.FileType.FILES, ~/.*\.opt/, { optFile ->

         result[optFile.getCanonicalPath()] = this.removeBOM(optFile.bytes)
      }

      return result
   }

   String normalizeTemplateId(String templateId, String language)
   {
      // https://gist.github.com/ppazos/12f3efc4eb178e43ff73a0c989a2e1d7
      String normalized = java.text.Normalizer.normalize(templateId, java.text.Normalizer.Form.NFD).replaceAll(/\p{InCombiningDiacriticalMarks}+/, '')
      String snake      = normalized.replaceAll( / ([A-Z])/, /$1/ ).replaceAll( /([A-Z])/, /_$1/ ).replaceAll(/\s/, '_').toLowerCase().replaceAll( /^_/, '' )
      String removeDots = snake.replaceAll(/\./, '_')
      String plusLAndV  = removeDots +'.'+ language +'.v1'
      return plusLAndV
   }

   boolean isNormalizedTemplateId(String templateId)
   {
      (templateId ==~ /([a-z]+(_[a-z0-9]+)*)\.([a-z]{2})\.v([0-9]+[0-9]*(\.[0-9]+[0-9]*(\.[0-9]+[0-9]*)?)?)/)
   }

   String addTrailingSeparator(String path)
   {
      if(path.charAt(path.length()-1) != File.separatorChar)
      {
         path += File.separator
      }

      return path
   }

   String removeBOM(byte[] bytes)
   {
      def inputStream = new ByteArrayInputStream(bytes)
      def bomInputStream = new UnicodeBOMInputStream(inputStream)
      bomInputStream.skipBOM() // NOP if no BOM is detected
      def br = new BufferedReader(new InputStreamReader(bomInputStream))
      return br.text // http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/BufferedReader.html#getText()
   }

   String newOptFileLocation(String templateId, String language, String namespace)
   {
      if (!isNormalizedTemplateId(templateId))
      {
         templateId = normalizeTemplateId(templateId, language)
      }

      addTrailingSeparator(repoLocation) +
      addTrailingSeparator(namespace) +
      templateId + '.opt'
   }
}
