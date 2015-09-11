package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.meta.ComponentAdapter
import com.readytalk.util.ClosureGlue
import groovy.transform.TypeChecked

/**
 * Contains everything needed to generate a job from the component dsl blocks
 * DslDelegate is a callback to generate the appropriate delegate to generate the components with
 */
class ItemSource {
  //TODO: Keep context confined to ItemSource (remove from ModelElement classes)
  //TODO: We're hitting conflicts between vanilla and injected context - should we always inject context?
  @Delegate ModelContext itemContext
  Set<AbstractComponentType> components
  Closure<ContextProxy> proxyMaker

  ItemSource(TypeRegistry registry, Set<String> components, ModelContext itemContext) {
    this.components = components.collect{ String componentName -> registry.lookup(componentName) }.toSet()
    this.itemContext = itemContext
    this.proxyMaker = ContextProxy.metaClass.&invokeConstructor.curry(registry)
  }

  //This constructor *must* be used when generating multiple jobs using post-processing components
  //Otherwise you could end up with undefined ordering problems
  ItemSource(ItemSource origin, boolean copy = true) {
    this.itemContext = origin.itemContext.childContext()
    this.proxyMaker = origin.proxyMaker

    if(copy) {
      this.components = origin.components.asImmutable()
    } else {
      throw new UnsupportedOperationException("Multi-job post-processing with non-homogenous components not currently possible")
    }
  }

  def generateWith(DslDelegate delegateGenerator) {
    def item = delegateGenerator.create(this)

    prioritizedComponents().each { AbstractComponentType component ->
      ContextLookup local = component.composeAdapter().injectContext(itemContext).getContext()
      Closure config = (Closure)component.getDslConfig().clone()
      config.setDelegate(item)
      config.resolveStrategy = Closure.DELEGATE_FIRST
      config.call(proxyMaker(local).generate(component.getName()))
    }

    return item
  }

  def prioritizedComponents() {
    return components.asList().sort { a, b -> a.getPriority() <=> b.getPriority() }
  }

  String getItemName() {
    return context.lookup('base','name').get()
  }

  void setItemName(String name) {
    user.bind('base','name',name)
  }

  def lookupValue(String namespace, String field, ContextLookup lookupContext = context) {
    return proxyMaker(lookupContext).generate(namespace).getProperty(field)
  }
}

interface DslDelegate {
  def create(ItemSource)
}

