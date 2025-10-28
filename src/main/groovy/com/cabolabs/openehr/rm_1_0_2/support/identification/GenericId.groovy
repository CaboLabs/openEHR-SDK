package com.cabolabs.openehr.rm_1_0_2.support.identification

class GenericId extends ObjectId {

   String scheme

   @Override
   ObjectId clone()
   {
      def other = new GenericId(this.value)
      other.scheme = this.scheme
      return other
   }
}
