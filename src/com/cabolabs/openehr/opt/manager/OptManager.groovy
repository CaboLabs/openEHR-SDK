package com.cabolabs.openehr.opt.manager

import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate

import org.apache.log4j.Logger

class OptManager {
   
   private Logger log = Logger.getLogger(getClass())

   private static String PS = File.separator
   
   private String optRepositoryPath = "opts"+ PS
   
   // Cache: archetypeId => Archetype
   private static Map<String, OperationalTemplate> cache = [:]
   
   // archetypeId => timestamp de cuando fue usado por ultima vez.
   // Sirve para saber si un arquetipo no fue utilizado por mucho tiempo, y bajarlo del cache par optimizar espacio en memoria.
   private static Map<String, Date> timestamps = [:]
   
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
         
         log.debug("LOAD: [" + optFile.name + "]")

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
      return this.cache
   }
   
   public void unloadAll()
   {
       // FIXME: debe estar sincronizada
       this.cache.clear()
       this.timestamps.clear()
   }
}
