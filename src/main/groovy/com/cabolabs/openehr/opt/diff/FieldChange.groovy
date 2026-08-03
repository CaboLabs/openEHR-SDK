package com.cabolabs.openehr.opt.diff

// a single scalar constraint field that differs between the two compared nodes
class FieldChange {
   String field
   def oldValue
   def newValue
}
