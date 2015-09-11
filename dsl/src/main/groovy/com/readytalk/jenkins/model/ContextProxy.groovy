package com.readytalk.jenkins.model

import com.readytalk.util.StringUtils
import groovy.text.GStringTemplateEngine
import groovy.transform.TupleConstructor

//TODO: Use exceptions for fallback / value not found instead of null

/**
 * Enables getting and setting values directly in type definitions
 * e.g. vars.team instead of vars.lookup('ownership', 'team')
 * e.g.
 *     gradle {
 *       tasks 'integTest'
 *       flags '--info'
 *     }
 *     git.watch 'master', 'tags'
 *
 * instead of:
 *     bind('gradle', 'tasks', 'integTest')
 *     bind('gradle', 'flags', '--info')
 *     bind('git', 'watch', ['master', 'tags'])
 */
@TupleConstructor
class ContextProxy {
  TypeRegistry registry
  ContextLookup getter
  ContextBind setter = { String name, value ->
    throw new UnsupportedOperationException("No binding method set, context is readonly (property: '${name}')")
  }

  private static unwrap = { Optional box -> box.orElse(null) }

  //Syntactic sugar helper for generating proxies
  static Closure<ProxyDelegate> generator(TypeRegistry registry) {
    return ContextProxy.metaClass.&invokeConstructor.curry(registry) >> { it.generate() }
  }

  /**
   * Context proxy generator
   * @param namespace - default component name
   * @return Returns proxy for looking up values in the correct scope
   */
  ProxyDelegate generate(String namespace = '', Closure getterLambda = null) {
    return new ProxyDelegate(
            proxy: [get: getterLambda ?: getter.&lookup.curry(namespace) >> unwrap,
                    set: setter.&bind.curry(namespace)] as PropertyProxy,
            fallback: { String name, PropertyProxy px ->
              if(name != null && registry.lookup(name) != null) {
                return this.generate(name)
              } else {
                return null
              }
            }
    )
  }
}

@TupleConstructor
class TemplateStr {
  @Delegate String _value
  String expandWith(ProxyDelegate proxy) {
    def map = [:].withDefault { String key ->
      proxy.getProperty(key)
    }
    return new GStringTemplateEngine().createTemplate(this._value).make(map).toString()
  }
}

interface PropertyProxy {
  def get(String name)
  def set(String name, value)
}

//Internal wrapper class to expand templates that belong to fields that are Maps
@TupleConstructor
class TemplateMap {
  @Delegate Map _value
  ProxyDelegate proxy

  def getProperty(String key) {
    get(key)
  }

  def get(String key) {
    def val = this._value.get(key)
    return val instanceof TemplateStr ? val.expandWith(proxy) : val
  }

  def each(Closure iter) {
    _value.keySet().each { String key ->
      iter(key, get(key))
    }
  }
}

/**
 * General-purpose proxy object
 *   Uses provided PropertyProxy for basic set/get operations
 *   Optional fallback to generate a new proxy or alternative value
 */
@TupleConstructor
class ProxyDelegate {
  PropertyProxy proxy
  Closure fallback = { String _, PropertyProxy px -> null }

  def propertyMissing(String name, value) {
    proxy.set(name, value)
  }

  def propertyMissing(String name) {
    if(proxy == null) return null
    def value = proxy.get(name)
    if(value != null) {
      //NOTE: templating is non-recursive
      if(value instanceof Map) {
        return new TemplateMap(value, this)
      }
      return value instanceof TemplateStr ? value.expandWith(this) : value
    } else {
      return fallback(name, proxy)
    }
  }

  //Syntactic sugar to support DSL-style mapping in the type definition blocks
  def methodMissing(String name, args) {
    if(args.size() == 1) {
      if(args.last() instanceof Closure) {
        Closure next = args.last()
        next.delegate = this.getProperty(name)
        next.resolveStrategy = Closure.DELEGATE_FIRST
        next.call()
        return
      } else {
        proxy.set(name, args.last())
        return
      }
    }
    throw new UnsupportedOperationException("Syntax error in closure-style setter for ${name} with ${args}")
  }
}
