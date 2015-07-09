package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.ComponentTrait

/**
 * Contains everything needed to generate a job from the component dsl blocks
 * DslDelegate is a callback to generate the appropriate delegate to generate the components with
 */
class ItemSource {
  //TODO: Keep context confined to ItemSource (remove from ModelElement classes)
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
      this.itemName = origin.itemName
      this.components = origin.components.asImmutable()
    } else {
      throw new UnsupportedOperationException("Multi-job post-processing with non-homogenous components not yet supported")
    }
  }

  def generateWith(DslDelegate delegateGenerator) {
    def item = delegateGenerator.create(this)
    prioritizedComponents().each { AbstractComponentType component ->
      def localContext = getContext()
      if(component instanceof ComponentTrait) {
        localContext = component.injectContext(itemContext)
      }

      Closure config = component.getDslConfig().clone()
      config.setDelegate(item)
      config.resolveStrategy = Closure.DELEGATE_FIRST
      config.call(proxyMaker(localContext).generate(component.getName()))
    }
    return item
  }

  def prioritizedComponents() {
    return components.asList().sort { a, b -> a.getPriority() <=> b.getPriority() }
  }

  String getItemName() {
    return context.lookup('base','name')
  }

  void setItemName(String name) {
    user.bind('base','name',name)
  }

  def lookup(String namespace, String field, ContextLookup lookupContext = context) {
    return proxyMaker(lookupContext).generate(namespace)."${field}"
  }
}

interface DslDelegate {
  def create(JobSource)
}

