package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.pipelines.AbstractPipeline
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Method
import java.lang.reflect.Modifier

interface TypeRegistry {
  //TODO: Consider renaming this so it doesn't get confused with ContextLookup.lookup
  NamedElementType lookup(String name)
}

interface TypeRegistryBundle {
  //This method is required, but the JVM doesn't treat static methods like normal methods
  //static TypeRegistryMap getTypes()
}

//TODO: This ought to use something Dagger to allow it to be passed around easily once constructed
@CompileStatic
class TypeRegistryMap implements TypeRegistry {
  final Map<String,NamedElementType> types = new HashMap<String,NamedElementType>()

  TypeRegistryMap() {
    types.put('group', GroupType.instance)
  }

  static TypeRegistryMap getDefaultTypes() {
    def map = new TypeRegistryMap()

    //Auto-import from classpath
    Reflections ref = new Reflections('com.readytalk.jenkins')
    ref.getSubTypesOf(NamedElementType).each { Class type ->
      if((type.fields.find {it.name == 'instance'} != null)
              && (type.annotations.find {it.annotationType().name == 'groovy.transform.Immutable'} != null)) {
        map.add((NamedElementType)(type.getMethod('getInstance').invoke(null).asType(type)))
      }
    }

    ref.getSubTypesOf(AbstractPipeline).each { Class type ->
      assert type.getMethods().any { Method method ->
        method.name == 'getType' &&
                Modifier.isStatic(method.getModifiers()) &&
                PipelineType.class.isAssignableFrom(method.returnType)
      }, "Pipeline ${type.getName()} must have method of signature 'static PipelineType getType()'\n\
Suggestion: static PipelineType type = new PipelineType(NAME, ${type.getName()}.class)"
      map.add(type.getMethod('getType').invoke(null).asType(PipelineType))
    }

    ref.getSubTypesOf(TypeRegistryBundle).each { Class type ->
      assert type.getDeclaredMethods().any { Method method ->
        method.name == 'generateTypes' &&
                Modifier.isStatic(method.getModifiers()) &&
                TypeRegistryMap.class.isAssignableFrom(method.returnType)
      }, "TypeRegistryBundle ${type.getName()} must have method of signature 'static TypeRegistryMap generateTypes()'!"
      map.merge(type.getDeclaredMethod('generateTypes', TypeRegistryMap.class).invoke(null, map).asType(TypeRegistryMap))
    }

    return map
  }

  NamedElementType lookup(String name) {
    return types.get(name)
  }

  def merge(TypeRegistryMap typeMap) {
    typeMap.types.values().each { NamedElementType type ->
      add(type)
    }
  }

  def add(NamedElementType type) {
    if(type == null || type.getName() == null) {
      throw new IllegalStateException("Tried to add null type to TypeRegistry")
    } else if(type instanceof AbstractComponentType &&
              (type.getFields() == null || type.getFields().containsKey(null))) {
      throw new IllegalStateException("Tried to add component '${type.getName()}' with null field id!\n" +
              "Fields: ${type.getFields()}")
    }
    String typeName = type.getName()
    types.put(typeName, type)
  }

  Set<AbstractComponentType> getComponents() {
    types.findAll { String typeName, NamedElementType type ->
      type instanceof AbstractComponentType
    }.values().toSet() as Set<AbstractComponentType>
  }

  Set<AbstractComponentType> componentsWithField(String fieldName) {
    components.findAll { AbstractComponentType type ->
      type.fields.containsKey(fieldName)
    }.toSet()
  }
}

class TypeDsl {
  final TypeRegistryMap registry = new TypeRegistryMap()

  TypeDsl(TypeRegistryMap registry) {
    this.registry.merge(registry)
  }

  def component(String name, Map defaults, Closure config) {
    return add(new ComponentType(name, defaults, config))
  }

  def job(String name, List<String> components, Closure defaults = {}) {
    return add(new JobType(name, components, defaults))
  }

  def job(AbstractItemType parent, String name, Closure defaults = {}) {
    job(parent, name, [], defaults)
  }

  def job(AbstractItemType parent, String name, List<String> components, Closure defaults = {}) {
    if(parent == null) {
      throw new RuntimeException("Cannot create job type '${name}' extending from null parent type")
    }
    return add(parent.extendWith(new JobType(name, components, defaults)))
  }

  def job(String parentName, String name, List<String> components, Closure defaults = {}) {
    AbstractItemType parentType = registry.lookup(parentName)
    if(parentType == null) {
      throw new RuntimeException("Cannot inherit from job type ${parentName} because it doesn't exist!")
    }
    return job(parentType, name, components, defaults)
  }

  def add(NamedElementType type) {
    return registry.add(type)
  }

  def getTypes() {
    return new ProxyDelegate([get: this.registry.&lookup] as PropertyProxy)
  }

  static TypeRegistryMap evaluate(Closure typeDef) {
    return evaluate(new TypeRegistryMap(), typeDef)
  }

  static TypeRegistryMap evaluate(TypeRegistryMap registry, Closure typeDef) {
    def typeDsl = new TypeDsl(registry)
    typeDef.delegate = typeDsl
    typeDef.call()
    return typeDsl.registry
  }
}
