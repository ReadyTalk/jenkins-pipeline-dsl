package com.readytalk.jenkins.model

import com.readytalk.util.ClosureGlue
import groovy.transform.Canonical
import groovy.transform.Immutable
import groovy.transform.TupleConstructor

interface ContextLookup {
  Optional lookup(String namespace, String field)
}

interface ContextBind {
  void bind(String namespace, String field, value) throws ContextAlreadyBoundException
}

/**
 * In general, attempting to set a value twice in the same context indicates some kind of conflict,
 * so it's an error by default. In controlled contexts, it may be acceptable or desirable and this
 * can be caught and ignored. The point is that it's an intentional choice.
 */
class ContextAlreadyBoundException extends Exception {
  final ContextMap context
  final String componentName
  final String fieldName
  final def value

  ContextAlreadyBoundException(ContextMap context, String componentName, String fieldName, value) {
    super("Component fields should only be set once in a given scope!\n" +
            "  ${componentName}.${fieldName} already set as '" +
            context.lookup(componentName, fieldName).get().toString() +
            "', attempted to set as '${value}'")
    this.context = context
    this.componentName = componentName
    this.fieldName = fieldName
    this.value = value
  }
}

/**
 * Hierarchical key-value store
 * Each model element (except components) have an associated context (aka scope)
 *   Setting values is always done in the current scope
 *   Getting values will delegate to parent (outer) scopes if the value doesn't exist in the local scope
 *     Because getting and setting for the same key might refer to different objects,
 *     the getter attempts to return immutable values
 */
class ContextMap implements ContextLookup, ContextBind {
  final ContextLookup parent
  final Map<String,Map<String,Object>> context

  ContextMap() {
    parent = null
    context = [:]
  }

  ContextMap(ContextLookup parent, context) {
    this.parent = parent
    this.context = context
  }

  static ContextLookup mergeLookups(ContextLookup first, ContextLookup second) {
    return ClosureGlue.fallbackIfEmpty(first.&lookup, second.&lookup) as ContextLookup
  }

  ContextLookup withFallback(ContextLookup fallback) {
    return mergeLookups(this, fallback)
  }

  void bindAppend(ContextLookup withScope, String namespace, String field, value) {
    try {
      this.bind(namespace, field, (withScope.lookup(namespace, field).orElse([:])) + value)
    } catch(ContextAlreadyBoundException e) {
      //Ignore - we're extending (not replacing) an existing value
    }
  }

  void bindPrepend(ContextLookup withScope, String namespace, String field, value) {
    try {
      this.bind(namespace, field, value + (withScope.lookup(namespace, field).orElse([:])))
    } catch(ContextAlreadyBoundException e) {
      //Ignore - we're extending (not replacing) an existing value
    }
  }

  //Check this context for value, then check parent context if possible
  Optional lookup(String componentName, String field) {
    if(componentName == null) return lookup('', field)
    def box
    if(context.get(componentName)?.containsKey(field)) {
      box = Optional.of(context.get(componentName).get(field))
    } else {
      box = parent != null ? parent.lookup(componentName,field) : Optional.empty()
    }

    if(box.present && box.get() instanceof Collection) {
      //This is required to ensure binders can't accidentally modify higher scope defaults for map/list values
      //i.e. 'something.map.put(key,value)' is illegal, but 'something.map = something.map + [key: value]' is okay
      //Not needed for Strings, since those are already immutable by default in the JVM
      return Optional.of(box.get().asImmutable())
    }
    return box
  }

  //TODO: Verify that bound values actually correspond to real component parameters
  //      Would help catch typos and people doing things they shouldn't that won't show up in the pretty printed output
  void bind(String componentName, String key, value) {
    if(value == null) {
      throw new RuntimeException("Attempted to set ${componentName}.${key} to null value, which is not allowed!")
    }
    if(componentName == null) {
      //TODO: should we support blank scope? i.e. named values with no namespace / component name
      bind('', key, value)
    } else {
      if (!context.containsKey(componentName)) {
        context.put(componentName, [:])
      }
      def componentContext = context.get(componentName)
      if(componentContext.containsKey(key)) {
        //This can be caught and ignored in controlled contexts, but normally it's an error
        def err = new ContextAlreadyBoundException(this, componentName, key, value)
        componentContext.put(key, value)
        throw err
      } else {
        componentContext.put(key, value)
      }
    }
  }

  ContextMap createChildContext() {
    return new ContextMap(this, [:])
  }

}

//Special context that always returns default component values
@TupleConstructor
class DefaultContext implements ContextLookup {
  TypeRegistry registry

  Optional lookup(String componentName, String field) {
    Map fieldMap = registry.lookup(componentName)?.fields
    if(fieldMap != null && fieldMap.containsKey(field)) {
      def defaultValue = fieldMap.get(field)
      if(defaultValue != null) {
        return Optional.of(defaultValue)
      } else {
        throw new RuntimeException("Field '${field}' from component '${componentName}' must be set explicitly (has no default value)!")
      }
    } else {
      //Lookup failed - either component or field doesn't exist
      return Optional.empty()
    }
  }
}
