package com.readytalk.jenkins.model.visitors

//Double-dispatch tree traversal based on http://groovy.codehaus.org/Visitor+Pattern

//Default to behaving like a leaf node by returning no children
//Ought to be a trait, but traits mess with polymorphic dispatch in counter-intuitive ways
abstract class Visitable {
  List<Visitable> getElements() { return [] }
}

/**
 * Default, in-order traversal of directed acyclic graph with empty default behavior
 * Override methods with type Visitable to set default behavior for all element types
 * Override methods with type T (implementing Visitable) to set behavior for elements of, extending, or implementing T
 * visit(element)   - traversal control flow for element
 * recurse(element) - traversal control flow for children of element
 * enter(element)   - by default, called when entering an element
 * exit(element)    - by default, called when leaving an element
 *
 * Override enter(Visitable)/exit(Visitable) to set default behavior
 * Override enter(T)/exit(T) to set behavior for type T (where T implements Visitable)
 * Override recurse(T) to set iteration behavior over child elements
 */
abstract class SymmetricVisitor {
  def enter(Visitable element) {}
  def exit(Visitable element) {}

  def visit(Visitable element) {
    enter(element)
    recurse(element)
    exit(element)
  }

  void recurse(Visitable element) {
    element.getElements().each { visit(it) }
  }
}
