package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.ContextAlreadyBoundException
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.TemplateStr
import groovy.text.Template
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import sun.reflect.generics.reflectiveObjects.NotImplementedException

abstract class AggregateField extends AbstractComponentAdapter {
  abstract String getAggregateField()

  boolean shouldInherit(ItemSource item) { true }

  protected def combine(outer, inner) {
    boolean noDefault = false

    //Concatenate raw template string - without this, templates will show up as instance ids
    if(outer instanceof TemplateStr || inner instanceof TemplateStr) {
      String a = outer instanceof TemplateStr ? outer._value : outer
      String b = inner instanceof TemplateStr ? inner._value : inner
      return new TemplateStr(a + b)
    }

    try {
      noDefault = !(outer.respondsTo('plus', inner.class))
    } catch(MethodSelectionException e) {
      //Ignore - if we hit this, it'll still work via dynamic dispatch
    }

    if(!noDefault) {
      return outer.plus(inner)
    } else {
      return plusOverride(outer, inner)
    }
  }

  def plusOverride(outer, inner) {
    throw new UnsupportedOperationException(
            "Cannot automatically combine ${aggregateField} values for types:" +
                    "${outer.class} + ${inner.class}\n" +
                    "Explicit override of AggregateField.combine(outer,inner) required."
    )
  }

  def getUnit() {
    def globalDefault = getFields().get(aggregateField)
    switch(globalDefault) {
      case String:
        return ''
      case Map:
        return [:]
      case Iterable:
        return []
      case Number:
        return 0
      case null:
        throw new IllegalStateException(
                "Global default for aggregate fields cannot be null!\n" +
                "Component: ${getName()}, Field: ${aggregateField}"
        )
      default:
        throw new UnsupportedOperationException(
                "Unit value for type ${globalDefault.class.name}\n"+
                        "Explicit getUnit() implementation required"
       )
    }
  }

  //Aggregate values from all visible scopes for the given field in the implementing component
  //NOTE: This method is required to be idempotent
  ItemSource injectItem(ItemSource item) {
    if(shouldInherit(item)) {
      String componentName = getName()
      String field = getAggregateField()

      //TODO: Should we block upward aggregation if an intermediate context has inherit = false?
      //TODO: This probably won't work with templating
      Closure aggregate = { ContextMap context ->
        def iter = context
        def sum = getUnit()
        while(iter != null && iter instanceof ContextMap) {
          sum = combine((iter.context.get(componentName)?.get(field) ?: getUnit()), sum)
          iter = iter.parent
        }
        return sum
      }

      def defaultsSum = aggregate(item.defaults) ?: getUnit()
      def userSum = aggregate(item.user) ?: getUnit()

      try {
        item.user.bind(componentName, field, combine(defaultsSum, userSum))
      } catch(ContextAlreadyBoundException e) {
        //Ignore - the point of this adapter specifically requires we bend the scoping rules a bit
      }
    }
    return item
  }
}
