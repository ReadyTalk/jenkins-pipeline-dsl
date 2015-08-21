package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

/**
 * The injection methods are monadic - i.e. they are chained together when run
 *
 * TODO: Figure out how to extend Groovy's compile time type checking to validate
 *       calls to AbstractComponentType, since those calls can normally only be
 *       resolved dynamically. We should be able to guarantee this statically, since
 *       the adapters are only designed to work as anonymous inner classes of components.
 */
interface ComponentAdapter {
  //Allows altering itemContext just prior to component execution (so after all postProcess steps)
  ModelContext injectContext(ModelContext itemContext)

  //Allows altering an item just prior to component postProcess call
  ItemSource injectItem(ItemSource item)
}

//Default case represents the unit operation
abstract class AbstractComponentAdapter implements ComponentAdapter {
  ModelContext injectContext(ModelContext itemContext) {
    return itemContext
  }

  ItemSource injectItem(ItemSource item) {
    return item
  }
}