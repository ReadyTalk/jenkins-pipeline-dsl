package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ContextLookup
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ModelContext
import com.readytalk.jenkins.model.TemplateStr

//NOTE: should only be used for job components (doesn't even make sense for views)
//@SelfType(AbstractComponentType) //TODO: Requires Groovy 2.4.x+
trait ExternalizedFields implements ComponentTrait {
  /**
   * This method "lifts" a component field into an explicit Jenkins parameter
   * e.g.:
   * someComponent {
   *   environment 'coreX9'
   * }
   * ...will create a jenkins parameter called "ENVIRONMENT" that will default to the value of 'coreX9'
   * Moreover, it rebinds the dsl field value as '${ENVIRONMENT}', so the dslConfig block doesn't need any special handling
   * as long as the field is used as a plain String value
   *
   */
  abstract Map<String,String> getExternalizedFields()

  //Call from post-process step
  ItemSource injectItem(ItemSource item) {
    ItemSource result = item
    getExternalizedFields().each { String field, String paramName ->
      item = externalizeFieldAs(item, field, paramName ?: paramName.toUpperCase())
    }
    return [result]
  }

  //Shadows the value of externalized fields within the component execution only
  //Ensures order-independence for other code that may read this field value and should get the real value, not ${FIELD}
  ContextLookup injectContext(ModelContext itemContext) {
    def local = itemContext.user.createChildContext()
    getExternalizedFields().each { String field, String paramName ->
      local.bind(this.getName(), field, "\${${paramName ?: paramName.toUpperCase()}}".toString())
    }
    return local.withFallback(itemContext.defaults)
  }

  ItemSource externalizeField(ItemSource item, String field) {
    externalizeFieldAs(item, field, field.toUpperCase())
  }

  ItemSource externalizeFieldAs(ItemSource item, String field, String paramName) {
    String componentName = this.getName()
    def defaultValue = item.lookup(componentName, field)

    //Can't auto-add here as it would break the iterator invariants for postProcess
    assert item.components.contains(ParameterizedComponent.instance), "ExternalizedField trait requires component 'parameterized'"

    //TODO: if value is a list, this could still work if we externalize only the first value in the list
    if(!(defaultValue instanceof String)) {
      throw new RuntimeException("${componentName}.${field}: Externalized fields only supported for String values")
    }

    def paramTemplate = new TemplateStr("\${${componentName}.${field}}")
    if(!(item.lookup('parameterized', 'parameters').containsKey(paramName))) {
      item.user.bindAppend(item.user, 'parameterized', 'parameters', [(paramName): paramTemplate])
    } else {
      def existing = item.lookup('parameterized','parameters').get(paramName)
      println "Warning: externalized parameter for ${componentName}.${field} already exists, field default ignored"
      println "         existing default: ${existing}"
    }

//    item.user.bind(componentName, field, "\${${paramName}}".toString())

    return item
  }
}
