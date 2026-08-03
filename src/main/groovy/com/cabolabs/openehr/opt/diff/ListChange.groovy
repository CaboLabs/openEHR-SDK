package com.cabolabs.openehr.opt.diff

// diff of a list-shaped constraint, e.g. CCodePhrase.codeList, CDvOrdinal.list, CDvQuantity.list, CInteger/CString.list
class ListChange {
   String field
   List added = []
   List removed = []
   List<ListItemChange> modified = []
}
