package com.readytalk.jenkins.model.meta

import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.ItemSource

//@SelfType(AbstractComponentType) //TODO: Requires Groovy 2.4.x+
trait AggregateField implements ComponentTrait {
  abstract String getAggregateField()
  abstract Map<String,?> getFields()
  boolean shouldInherit(ItemSource item) { true }

  //Aggregate values from all visible scopes for the given field in the implementing component
  //NOTE: This method is required to be idempotent
  ItemSource injectItem(ItemSource item) {
    if(shouldInherit(item)) {
      String componentName = this.getName()
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

      def localScope = item.lookup(componentName, field) ?: [:]
      item.user.bind(componentName, field, defaultsMap + userMap + localScope)
    }

    return super.injectItem(item)
  }
}