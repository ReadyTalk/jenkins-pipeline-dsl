package com.readytalk.jenkins.model.visitors

import com.readytalk.jenkins.model.ContextLookup
import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.DefaultContext
import com.readytalk.jenkins.model.ModelContext
import com.readytalk.jenkins.model.ScopedModelElement
import com.readytalk.jenkins.model.TypeRegistry

class ScopeConstructor extends SymmetricVisitor {
  private final Stack<ContextMap> userStack = new Stack<ContextMap>()
  private final Stack<ContextMap> defaultStack = new Stack<ContextMap>()

  ScopeConstructor(TypeRegistry registry) {
    defaultStack.push(new ContextMap(new DefaultContext(registry), [:]))
    userStack.push(new ContextMap())
  }

  ScopeConstructor(ContextLookup defaults, ContextMap contextRoot) {
    defaultStack.push(new ContextMap(defaults, [:]))
    userStack.push(contextRoot)
  }

  def enter(ScopedModelElement element) {
    userStack.push(userStack.peek().createChildContext())
    defaultStack.push(defaultStack.peek().createChildContext())
    element.modelContext = new ModelContext(userStack.peek(), defaultStack.peek())
  }

  def exit(ScopedModelElement element) {
    userStack.pop()
    defaultStack.pop()
  }
}
