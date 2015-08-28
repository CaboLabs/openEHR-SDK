package com.cabolabs.openehr.opt.model

class CodedTerm {

   String code
   Term term
   
   String toString()
   {
      return this.code +'::'+ this.term.toString()
   }
}
