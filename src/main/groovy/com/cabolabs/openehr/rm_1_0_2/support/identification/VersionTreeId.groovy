package com.cabolabs.openehr.rm_1_0_2.support.identification

class VersionTreeId {

   static final VERSION_PATTERN = /^(\d+)(?:\.(\d+)\.(\d+))?$/

   String value // e.g. 1.0.2

   VersionTreeId(String value)
   {
      this.value = value
   }

   String getTrunkVersion()
   {
      def matcher = expression =~ VERSION_PATTERN
      return matcher.matches() ? matcher.group(1) : null
   }

   boolean isBranch()
   {
      // true if this version identifier represents a branch, i.e. has branch_number and branch_version parts.
      return getBranchNumber() != null && getBranchVersion() != null
   }

   String getBranchNumber()
   {
      def matcher = expression =~ VERSION_PATTERN
      return (matcher.matches() && matcher.group(2) != null) ? matcher.group(2) : null
   }

   String getBranchVersion()
   {
      def matcher = expression =~ VERSION_PATTERN
      return (matcher.matches() && matcher.group(3) != null) ? matcher.group(3) : null
   }

}
