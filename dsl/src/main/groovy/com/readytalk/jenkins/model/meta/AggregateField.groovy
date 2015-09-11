package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.ContextAlreadyBoundException
import com.readytalk.jenkins.model.ItemSource

abstract class AggregateField extends AbstractComponentAdapter {
  abstract String getAggregateField()
  boolean shouldInherit(ItemSource item) { true }

  //Aggregate values from all visible scopes for the given field in the implementing component
  //NOTE: This method is required to be idempotent
  ItemSource injectItem(ItemSource item) {
    if(shouldInherit(item)) {
      String componentName = getName()
      String field = getAggregateField()

      //TODO: Should we block upward aggregation if an intermediate context has inherit = false?
      Closure aggregate = { ContextMap context ->
        def iter = context
        def map = [:]
        while(iter != null && iter instanceof ContextMap) {
          map = (iter.context.get(componentName)?.get(field) ?: [:]) + map
          iter = iter.parent
        }
        return map
      }
      def defaultsMap = aggregate(item.defaults)
      def userMap = aggregate(item.user)

      def localScope = item.lookupValue(componentName, field) ?: [:]
      try {
        item.user.bind(componentName, field, defaultsMap + userMap + localScope)
      } catch(ContextAlreadyBoundException e) {
        //Ignore - the point of this adapter specifically requires we bend the scoping rules a bit
      }
    }
    return item
  }
}
