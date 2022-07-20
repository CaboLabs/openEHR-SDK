package com.cabolabs.openehr.opt.manager

interface OptRepository {

   /**
    * location is a key used to reference one OPT, could be an absolute file path,
    * or an S3 object key. The namespace is already included in the location as a
    * prefix.
    * Returns null if no OPT was found.
    */
   String getOptContents(String location);

   boolean existsOpt(String location);

   boolean existsOpt(String templateId, String namespace);

   /**
    * Does a search in the namespace and finds the first OPT that matches the templateId.
    * Returns null if no OPT was found.
    */
   String getOptContentsByTemplateId(String templateId, String namespace);

   /**
    * Returns the contents of all the OPTs under the given namespace.
    * Returns an empty list if there are no OPTs.
    */
   List<String> getAllOptContents(String namespace);

   /**
    * Similar to getAllOptContents, but returns the key (location) of each OPT.
    */
   Map<String, String> getAllOptKeysAndContents(String namespace);
}
