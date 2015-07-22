package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.types.BaseComponent
import com.readytalk.jenkins.model.types.CommonComponent
import com.readytalk.jenkins.model.visitors.ModelEvaluator
import com.readytalk.jenkins.model.visitors.ScopeConstructor
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

/**
 * Sets up convenient mocks that can be passed closures containing bits of DSL
 * types { ... } - evaluate block as type definitions and add them to registry
 * model { ... } - parses block as new model DSL and returns unevaluated element tree
 * eval { ... }  - parses and evaluates block as new model DSL, returns list of JobSource objects
 * generate { ... } - parses, evaluates, and generates a list of mock upstream Job objects
 */
class ModelSpecification extends Specification {
  TypeRegistryMap registry
  Closure<GroupModelElement> model
  Closure<TypeRegistryMap> types
  Closure<List<ItemSource>> eval
  Closure generate
  Closure itemDefaults

  def setup() {
    this.registry = ModelSpecification.getMockRegistry()
    this.registry.types.put('base', BaseComponent.instance)
    this.model = ModelSpecification.&mockParse.curry(registry)
    this.types = registry.&merge << TypeDsl.&evaluate.curry(registry)

    this.eval = ModelEvaluator.&evaluateTree.curry(registry) << model
    this.generate = { List<ItemSource> jobList ->
      JobManagement jm = Mock(JobManagement)
      JobParent jp = Spy(JobParent)
      jp.jm = jm
      return jobList.collect { ItemSource source ->
        source.generateWith(ModelDsl.&itemCreator.curry(jp) as DslDelegate)
      }
    } << eval
  }

  def itemDefaults(AbstractItemType type, Map<String,?> context) {
    itemDefaults(type, new ContextMap(new DefaultContext(registry), context))
  }

  def itemDefaults(AbstractItemType type, ContextMap context) {
    println "itemDefaults map: ${context}"
    ProxyDelegate jobDefaultsProxy = new ContextProxy(registry, context, context).generate()

    Closure defaults = type.defaults.clone()
    defaults.setDelegate(jobDefaultsProxy)
    defaults.resolveStrategy = Closure.DELEGATE_FIRST
    defaults.call(jobDefaultsProxy)
    return context
  }

  ProxyDelegate autoproxy(ContextMap map) {
    new ContextProxy(registry, map, map).generate()
  }

  ContextMap componentContext(Map userVals = [:]) {
    new ContextMap(new DefaultContext(registry), userVals)
  }

  //Dynamically generates fake jobs and component types based on the name
  static TypeRegistryMap getMockRegistry() {
    return new MockTypeMap()
  }

  static ModelElement mockComponentModel() {
    mockComponentModel(mockRegistry)
  }

  static ModelElement mockParse(TypeRegistry registry, Closure modelBlock) {
    return GroupParser.parse(registry, modelBlock)
  }

  static boolean jobHasStringParameters(Node jobNode, Map parameters) {
    return parameters.inject(true) { cumulative, name, value ->
      return cumulative && (jobNode.properties.'hudson.model.ParametersDefinitionProperty'[0].parameterDefinitions[0].any { param ->
        param.name[0].value() == name && param.defaultValue[0].value() == value
      })
    }
  }

  static class MockTypeMap extends TypeRegistryMap {
    @Override
    NamedElementType lookup(String typeName) {
      print "Mock lookup of '${typeName}': "
      if(typeName.equals('')) {
        println "  returning null for blank type name"
        return null
      }
      if(types.containsKey(typeName)) {
        println "  found named type ${typeName}"
        return types.get(typeName)
      }
      if(typeName.startsWith('c') || typeName.contains('Component')) {
        println "    generating fake component ${typeName}"
        types.put(typeName, new ComponentType(typeName, [field: 'defaultValue'], {}))
        return types.get(typeName)
      } else if(typeName.startsWith('j') || typeName.contains('Job')) {
        println "    generating fake job ${typeName}"
        types.put(typeName, new JobType(typeName, ['base'], {}))
        return types.get(typeName)
      } else {
        println "    can't generate requested fake type ${typeName}"
        return null
      }
    }
  }
}
