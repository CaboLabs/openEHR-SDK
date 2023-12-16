package com.cabolabs.openehr.query

/**
 * All openEHR query formalisms will inherit from this class.
 */
abstract class OpenEhrQuery {

   String qualifiedName
   String version // semver
}