package com.cabolabs.openehr.opt.model

/**
 * This class is used to represent the output of the API when listing templates,
 * it just includes basic information about the template.
 */
class OperationalTemplateSummary {

   String templateId
   String concept
   String archetypeId
   String created_timestamp
   String language
   String semver
   String uid
}