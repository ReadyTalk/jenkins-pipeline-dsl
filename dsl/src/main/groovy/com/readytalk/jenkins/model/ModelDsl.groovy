package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.visitors.*
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import groovy.transform.TypeChecked
import javaposse.jobdsl.dsl.*

@CompileStatic
interface ModelDslMethods {
  void types(Closure typesBlock)
  void model(Closure modelBlock)
}

@CompileStatic
class ModelDsl implements ModelDslMethods {
  TypeRegistryMap registry
  JobParent jp
  ContextLookup defaultLookup

  ModelDsl(JobParent jp, TypeRegistryMap registry = TypeRegistryMap.getDefaultTypes()) {
    this.jp = jp
    this.registry = registry
    this.defaultLookup = new DefaultContext(registry)
  }

  protected static def itemCreator(JobParent jp, ItemSource source) {
    String type = source.context.lookup('base','type').get()
    return jp.invokeMethod(type, [source.itemName, {}])
  }

  void types(Closure typesBlock) {
    registry.merge(TypeDsl.evaluate(registry, typesBlock))
  }

  void defaults(Closure defaultsBlock) {
    def defaultsTree = parse(defaultsBlock)
    def scoping = new ScopeConstructor(this.defaultLookup, new ContextMap())
    ModelEvaluator.evaluateTree(registry, defaultsTree, scoping)
    this.defaultLookup = ContextMap.mergeLookups(defaultsTree.user, this.defaultLookup)
  }

  GroupModelElement parse(Closure modelBlock) {
    return GroupParser.parse(registry, modelBlock)
  }

  GroupModelElement parse(GroupModelElement parent, Closure modelBlock) {
    parent.elements.add(GroupParser.parse(registry, modelBlock))
    return parent
  }

  void generate(Closure modelBlock) {
    generate(parse(modelBlock))
  }

  void generate(GroupModelElement tree) {
    if(System.getProperty('ModelDSLPrettyPrint')) {
      new ModelPrettyPrinter().visit(tree)
    }

    ModelValidator.evaluate(registry, tree)

    def scoping = new ScopeConstructor(defaultLookup, new ContextMap())
    List<ItemSource> jobList = ModelEvaluator.evaluateTree(registry, tree, scoping)

    //TODO: Make this optional / flaggable
    ModelPrettyPrinter.printItemList(jobList)

    jobList.each {
      it.generateWith((ModelDsl.&itemCreator.curry(jp)) as DslDelegate)
    }
  }

  void model(Closure modelBlock) {
    generate(modelBlock)
  }
}

@Category(JobParent)
class ModelDslProxy {
  void model(Closure modelBlock) {
    getModel().model(modelBlock)
  }

  void types(Closure typeBlock) {
    getModel().types(typeBlock)
  }

  def getModel() {
    if(!this.hasProperty('modelDsl') || this.modelDsl == null) {
      this.metaClass.modelDsl = new ModelDsl(this)
    } else {
      return this.modelDsl
    }
  }
}
