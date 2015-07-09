package com.readytalk.util

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
}
