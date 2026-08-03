package com.cabolabs.openehr.opt.diff

// a list item that was matched on both sides of a ListChange but has field-level differences,
// e.g. a CDvOrdinalItem matched by 'value' whose 'symbol' changed
class ListItemChange {
   def item // the matching key of the item (e.g. a CQuantityItem.units or a CDvOrdinalItem.value)
   List<FieldChange> changes = []
}
