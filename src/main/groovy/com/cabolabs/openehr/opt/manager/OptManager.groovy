package com.cabolabs.openehr.opt.manager

import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.OperationalTemplate
// import org.apache.logging.log4j.Logger
// import org.apache.logging.log4j.LogManager
import groovy.transform.Synchronized
import groovy.time.TimeCategory
import groovy.time.TimeDuration

@groovy.util.logging.Slf4j
class OptManager {

   //private Logger log = LogManager.getLogger(getClass())

   OptRepository repo
   int ttl_seconds = 1800 // 30 min in seconds

   // ns will be used as folder name where OPTs are separated in the repo
   // most OS have a file name limit of 255, so that should be the limit of the ns size
   public static String DEFAULT_NAMESPACE = 'com.cabolabs.openehr_opt.namespaces.default'

   // [namespace -> [optid -> OPT]]
   private static Map<String, Map<String, OperationalTemplate>> cache = [:]

   // otpid => timestamp de cuando fue usado por ultima vez.
   // Sirve para saber si un arquetipo no fue utilizado por mucho tiempo, y bajarlo del cache par optimizar espacio en memoria.
   //private static Map<String, Date> timestamps = [:]
   private static Map<String, Map<String, Date>> timestamps = [:] // [namespace -> [optid -> date]]

   // Archetypes referenced by all the templates loaded
   // The list of archetype roots has more than one item when the same archetype is
   // referenced from different OPTs
   // namespace -> [archId -> [arch roots]]
   private static Map<String, Map<String, List<ObjectNode>>> referencedArchetypes = [:]

   // SINGLETON
   private static OptManager instance = null


   //private OptManager(String repoPath)
   private OptManager()
   {

   }

   //public static OptManager getInstance(String repoPath)
   public static OptManager getInstance()
   {
      if (!instance) instance = new OptManager()
      return instance
   }

   public void init(OptRepository repo, int ttl_seconds = 1800)
   {
      this.repo = repo
      this.ttl_seconds = ttl_seconds
   }


   // returns the cache status in a displayable string
   public String status()
   {
      String out = "Template cache status:\n"
      //this.cache[namespace][opt.templateId] = opt
      this.cache.each { namespace, template_id_opt ->
         out += "  namespace: ${namespace} has ${template_id_opt.size()} templates loaded\n"
         template_id_opt.each { template_id, opt ->
            out += "    template_id: ${template_id}\n"
         }
      }

      return out
   }

   @Synchronized
   public void loadAll(String namespace = DEFAULT_NAMESPACE, boolean complete = false)
   {
      if (!repo) throw new Exception("Please initialize the OPT repository by calling init()")

      def opt
      def parser = new OperationalTemplateParser(true)

      def opts = this.repo.getAllOptKeysAndContents(namespace)
      opts.each { location, text ->

         try
         {
            opt = parser.parse(text)
         }
         catch (Exception e)
         {
            log.error("OPT could not be loaded from "+ location)
            log.error(parser.getLastErrors().toString())
         }

         if (opt)
         {
            if (complete) opt.complete()

            log.debug("Loading OPT: " + opt.templateId)

            if (!this.cache[namespace]) this.cache[namespace] = [:]
            if (!this.timestamps[namespace]) this.timestamps[namespace] = [:]

            this.cache[namespace][opt.templateId] = opt
            this.timestamps[namespace][opt.templateId] = new Date()
         }
         else
         {
            log.error("OPT could not be loaded from "+ location)
            log.error(parser.getLastErrors().toString())
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
            if (!this.referencedArchetypes[namespace])
            {
               this.referencedArchetypes[namespace] = [:]
            }

            if (!this.referencedArchetypes[namespace][_objectNode.archetypeId])
            {
               this.referencedArchetypes[namespace][_objectNode.archetypeId] = []
            }

            // avoids to load object nodes from the same template twice
            if (!this.referencedArchetypes[namespace][_objectNode.archetypeId].any { it.templatePath == _objectNode.templatePath })
            {
               this.referencedArchetypes[namespace][_objectNode.archetypeId] << _objectNode
            }
         }
      }
   }

   /*
    * Loads one template in the cache.
    */
   @Synchronized
   public void load(String templateId, String namespace = DEFAULT_NAMESPACE, boolean complete = false)
   {
      if (!repo) throw new Exception("Please initialize the OPT repository by calling init()")


      def text
      try
      {
         text = this.repo.getOptContentsByTemplateId(templateId, namespace)
      }
      catch (Exception e)
      {
         throw new Exception("There was a problem reading the template from the repo", e)
      }

      if (!text)
      {
         throw new Exception("OPT not found "+ templateId)
      }

      def parser = new OperationalTemplateParser()
      def opt = parser.parse(text)

      if (opt)
      {
         if (complete) opt.complete()

         log.debug("Loading OPT: ${templateId} internally has the template_id: ${opt.templateId}")

         if (!this.cache[namespace]) this.cache[namespace] = [:]
         if (!this.timestamps[namespace]) this.timestamps[namespace] = [:]

         // this is indexed with the templateId param passed, not with the loaded opt.templateId which could be not normalized, the templateId param is normalized by the client
         this.cache[namespace][templateId] = opt
         this.timestamps[namespace][templateId] = new Date()

         // set referencedArchetypes
         def refarchs = opt.getReferencedArchetypes()
         refarchs.each { _objectNode ->
            // If the archertype is referenced twice by different opts, it is overwritten,
            // the objectnodes can have different structures since one OPT might have all
            // the nodes and another that uses the SAME archetype might have a couple.
            // To avoid that issue, we save here all the references to the same archetype.
            // A better solution to reduce the memory use, is to merge all the internal
            // structures into one complete structure. (TODO)
            // TODO: do not add the reference twice for the same OPT (check if this case can happen)
            if (!this.referencedArchetypes[namespace])
            {
               this.referencedArchetypes[namespace] = [:]
            }

            if (!this.referencedArchetypes[namespace][_objectNode.archetypeId])
            {
               this.referencedArchetypes[namespace][_objectNode.archetypeId] = []
            }

            // avoids to load object nodes from the same template twice
            if (!this.referencedArchetypes[namespace][_objectNode.archetypeId].any { it.templatePath == _objectNode.templatePath })
            {
               this.referencedArchetypes[namespace][_objectNode.archetypeId] << _objectNode
            }
         }
      }
      else
      {
         throw new Exception("OPT could not be loaded "+ templateId)
      }
   }

   public boolean existsOpt(String templateId, String namespace = DEFAULT_NAMESPACE)
   {
      if (!repo) throw new Exception("Please initialize the OPT repository by calling init()")

      return this.repo.existsOpt(templateId, namespace)
   }

   /**
    * templateId identifier of the OPT that is requested
    * namespace from where the manager will try to load the template
    * filename associated with the template, if not present, it is the templateId with .opt extension,
    *          this is because external systems might assign a custom filename for OPTs that is not the templateId.
    */
   public OperationalTemplate getOpt(String templateId, String namespace = DEFAULT_NAMESPACE, String filename = null)
   {
      if (!repo) throw new Exception("Please initialize the OPT repository by calling init()")

      // cache hit?
      if (this.cache[namespace] && this.cache[namespace][templateId])
      {
         this.timestamps[namespace][templateId] = new Date() // actualizo timestamp
         return this.cache[namespace][templateId]
      }

      load(templateId, namespace)

      /*
      def text = this.repo.getOptContentsByTemplateId(templateId, namespace)
      if (!text)
      {
         throw new Exception("OPT not found "+ templateId)
      }

      def parser = new OperationalTemplateParser()
      def opt = parser.parse( text )

      if (opt)
      {
         log.debug("Loading OPT: " + opt.templateId)

         if (!this.cache[namespace]) this.cache[namespace] = [:]
         if (!this.timestamps[namespace]) this.timestamps[namespace] = [:]

         this.cache[namespace][opt.templateId] = opt
         this.timestamps[namespace][opt.templateId] = new Date()
      }
      else
      {
         throw new Exception("OPT could not be loaded "+ templateId)
      }
      */

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

   /*
   public List getNodesByTemplateDataPath(String templateDataPath, String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return null
      if (!this.referencedArchetypes[namespace][archetypeId]) return null

      def res = []

      // can have many roots for the same archtypeId if the same arch is
      // referenced from many OPTs.
      this.referencedArchetypes[namespace][archetypeId].each { arch ->

         // check the dataPath of the nodes in the arch, and add it to the result
         // .values because findAll resturns a map
         res.addAll( arch.nodes.findAll{ it.value.templateDataPath == templateDataPath }.values() )
      }

      return res
   }
   */

   public List getNodesByDataPath(String archetypeId, String dataPath, String namespace = DEFAULT_NAMESPACE)
   {
      if (!this.referencedArchetypes[namespace]) return null
      if (!this.referencedArchetypes[namespace][archetypeId]) return null

      def res = []

      // can have many roots for the same archtypeId if the same arch is
      // referenced from many OPTs.
      this.referencedArchetypes[namespace][archetypeId].each { arch ->

         // arch.nodes is a map of atchetype_path => list of nodes

         // check the dataPath of the nodes in the arch, and add it to the result
         // .values because findAll resturns a map
         res.addAll( arch.nodes.values().flatten().findAll{ it.dataPath == dataPath } )
      }

      return res
   }

   // done to avoid merging, merge is the optimal solution!
   // founds the first archetype node that matches with the archid+path, and there can be
   // many nodes at that location, since different OPTs might have the same node but with
   // more or less constraints.
   /*
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
   */

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

      def nodes
      for (arch in this.referencedArchetypes[namespace][archetypeId])
      {
         nodes = arch.getNodes(path)
         if (nodes) res.addAll(nodes)
      }

      return res
   }

   @Synchronized
   public void unloadAll(String namespace = DEFAULT_NAMESPACE)
   {
      if (this.cache[namespace]) // just in case an unload is called before loading, maps doesnt exist and throws exception.
      {
         // TODO: sometimes we get 'Cannot invoke method clear() on null object' from here, not sure which one is null, needs more testing, added the ? just in case.
         this.cache[namespace]?.clear()
         this.timestamps[namespace]?.clear()
         this.referencedArchetypes[namespace]?.clear()
      }
   }

   @Synchronized
   public void removeOpt(String templateId, String namespace = DEFAULT_NAMESPACE)
   {
      if (this.cache[namespace] && this.cache[namespace][templateId])
      {
         this.cache[namespace].remove(templateId)
         this.timestamps[namespace].remove(templateId)
      }

      // TODO: it is not checking for referenced archetypes that might depend only
      // on this OPT and should also be removed from this.referencedArchetypes[namespace]
   }

   @Synchronized
   public static void reset()
   {
      this.instance = null
   }

   /**
    * cleans the cache from items used more than ttl_seconds ago, this is called from an
    * external client with certain frequency, for instance from a cron job.
    */
   @Synchronized
   public void cleanCache()
   {
      def now = new Date()

      def toBeRemoved = []

      this.timestamps.each { namespace, templateIds ->

         templateIds.each { templateId, timestamp ->

            TimeDuration dur = TimeCategory.minus(now, timestamp)

            if ((dur.toMilliseconds() / 1000) > this.ttl_seconds)
            {
               // should avoid removeing this because of concurrent modification exception
               //this.removeOpt(templateId, namespace)
               toBeRemoved << [templateId: templateId, namespace: namespace]
            }
         }
      }

      toBeRemoved.each { item->
         this.removeOpt(item.templateId, item.namespace)
      }
   }

   /**
    * Empties all caches from all namespaces, useful for testing.
    */
   @Synchronized
   public void cleanAll()
   {
      this.cache.clear()
      this.timestamps.clear()
      this.referencedArchetypes.clear()
   }
}
