package com.readytalk.util

import com.readytalk.jenkins.model.PipelineDslException
import com.readytalk.jenkins.model.meta.ComponentAdapter

import java.lang.reflect.Method

/**
 * Function composition utilities
 * NOTE: Consider using Closure<T> to enforce same return type
 */
class ClosureGlue {
  /**
   * Calls a fall-back function if the first function returns null
   * Doesn't memoize in case it needs to be chained again
   */
  static Closure fallbackIfNull(Closure function, Closure fallback) {
    return { Object ...args ->
      def value = function.call(*args)
      return value != null ? value : fallback.call(*args)
    }
  }

  /**
   * Same as fallbackIfNull, but with Optionals instead
   */
  static Closure<Optional> fallbackIfEmpty(Closure<Optional> function, Closure<Optional> fallback) {
    return { Object ...args ->
      Optional box = function.call(*args)
      return box.present ? box : fallback.call(*args)
    }
  }

  /**
   * Combine two void functions while preserving the delegate
   */
  static Closure combinePreservingDelegate(Closure first, Closure second) {
    return { Object... args ->
      first.delegate = delegate
      second.delegate = delegate
      first.resolveStrategy = resolveStrategy
      second.resolveStrategy = resolveStrategy
      first.call(*args)
      second.call(*args)
    }
  }

  /**
   * For a given interface where all methods are of type T method(T),
   * and a list of actions implementing this interface:
   * Fold all actions into a single instance of the interface via composition
   *
   * Return type is an instance of monadicInterface
   *
   * TODO: Split this into smaller pieces - the lift&merge could be a separate function mapped to the list
   */
  static def monadicFold(Class monadicInterface, List actions, identity = { it }) {
    monadicInterface.getMethods().each { Method method ->
      assert method.parameterTypes.size() == 1 &&
             method.returnType == method.parameterTypes.first(),
             "ClosureGlue.monadicFold: interface ${monadicInterface} has " +
                     "methods that do not match pattern 'T METHOD(T)'. Instead found:\n" +
                     "${method.returnType} ${method.name}(${method.parameterTypes.join(', ')})"
    }

    //Unit operation - ensures result is valid even if action list is empty
    Map<String,Closure> unit = monadicInterface.getMethods().collectEntries {
      [(it.name): identity]
    }

    List liftedActions = actions.collect { action ->
      assert monadicInterface.isAssignableFrom(action.getClass())
      monadicInterface.getMethods().collectEntries { Method method ->
        [(method.name): action.&invokeMethod.curry(method.name)]
      }
    }

    def result = liftedActions.inject(unit) { Map<String, Closure> aggregate, Map<String, Closure> action ->
      aggregate.collectEntries { String methodName, Closure cumulative ->
        def next = action.get(methodName)
        [(methodName): cumulative << next]
      }
    }.asType(monadicInterface)

    assert monadicInterface.isAssignableFrom(result.getClass()),
            "ClosureGlue internal bug: monadicFold returned ${result.getClass()}," +
            "which is incompatible with interface ${monadicInterface.getClass()}"
    return result
  }

  /**
   * Create a closure that will execute a string as groovy code against given context object
   * Useful for enabling embedded groovy code snippets
   */
  static Closure executeWith(Object context, String groovyCode) {
    String wrappedCode =
"""Closure __wrappedExecutor = { ${groovyCode} }
__wrappedExecutor.delegate = x
__wrappedExecutor.call(x)
"""
    return {
      Eval.x(context, wrappedCode)
    }
  }

  /**
  /* Same as above, except use returned closure as implicit context
  /* NOTE: is not a "true" closure as it can't directly access calling scope (i.e. owner object is meaningless)
  /*       context can be passed in via the delegate of the returned closure or via passed parameters
  */
  static Closure asClosure(String groovyCode) {
    return { Object... args ->
      String wrappedCode = """Closure __executor = { ${groovyCode} }
__executor.delegate = x
__executor.resolveStrategy = Closure.DELEGATE_FIRST
__executor.call(*y)"""
      Eval.xy(getDelegate(), args, wrappedCode)
    }
  }

  //Helper function for injecting additional context like filename into dsl errors
  //Can be wrapped multiple times for context from different layers
  static Closure wrapWithErrorContext(Closure scriptClosure, context) {
    return { Object... args ->
      try {
        scriptClosure.delegate = getDelegate()
        scriptClosure.call(*args)
      } catch(PipelineDslException e) {
        println "Pipeline DSL Exception in context: ${context}"
        throw e
      } catch(RuntimeException e) {
        println "RuntimeException from context: ${context}"
        throw e
      }
    }
  }
}
