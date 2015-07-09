package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ContextLookup
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

//TODO: Components can't combine ComponentTraits
trait ComponentTrait {
  abstract String getName()

  //Allows altering itemContext just prior to component execution (so after all postProcess steps)
  ContextLookup injectContext(ModelContext itemContext) {
    return itemContext.getContext()
  }

  //Allows altering an item just prior to component postProcess call
  ItemSource injectItem(ItemSource item) {
    return item
  }
}