package com.readytalk.jenkins.model.visitors

import com.readytalk.jenkins.model.*
import com.readytalk.jenkins.model.meta.ComponentAdapter
import com.readytalk.jenkins.model.pipelines.AbstractPipeline
import com.readytalk.util.ClosureGlue

//Actually evaluate the model dsl
//Stacks are mainly for clarity - every ContextMap instance has a reference to its parent already
class ModelEvaluator extends SymmetricVisitor {
  final TypeRegistry registry
  final List<ItemSource> itemSet = []

  //MUTABLE
  private final Stack<ModelContext> contextStack = new Stack<ModelContext>()
  private List<ItemSource> pipelineSet = null

  static List<ItemSource> evaluateTree(TypeRegistry registry, GroupModelElement root, ScopeConstructor scoping = null) {
    (scoping ?: new ScopeConstructor(registry)).visit(root)
    def evaluator = new ModelEvaluator(registry)
    evaluator.visit(root)
    return processItems(evaluator.itemSet)
  }

  //Each component's post-process method is evaluated exactly once against each job
  //As long as post-process methods are independent, ordering doesn't matter
  //NOTE: post-process methods can generate multiple jobs, but only if the jobs have the same component set
  static List<ItemSource> processItems(List<ItemSource> itemList) {
    return itemList.collect { ItemSource source ->
      ItemSource injected = ClosureGlue.monadicFold(ComponentAdapter,
              source.components.collect { it.getTraits() }.flatten()).injectItem(source)
      return source.components.inject([injected]) { itemsSoFar, AbstractComponentType component ->
        itemsSoFar.collect { ItemSource item ->
          component.postProcess(item)
        }.flatten()
      }
    }.flatten()
  }

  //TODO: Use injection
  ModelEvaluator(TypeRegistry registry) {
    this.registry = registry
  }

  ContextMap getContext() {
    return contextStack.peek().user
  }

  ContextMap getDefaultContext() {
    return contextStack.peek().defaults
  }

  def enter(ScopedModelElement element) {
    contextStack.push(element.modelContext)
  }

  def exit(ScopedModelElement element) {
    contextStack.pop()
  }

  //Override standard control flow to ensure component model elements are evaluated first to build up context
  void recurse(ScopedModelElement element) {
    element.getElements().findAll { it instanceof ComponentModelElement }.each { visit(it) }
    element.getElements().findAll { !(it instanceof ComponentModelElement) }.each { visit(it) }
  }

  //JOB NODE
  def enter(ItemModelElement itemElement) {
    enter((ScopedModelElement)itemElement)
    //TODO: Should this be defaults instead?
    context.bind('base', 'name', itemElement.name)
  }

  //Override to preserve correct ordering - components and job defaults should be evaluated first
  void recurse(ItemModelElement itemElement) {
    itemElement.getElements().findAll { it instanceof ComponentModelElement }.each { visit(it) }

    //Binds job-level default values (overrides global component defaults, but not user-supplied values)
    def genProxy = ContextProxy.generator(registry)
    ProxyDelegate jobDefaultsProxy = genProxy(itemElement.context, defaultContext)

    Closure defaults = itemElement.type.defaults.clone()
    defaults.setDelegate(jobDefaultsProxy)
    defaults.resolveStrategy = Closure.DELEGATE_FIRST
    defaults.call(jobDefaultsProxy)
    ItemSource source = new ItemSource(registry, itemElement.getComponents(), itemElement.modelContext)
    itemSet.add(source)
    if(pipelineSet != null) pipelineSet.add(source)

    itemElement.getElements().findAll { !(it instanceof ComponentModelElement) }.each { visit(it) }
  }

  //COMPONENT NODES
  //Overrides visit because it's a leaf node and we don't want to modify the context stack in component nodes
  def visit(ComponentModelElement element) {
    element.fields.each { String key, value ->
      context.bind(element.type.getName(), key, value)
    }
  }

  //PIPELINE
  void recurse(PipelineModelElement element) {
    pipelineSet = []
    recurse((ScopedModelElement)element)
    AbstractPipeline pipeline = element.getType().implementation.newInstance(element.project)
    pipeline.configure(pipelineSet)
    pipelineSet = null
  }
}

