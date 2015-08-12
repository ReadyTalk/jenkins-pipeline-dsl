package com.readytalk.jenkins.model

import com.readytalk.util.ClosureGlue
import com.readytalk.util.StringUtils
import groovy.transform.InheritConstructors
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag

/*
   Marshals the declarative DSL into a coherent structure
   methodMissing is used to setup named elements, lookup their type, and then delegate to the appropriate parser
 */

//TODO: Improve feedback from error handling - "assertion failed" isn't very informative to users

abstract class ModelParser {
  protected final TypeRegistryMap registry

  ModelParser(TypeRegistryMap registry) {
    this.registry = registry
  }

  NamedElementType resolveType(String typeName) {
    NamedElementType type = registry.lookup(typeName)
    if(type == null) {
      throw new RuntimeException("Could not find type '${typeName}' - aborting")
    }
    return type
  }

  //Map type class to parser class
  //Parser classes can't be combined with type class to avoid collisions with methodMissing magic
  protected def delegatedParse(NamedElementType type, args) {
    switch(type) {
      case AbstractComponentType:
        return ComponentParser.parse(registry, type, args)
      case AbstractItemType:
        return ItemParser.parse(registry, type, args)
      case GroupType:
        return GroupParser.parse(registry, args)
      case PipelineType:
        return PipelineParser.parse(registry, type, args)
      default:
        throw new RuntimeException("No parser for model type class ${type.getClass()}")
    }
  }
}

@InheritConstructors
class ComponentParser extends ModelParser {
  private final Map fieldMap = [:]

  def methodMissing(String name, args) {
    //Syntax: fieldName VALUE || fieldName VALUE... || fieldName [:]
    //TODO: check that types are values (e.g. no passing File objects)
    fieldMap.put(name, args.size() == 1 ? args[0] : args)
  }

  def propertyMissing(String name, value) {
    //Syntax: fieldName = value || fieldName = [values...] || fieldName = [:]
    fieldMap.put(name, value)
  }

  static ComponentModelElement parse(TypeRegistry registry, AbstractComponentType type, args) {
    //Syntax: componentName || componentName { ... }
    assert (args.size() == 1 && args[0] instanceof Closure) || args.size() == 0
    def context = new ComponentParser(registry)
    context.with(args[0])
    return new ComponentModelElement(type, context.fieldMap)
  }
}

@InheritConstructors
class ItemParser extends ModelParser {
  private final List<ModelElement> elements = []

  private def fieldClippy(String name) {
    String suggestions = registry.componentsWithField(name).collect{ it.name }.join(', ')
    if(suggestions != '') {
      throw new RuntimeException("'${name}' does not match a component. Did you mean to set these as a field? (matched component field names for ${suggestions})")
    } else {
      throw new RuntimeException("'${name}' does not match any known component or field name")
    }
  }

  //Syntactic sugar - add a component with no config closure by pretending it's a property get
  def propertyMissing(String name) {
    def type = registry.lookup(name) ?: fieldClippy(name)
    assert type instanceof AbstractComponentType
    elements.add(new ComponentModelElement(type, [:]))
  }

  def propertyMissing(String name, value) {
    throw new RuntimeException("DSL syntax error: attempted to set '${name}' as a property, expected raw component or component name with closure")
  }

  def methodMissing(String name, args) {
    //FUTURE: Duplicate entries should be folded into a list
    //TODO: If we encounter a component field name here, suggest a fix to the user
    def type = registry.lookup(name) ?: fieldClippy(name)
    elements.add(this.delegatedParse(type, args))
  }

  static ItemModelElement parse(TypeRegistry registry, AbstractItemType type, args) {
    //Syntax: jobTypeName(jobName) || jobTypeName(jobName) { ... }
    assert args[0] instanceof String && (args.size() == 1 || (args.size() <= 2 && args.last() instanceof Closure)),
      "Can't create job/item with no name (item type: ${type.getName()})"
    def context = new ItemParser(registry)
    context.with(args.size() > 1 ? args[1] : {})
    return new ItemModelElement(context.elements, type, args[0])
  }
}

@InheritConstructors
class GroupParser extends ModelParser {
  private final List<ModelElement> elements = []

  def methodMissing(String name, args) {
    NamedElementType elementType = resolveType(name)

    if(elementType == null) {
      throw new RuntimeException("No type found for element name ${name}")
    }

    elements.add(this.delegatedParse(elementType, args))
  }

  //Parser entry point
  static GroupModelElement parse(TypeRegistry registry, Object[] args) {
    assert args.size() == 1 && args[0] instanceof Closure
    def context = new GroupParser(registry)
    context.with(args[0])
    return new GroupModelElement(context.elements)
  }
}

@InheritConstructors
class PipelineParser extends ModelParser {
  List<ModelElement> elements = []

  def methodMissing(String name, args) {
    def type = registry.lookup(name)

    //Parse components normally
    if(type instanceof AbstractComponentType) {
      elements.add(this.delegatedParse(type, args))
    } else {

      assert args.size() <= 2
      assert args.first() instanceof Map && (args.size() == 1 || args.last() instanceof Closure),
        "Expected 'type: JOB_TYPE' entry for pipeline stage"

      Map<String, String> opts = args.first()
      def jobType = resolveType(opts.get('type'))
      assert jobType instanceof AbstractItemType

      def jobArgs = args.size() == 1 ? [name] : [name, args.last()]
      ItemModelElement jobElement = this.delegatedParse(jobType, jobArgs)
      elements.add(jobElement)
    }
  }

  static PipelineModelElement parse(TypeRegistry registry, PipelineType type, Object[] args) {
    assert args.size() == 2 && args.first() instanceof String && args.last() instanceof Closure
    def context = new PipelineParser(registry)
    context.with(args.last())
    return new PipelineModelElement(context.elements, type, args.first())
  }
}

class YamlParser {
  //TODO: This ended up being more complex than intended
  //TODO: It might be a good idea to simplify the original dsl, and then simplify this one
  static Closure parse(String yaml) {
    Constructor cons = new Constructor()
    cons.addTypeDescription(new TypeDescription(TemplateStr.class, "!template"))
    Yaml yamlLoader = new Yaml(cons)
    def _model = yamlLoader.load(yaml)
    Closure yamlParser = {
      Closure yamlTransform
      //Translate YAML elements into groovy "method" calls mimicking the original dsl
      //NOTE: This logic should not need to be altered unless the dsl syntax itself is altered or extended
      yamlTransform = { context, _entry ->
        if(_entry instanceof List) {
          _entry.each(yamlTransform.curry(context))
        } else if(_entry instanceof Map && _entry.size() == 1) {
          def (key, value) = _entry.collect { k, v -> [k,v] }.first()
          yamlTransform.call(context, new MapEntry(key, value))
        } else {
          String key = _entry.key
          def value = _entry.value
          if(context instanceof ComponentParser) {
            if(key == 'dsl' && value instanceof String) {
              //Special case to allow embedded dsl logic
              //dsl: |
              //  #upstream groovy dsl code
              context.setProperty(key, ClosureGlue.asClosure("vars -> ${value}"))
            } else {
              //Assign value to component field (field: value)
              context.setProperty(key, value != null ? value : '')
            }
          } else if (context instanceof ItemParser && value == null) {
            //Add component with default config (componentName:)
            context.getProperty(key)
          } else if (context instanceof GroupParser && value instanceof String) {
            //Job definition with no config (- jobType: jobName)
            context.invokeMethod(key, [value] as Object[])
          } else if (value instanceof Map) {
            if (value.containsKey('name') && !(context instanceof ItemParser)) {
              //Assume this is a named item element (or pipeline)
              //- jobType:
              //    name: jobName
              context.invokeMethod(key, [value.get('name'), {
                ((Map) value).remove('name')
                value.each(yamlTransform.curry(getDelegate()))
              }] as Object[])
            } else if (context instanceof PipelineParser && value.containsKey('type')) {
              //Pipelines use special snowflake syntax
              //stageName:
              //  type: jobType
              context.invokeMethod(key, [[type: value.get('type')], {
                ((Map) value).remove('type')
                value.each(yamlTransform.curry(getDelegate()))
              }] as Object[])
            } else { //Probably a component + config
              context.invokeMethod((String) key, [{
                                                    value.each(yamlTransform.curry(getDelegate()))
                                                  }] as Object[])
            }
          } else { //Generic - should catch nested group structures among other things
            context.invokeMethod(key, [{
                                         value.each(yamlTransform.curry(getDelegate()))
                                       }] as Object[])
          }
        }
      }
      _model.each(yamlTransform.curry(getDelegate()))
    }

    return yamlParser
  }
}