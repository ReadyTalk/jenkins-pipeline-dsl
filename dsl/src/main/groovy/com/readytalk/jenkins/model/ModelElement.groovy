package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.visitors.Visitable

import groovy.transform.AnnotationCollector
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

/**
 * Model tree element types
 * There should be very little or no logic in any of these classes, they're intended to be plain date
 * Evaluation of the tree dispatches based on the type, so generics wouldn't work due to type erasure
 */

//@ToString(includeSuperProperties=true) //TODO: Requires Groovy 2.4.x
@TupleConstructor(includeSuperProperties=true, callSuper=true, excludes=['modelContext'])
@CompileStatic
@AnnotationCollector
public @interface ModelConstructor {}

@Canonical
class ModelContext {
  ContextMap user
  ContextMap defaults

  ContextLookup getContext() {
    return user.withFallback(defaults)
  }

  ModelContext childContext() {
    return new ModelContext(this.user.createChildContext(), this.defaults.createChildContext())
  }
}

@ModelConstructor
abstract class ModelElement extends Visitable {
  abstract NamedElementType getType()
}

@ModelConstructor
abstract class ScopedModelElement extends ModelElement {
  @Delegate ModelContext modelContext
  final List<ModelElement> elements
}

@ModelConstructor
class ItemModelElement extends ScopedModelElement {
  final AbstractItemType type
  final String name
  //Combine item-type-defined and user-added components
  Set<String> getComponents() {
    return (elements.findAll{ it instanceof ComponentModelElement }.collect { ModelElement elem ->
      ((ComponentModelElement)elem).getType().getName()
    } + this.type.getComponents()).toSet()
  }
}

@ModelConstructor
class ComponentModelElement extends ModelElement {
  final AbstractComponentType type
  final Map fields
}

@ModelConstructor
class GroupModelElement extends ScopedModelElement {
  final GroupType type = GroupType.instance
}

@ModelConstructor
class PipelineModelElement extends ScopedModelElement {
  final PipelineType type
  final String project
}