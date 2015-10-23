package com.readytalk.jenkins.model


import com.readytalk.jenkins.model.pipelines.AbstractPipeline
import com.readytalk.jenkins.model.meta.ComponentAdapter
import com.readytalk.util.ClosureGlue
import groovy.transform.AnnotationCollector
import groovy.transform.Immutable

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.reflect.Field

//For use on single-type classes implementing AbstractComponentType or AbstractJobType
@Immutable
@Singleton(strict=false)
@AnnotationCollector
public @interface Fixed {}

interface NamedElementType {
  String getName()
  boolean canContain(NamedElementType type)
}

//TODO: Internal field to denote item type this component is applicable to - e.g. shouldn't apply view component to job item
//TODO: Allow components to mark themselves incompatible with other components
//      Ideally this could happen automatically, but that would get pretty tricky unless we abandon the upstream dsl
/**
 * Base component type
 *
 * Contains a flat map of parameters to arbitrary values and a block of upstream dsl logic
 *
 * Defaults are static to avoid dependency hell - if you want to set defaults dynamically, use an item type
 * Item types can't arbitrarily refer to other item's values like components can, and follow a strict hierarchy
 */
abstract class AbstractComponentType implements NamedElementType {
  //TODO: Optional automatic type / processing for fields to guarantee string/list format on lookup
  //TODO: Allow components to depend on other components being present
  //Map of component fields to default values
  abstract Map<String, ?> getFields()

  //Actual execution logic for this component
  Closure getDslConfig() { return {vars->} }

  //Internal validation - components are leaf nodes, so can't contain further elements
  boolean canContain(NamedElementType type) { return false }

  /**
   * Escape hatch to allow direct manipulation of the resulting job, including generating additional jobs (e.g. pullRequest)
   * All post processing closures will be evaluated exactly once per job, including jobs created by other components' post processing
   * Evaluation order is undefined, so post processing steps must not rely on other components' post-processing results
   */
  ItemSource[] postProcess(ItemSource original) { return [original] }

  //Allows components to force ordering - lower value means higher precedence
  int getPriority() { return 50 }

  //TODO: Rename this to adapter instead of traits, to avoid confusion with Groovy's native traits
  List<ComponentAdapter> traits = []

  //Would be nice to generate this statically, but the performance hit is negligible anyways
  ComponentAdapter composeAdapter() {
    return ClosureGlue.monadicFold(ComponentAdapter, getTraits()) {
      assert it != null, "A component adapter on '${getName()}' returned null, which is forbidden\n" +
              "Adapter classes: ${getTraits().collect{it.getClass().superclass}}"
      return it
    }
  }
}

abstract class AbstractItemType implements NamedElementType {
  //Simple list of component names that make up this job type
  abstract List<String> getComponents()

  //Set and manipulate default component values for this job type
  abstract Closure getDefaults()

  boolean canContain(NamedElementType type) {
    return type instanceof AbstractComponentType
  }

  //TODO: It would be nice if we could do this with components too, but it's a lot trickier
  JobType extendWith(AbstractItemType childType) {
    new JobType(childType.name,
                components + childType.components,
                ClosureGlue.combinePreservingDelegate(defaults, childType.defaults))
  }

}

@Immutable
class PipelineType implements NamedElementType {
  String name
  Class<? extends AbstractPipeline> implementation
  boolean canContain(NamedElementType type) {
    return type instanceof AbstractComponentType || type instanceof AbstractItemType
  }
}

@Fixed
class GroupType implements NamedElementType {
  String name = 'group'
  boolean canContain(NamedElementType type) {
    return type instanceof AbstractItemType || type instanceof AbstractComponentType ||
           type instanceof GroupType        || type instanceof PipelineType
  }
}

@Immutable
class ComponentType extends AbstractComponentType {
  String name
  Map<String,?> fields
  Closure dslConfig
}

@Immutable
class JobType extends AbstractItemType {
  String name
  List<String> components
  Closure defaults
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ComponentField {}

//Allows declaring component parameters as class fields, not just a map
//This enables annotating component parameters with actual types
abstract class AnnotatedComponentType extends AbstractComponentType {
  Map<String,?> getFields() {
    Map<String,?> map = [:]
    map.putAll(this.getClass().getDeclaredFields().collectEntries { Field field ->
      if(ComponentField in field.declaredAnnotations*.annotationType()) {
        [(field.name): this.getProperty(field.name)]
      } else {
        []
      }
    })
    return map
  }

  //TODO: validation method to check field types
}
