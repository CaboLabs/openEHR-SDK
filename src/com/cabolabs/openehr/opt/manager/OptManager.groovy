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
   
   @Synchronized
   public void loadAll()
   {
      def root = new File( this.optRepositoryPath )
      def text, opt
      def parser = new OperationalTemplateParser()
      
      root.eachFileMatch groovy.io.FileType.FILES, ~/.*\.opt/, { optFile ->
         
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
            // If the archertype is referenced twice by different opts, it is overwritten,
            // the objectnodes can have different structures since one OPT might have all
            // the nodes and another that uses the SAME archetype might have a couple.
            // To avoid that issue, we save here all the references to the same archetype.
            // A better solution to reduce the memory use, is to merge all the internal
            // structures into one complete structure. (TODO)
            // TODO: do not add the reference twice for the same OPT (check if this case can happen)
            if (!this.referencedArchetypes[_objectNode.archetypeId]) this.referencedArchetypes[_objectNode.archetypeId] = []
            this.referencedArchetypes[_objectNode.archetypeId] << _objectNode
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
   
   /* now this is a list
   public ObjectNode getReferencedArchetype(String archetypeId)
   {
      return this.referencedArchetypes[archetypeId] // can be null!
   }
   */
   
   // done to avoid merging, merge is the optimal solution!
   public List getReferencedArchetypes(String archetypeId)
   {
      return this.referencedArchetypes[archetypeId] // can be null!
   }
   
   // done to avoid merging, merge is the optimal solution!
   public String getText(String archetypeId, String code, String lang)
   {
      if (!this.referencedArchetypes[archetypeId]) return null
      
      def t
      for (arch in this.referencedArchetypes[archetypeId])
      {
         // only query object nodes that belong to an opt that is in the language
         // that the code text is needed to be.
         if (arch.owner.getLangCode() == lang)
         {
            t = arch.getText(code)
            if (t) break
         }
      }
      
      return t // can be null
   }
   
   // done to avoid merging, merge is the optimal solution!
   public String getDescription(String archetypeId, String code)
   {
      if (!this.referencedArchetypes[archetypeId]) return null
      
      def d
      for (arch in this.referencedArchetypes[archetypeId])
      {
         d = arch.getDescription(code)
         if (d) break
      }
      
      return d // can be null
   }
   
   // done to avoid merging, merge is the optimal solution!
   public ObjectNode getNode(String archetypeId, String path)
   {
      if (!this.referencedArchetypes[archetypeId]) return null
      
      def n
      for (arch in this.referencedArchetypes[archetypeId])
      {
         n = arch.getNode(path)
         if (n) break
      }
      
      return n // can be null
   }
   
   // The problem with the previous method is that can return a node in with definitions
   // in any language, and when a getText is called, we get terms on that language instead
   // of the current locale.
   // This method returns all nodes for the arch id, and the user selects the correct language.
   public List<ObjectNode> getNodes(String archetypeId, String path)
   {
      List<ObjectNode> res = []
      
      if (!this.referencedArchetypes[archetypeId]) return res
      
      def n
      for (arch in this.referencedArchetypes[archetypeId])
      {
         n = arch.getNode(path)
         if (n) res << n
      }
      
      return res
   }
   
   @Synchronized
   public void unloadAll()
   {
       this.cache.clear()
       this.timestamps.clear()
   }
}
