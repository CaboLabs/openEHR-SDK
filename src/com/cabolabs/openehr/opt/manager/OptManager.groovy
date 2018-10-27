package com.cabolabs.openehr.opt.manager

import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.OperationalTemplate

import org.apache.log4j.Logger
import groovy.transform.Synchronized

class OptManager {

   private Logger log = Logger.getLogger(getClass())

   private static String PS = File.separator

   // ns will be used as folder name where OPTs are separated in the repo
   // most OS have a file name limit of 255, so that should be the limit of the ns size
   private static String DEFAULT_NAMESPACE = 'com.cabolabs.openehr_opt.namespaces.default'

   private String baseOptRepoPath = "opts"+ PS

   // Cache: otpid => OperationalTemplate
   //private static Map<String, OperationalTemplate> cache = [:]
   private static Map<String, Map<String, OperationalTemplate>> cache = [:] // [namespace -> [optid -> OPT]]

   // otpid => timestamp de cuando fue usado por ultima vez.
   // Sirve para saber si un arquetipo no fue utilizado por mucho tiempo, y bajarlo del cache par optimizar espacio en memoria.
   //private static Map<String, Date> timestamps = [:]
   private static Map<String, Map<String, Date>> timestamps = [:] // [namespace -> [optid -> date]]

   // Archetypes referenced by all the templates loaded
   // The list of archetype roots has more than one item when the same archetype os referenced from different OPTs
   // namespace -> [archId -> [arch roots]]
   private static Map<String, Map<String, List<ObjectNode>>> referencedArchetypes = [:]

   // SINGLETON
   private static OptManager instance = null


   private OptManager(String repoPath)
   {
      if (repoPath) this.baseOptRepoPath = repoPath
      else log.warn('OptManager using deefault baseOptRepoPath '+ this.baseOptRepoPath)

      def repo = new File(this.baseOptRepoPath)
      if (!repo.exists() || !repo.canRead())
         throw new Exception(this.baseOptRepoPath + " doesn't exists or can't be read")
   }

   public static OptManager getInstance(String repoPath)
   {
      if (!instance) instance = new OptManager(repoPath)
      return instance
   }

   @Synchronized
   public void loadAll(String namespace = DEFAULT_NAMESPACE)
   {
      def root = new File( this.baseOptRepoPath + PS + namespace )

      if (!root.exists() || !root.canRead())
         throw new Exception(root.canonicalPath + " doesn't exists or can't be read")

      def text, opt
      def parser = new OperationalTemplateParser()

      root.eachFileMatch groovy.io.FileType.FILES, ~/.*\.opt/, { optFile ->

         text = optFile.getText()
         opt = parser.parse( text )

         if (opt)
         {
            log.debug("Loading OPT: " + optFile.path)

            if (!this.cache[namespace]) this.cache[namespace] = [:]
            if (!this.timestamps[namespace]) this.timestamps[namespace] = [:]

            this.cache[namespace][opt.templateId] = opt
            this.timestamps[namespace][opt.templateId] = new Date()
         }
         else
         {
            //log.error("No se pudo cargar el arquetipo: " + f.name + " de:\n\t " + root.path)
         }
      }

      def refarchs = []
      this.cache[namespace].each { _optid, _opt ->
         refarchs = _opt.getReferencedArchetypes()
         refarchs.each { _objectNode ->
            // If the archertype is referenced twice by different opts, it is overwritten,
            // the objectnodes can have different structures since one OPT might have all
            // the nodes and another that uses the SAME archetype might have a couple.
            // To avoid that issue, we save here all the references to the same archetype.
            // A better solution to reduce the memory use, is to merge all the internal
            // structures into one complete structure. (TODO)
            // TODO: do not add the reference twice for the same OPT (check if this case can happen)
            if (!this.referencedArchetypes[namespace]) this.referencedArchetypes[namespace] = [:]
            if (!this.referencedArchetypes[namespace][_objectNode.archetypeId]) this.referencedArchetypes[namespace][_objectNode.archetypeId] = []
            this.referencedArchetypes[namespace][_objectNode.archetypeId] << _objectNode
         }
      }
   }

   /**
    * templateId identifier of the OPT that is requested
    * namespace from where the manager will try to load the template
    * filename associated with the template, if not present, it is the templateId with .opt extension,
    *          this is because external systems might assign a custom filename for OPTs that is not the templateId.
    */
   public OperationalTemplate getOpt(String templateId, String namespace = DEFAULT_NAMESPACE, String filename = null)
   {
      // cache hit?
      if (this.cache[namespace] && this.cache[namespace][templateId])
      {
         this.timestamps[namespace][templateId] = new Date() // actualizo timestamp
         return this.cache[namespace][templateId]
      }

      // cache miss, try to load
      def root = new File( this.baseOptRepoPath + PS + namespace )

      if (!root.exists() || !root.canRead())
         throw new Exception(root.canonicalPath + " doesn't exists or can't be read")

      if (!filename) filename = templateId +".opt"
      def optFile = new File( root.canonicalPath + PS + filename )

      if (!optFile.exists() || !optFile.canRead())
         throw new Exception(optFile.canonicalPath + " doesn't exists or can't be read")


      def text = optFile.getText()
      def parser = new OperationalTemplateParser()
      def opt = parser.parse( text )

      if (opt)
      {
         log.debug("Loading OPT: " + optFile.path)

         if (!this.cache[namespace]) this.cache[namespace] = [:]
         if (!this.timestamps[namespace]) this.timestamps[namespace] = [:]

         this.cache[namespace][opt.templateId] = opt
         this.timestamps[namespace][opt.templateId] = new Date()
      }
      else
      {
         throw new Exception("OPT file could not be loaded "+ optFile.canonicalPath +" "+ templateId)
      }

      return this.cache[namespace][templateId]
   }

   public Map getLoadedOpts(String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.cache[namespace]) return [:]
      return this.cache[namespace].asImmutable()
   }

   public Map getAllReferencedArchetypes(String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return [:]
      return this.referencedArchetypes[namespace].asImmutable()
   }

   // done to avoid merging, merge is the optimal solution!
   public List getReferencedArchetypes(String archetypeId, String namespace = DEFAULT_NAMESPACE)
   {
       if (!this.referencedArchetypes[namespace]) return null
      return this.referencedArchetypes[namespace][archetypeId] // can be null!
   }

   // done to avoid merging, merge is the optimal solution!
   public String getText(String archetypeId, String code, String lang, String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return null
      if (!this.referencedArchetypes[namespace][archetypeId]) return null

      def t
      for (arch in this.referencedArchetypes[namespace][archetypeId])
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
   public String getDescription(String archetypeId, String code, String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return null
      if (!this.referencedArchetypes[namespace][archetypeId]) return null

      def d
      for (arch in this.referencedArchetypes[namespace][archetypeId])
      {
         d = arch.getDescription(code)
         if (d) break
      }

      return d // can be null
   }

   // done to avoid merging, merge is the optimal solution!
   // founds the first archetype node that matches with the archid+path, and there can be
   // many nodes at that location, since different OPTs might have the same node but with
   // more or less constraints.
   public Constraint getNode(String archetypeId, String path, String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return null
      if (!this.referencedArchetypes[namespace][archetypeId]) return null

      // root path is checked here because arch.getNode() doesnt include itself in
      // the node list, and / matches the root.
      if (path == '/')
      {
         return this.referencedArchetypes[namespace][archetypeId][0] // can be null
      }

      def n
      for (arch in this.referencedArchetypes[namespace][archetypeId])
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
   public List<ObjectNode> getNodes(String archetypeId, String path, String namespace = DEFAULT_NAMESPACE)
   {
      List<ObjectNode> res = []
      if (!this.referencedArchetypes[namespace]) return res
      if (!this.referencedArchetypes[namespace][archetypeId]) return res

      if (path == '/')
      {
         return this.referencedArchetypes[namespace][archetypeId] // can be empty
      }

      def n
      for (arch in this.referencedArchetypes[namespace][archetypeId])
      {
         n = arch.getNode(path)
         if (n) res << n
      }

      return res
   }

   @Synchronized
   public void unloadAll(String namespace = DEFAULT_NAMESPACE)
   {
      if (this.cache[namespace]) // just in case an unload is called before loading, maps doesnt exist and throws exception.
      {
         this.cache[namespace].clear()
         this.timestamps[namespace].clear()
         this.referencedArchetypes[namespace].clear()
      }
   }
}
