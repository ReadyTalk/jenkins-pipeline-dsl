package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext

//This is good example of why the meta component traits are useful
//Sure, it's trivial to rebind the job's base type... but this allows me to validate it
//In other words, I can ensure that it correctly aborts if two components will conflict!
abstract class JobTypeOverride extends AbstractComponentAdapter {
  abstract String getJobType()

  ModelContext injectContext(ModelContext itemContext) {
    itemContext.user.bind('base', 'type', getJobType())
    return itemContext
  }

  //No alterations - this just a validation check
  ItemSource injectItem(ItemSource item) {
    def conflicts = item.components.collect { AbstractComponentType component ->
      component.traits.findAll {
        it instanceof JobTypeOverride && it.getJobType() != this.getJobType()
      }.collect { "${component.getName()}->${it.getJobType()}" }
    }.flatten().join(', ')
    if(conflicts != '') {
      throw new RuntimeException("Components [${getName()}->${getJobType()}" +
              ", ${conflicts}] require incompatible job types, aborting!")
    }
    return item
  }
}
