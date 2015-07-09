package com.readytalk.util

import groovy.text.GStringTemplateEngine

/**
 * String manipulation utilities
 */
class StringUtils {
  //Guarantee input as list, splitting if needed
  static List<String> asList(value, String delimiter = ' ') {
    if(value instanceof String) {
      return !value.equals('') ? value.split(delimiter) : []
    } else if(isStringList(value)) {
      return value
    } else {
      throw new UnsupportedOperationException("Can't convert from type ${value.getClass()} to list.\n${value}")
    }
  }

  //Guarantee output as string, combining if needed
  static String asString(value, String delimiter = ' ') {
    switch(value) {
      case String:
        return value
      case Number:
        return value.toString()
      case Iterable:
        return value.join(delimiter)
      default:
        return value.toString()
    }
  }

  static boolean isStringList(value) {
    return value instanceof String[] || value instanceof Iterable<String> ||
            (value instanceof Object[] && value.every { it instanceof String })
  }

  static boolean isValueType(value) {
    return value instanceof Number || value instanceof String
  }

  static boolean isValueList(value) {
    return value instanceof String[] || value instanceof Iterable<String> ||
            value instanceof Number[] || value instanceof Iterable<Number> ||
            (value instanceof Object[] && value.every { isValueType(it) })
  }

  static String templateWith(String template, delegate) {
    def map = [:].withDefault { key ->
      delegate."${key}"
    }
    return new GStringTemplateEngine().createTemplate(template).make(map).toString()
  }
}
