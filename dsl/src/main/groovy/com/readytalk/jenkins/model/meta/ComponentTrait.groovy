package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

/**
 * The injectors are monad-like - they depend on groovy trait chaining to allow gluing injector logic together
 * The injected object *must* be passed to super's inject method, or it won't work correctly
 * i.e.:
 *   return super.injectContext(modifiedContext)
 */
interface ComponentTrait {
  String getName()

  //Allows altering itemContext just prior to component execution (so after all postProcess steps)
  ModelContext injectContext(ModelContext itemContext)

  //Allows altering an item just prior to component postProcess call
  ItemSource injectItem(ItemSource item)
}
