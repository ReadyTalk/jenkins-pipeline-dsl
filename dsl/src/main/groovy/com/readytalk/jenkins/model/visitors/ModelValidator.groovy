package com.readytalk.jenkins.model.visitors

import com.readytalk.jenkins.model.*

//Basic sanity check so that the evaluation logic can make some simplifying assumptions
//FUTURE: evaluate every node before aborting to show all errors in one go
class ModelValidator extends SymmetricVisitor {
  TypeRegistry registry
  def failures = []

  ModelValidator(TypeRegistry registry) {
    this.registry = registry
  }

  static void evaluate(TypeRegistry registry, GroupModelElement tree) {
    def validator = new ModelValidator(registry)
    validator.visit(tree)
    if(validator.failures.size() != 0) {
      validator.failures.each { AssertionError err ->
        println(err.message)
        println()
      }
      throw new RuntimeException("Model validation failed")
    }
  }

  def visit(ModelElement element) {
    try {
      enter(element)
    } catch(AssertionError err) {
      failures.add(err)
    }
    recurse(element)
    exit(element)
  }

  def recurse(ModelElement element) {
    element.elements.each { ModelElement child ->
      assert element.type.canContain(child.type)
      visit(child)
    }
  }

  def enter(ItemModelElement element) {
    assert element.type != null
    assert element.type instanceof AbstractItemType
    def indexType = registry.lookup(element.type.name)
    assert indexType != null && indexType.equals(element.type), "Job type must be identifiable by name"
    element.type.components.each { String componentName ->
      assert registry.lookup(componentName) != null, "Job type ${element.type.name} has null component: ${componentName}"
    }
  }

  def enter(ComponentModelElement element) {
    assert element.type != null
    assert element.type instanceof AbstractComponentType
    def indexType = registry.lookup(element.type.name)
    assert indexType != null, "Component type '${element.type.name}' does not exist in type registry"
    assert indexType.equals(element.type), "[BUG]: Component type '${element.type.name}' instance doesn't match instance returned by registry."
    element.fields.keySet().each { userField ->
      assert element.type.fields.containsKey(userField)
    }
  }
}
