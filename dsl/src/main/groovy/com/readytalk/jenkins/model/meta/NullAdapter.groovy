package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

//Does nothing - use as unit/identity function
@Singleton
final class NullAdapter implements ComponentAdapter {
  ModelContext injectContext(ModelContext itemContext) {
    return itemContext
  }

  ItemSource injectItem(ItemSource item) {
    return item
  }
}
