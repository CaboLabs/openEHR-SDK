package com.cabolabs.openehr.opt.manager

import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.OperationalTemplate

import org.apache.log4j.Logger
import groovy.transform.Synchronized

class OptManager {
   
   private Logger log = Logger.getLogger(getClass())

   private static String PS = File.separator
   
   private String optRepositoryPath = "opts"+ PS
   
   // Cache: otpid => OperationalTemplate
   private static Map<String, OperationalTemplate> cache = [:]
   
   // otpid => timestamp de cuando fue usado por ultima vez.
   // Sirve para saber si un arquetipo no fue utilizado por mucho tiempo, y bajarlo del cache par optimizar espacio en memoria.
   private static Map<String, Date> timestamps = [:]
   
   
   // Archetypes referenced by all the templates loaded
   // it allows to reference the archetypes instead of the templates,
   // e.g. for querying. ObjectNode points to an archetype root.
   private static Map<String, ObjectNode> referencedArchetypes = [:]
   
   // SINGLETON
   private static OptManager instance = null
   
   
   private OptManager(String repoPath)
   {
     if (repoPath) this.optRepositoryPath = repoPath
     
     if (!new File(this.optRepositoryPath).exists() || !new File(this.optRepositoryPath).canRead())
        throw new Exception(this.optRepositoryPath + " doesn't exists or can't be read")
   }
   
   public static OptManager getInstance(String repoPath)
   {
      if (!instance) instance = new OptManager(repoPath)
      return instance
   }
   
   public void loadAll()
   {
      def root = new File( this.optRepositoryPath )
      def text, opt
      def parser = new OperationalTemplateParser()
      
      root.eachFile(groovy.io.FileType.FILES) { optFile ->
         
         text = optFile.getText()
         opt = parser.parse( text )

         if (opt)
         {
            log.debug("Loading OPT: " + optFile.path)
            this.cache[opt.templateId] = opt
            this.timestamps[opt.templateId] = new Date()
         }
         else
         {
            //log.error("No se pudo cargar el arquetipo: " + f.name + " de:\n\t " + root.path)
         }
      }
      
      def refarchs = []
      this.cache.each { _optid, _opt ->
         refarchs = _opt.getReferencedArchetypes()
         refarchs.each { _objectNode ->
            // if the archertype is referenced twice by different opts, it is overriden,
            // the objectnode will have the same structure in most cases.
            // TODO:
            // For now we dont care about specific constraints, but later we might need to
            // merge the different constraints or just store all the different ones here.
            this.referencedArchetypes[_objectNode.archetypeId] = _objectNode
         }
      }
   }
   
   public OperationalTemplate getOpt(String templateId)
   {
      if (this.cache[templateId])
      {
         this.timestamps[templateId] = new Date() // actualizo timestamp
         return this.cache[templateId]
      }
      
      def optFile = new File( this.optRepositoryPath + PS + templateId +".opt" )
      def text = optFile.getText()
      def parser = new OperationalTemplateParser()
      def opt = parser.parse( text )
      
      if (opt)
      {
         log.debug("Loading OPT: " + optFile.path)
         this.cache[opt.templateId] = opt
         this.timestamps[opt.templateId] = new Date()
      }
   }
   
   public Map getLoadedOpts()
   {
      return this.cache.asImmutable()
   }
   
   public Map getAllReferencedArchetypes()
   {
      return this.referencedArchetypes.asImmutable()
   }
   
   @Synchronized
   public void unloadAll()
   {
       this.cache.clear()
       this.timestamps.clear()
   }
}
