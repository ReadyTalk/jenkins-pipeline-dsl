package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

/**
 * The injection methods are monadic - i.e. they are chained together when run
 *
 * NOTE: This is an abstraction for handling unusual cases and syntactic sugar.
 *       It should not be used unless there's no other way to cleanly implement
 *       the behavior using the normal methods and interfaces!
 */
interface ComponentAdapter {
  //Alter context just prior to specific component execution (after all postProcess calls)
  //Will *not* affect context provided to other components!
  //PURPOSE: value has special representation within implementing component
  ModelContext injectContext(ModelContext itemContext)

  //Allows altering an item just prior to component postProcess call
  //Affects context visible to *all* other components!
  //PURPOSE: general override like postProcess, but more composable
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