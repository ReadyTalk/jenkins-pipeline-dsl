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
  final TypeRegistry registry
  @Delegate ModelContext itemContext
  Set<AbstractComponentType> components

  ItemSource(TypeRegistry registry, Set<String> components, ModelContext itemContext) {
    this.registry = registry
    this.components = components.collect{ String componentName -> registry.lookup(componentName) }.toSet()
    this.itemContext = itemContext
  }

  //This constructor *must* be used when generating multiple jobs using post-processing components
  //Otherwise you could end up with undefined ordering problems
  ItemSource(ItemSource origin, boolean copy = true) {
    this.itemContext = origin.itemContext.childContext()
    this.registry = origin.registry

    if(copy) {
      this.components = origin.components.asImmutable()
    } else {
      throw new UnsupportedOperationException("Multi-job post-processing with non-homogenous components not currently possible")
    }
  }

  def generateWith(DslDelegate delegateGenerator) {
    def item = delegateGenerator.create(this)

    Map<Integer,List<Closure>> dslBlocks = [:].withDefault {[]}

    components.each { AbstractComponentType component ->
      ContextLookup local = component.composeAdapter().injectContext(itemContext).getContext()
      component.dslBlocks.each { Integer priority, Closure block ->
        Closure config = (Closure)block.clone()
        config.setDelegate(item)
        config.resolveStrategy = Closure.DELEGATE_FIRST
        ProxyDelegate proxy = proxyOf(local).generate(component.getName())
        dslBlocks.get(priority).add(config.curry(proxy))
      }
    }

    dslBlocks.keySet().asList().sort().each { Integer ord ->
      dslBlocks.get(ord).each { it.call() }
    }

    return item
  }

  String getItemName() {
    return context.lookup('base','name').get()
  }

  void setItemName(String name) {
    user.bind('base','name',name)
  }

  //Includes template expansion, but not injectContext() overrides
  def lookupValue(String namespace, String field, ContextLookup lookupContext = context) {
    return proxyOf(lookupContext).generate(namespace).getProperty(field)
  }

  ContextProxy proxyOf(...args) {
    return ContextProxy.metaClass.&invokeConstructor.curry(registry).call(args)
  }

  ContextProxy getProxy() {
    return proxyOf(defaults, context)
  }
}

interface DslDelegate {
  def create(ItemSource)
}

