package com.readytalk.jenkins

import com.readytalk.jenkins.model.GroupModelElement
import com.readytalk.jenkins.model.ModelDsl
import com.readytalk.jenkins.model.ModelDslMethods
import com.readytalk.jenkins.model.TypeRegistryMap
import com.readytalk.jenkins.model.YamlParser
import com.readytalk.util.ClosureGlue
import groovy.transform.CompileStatic
import javaposse.jobdsl.dsl.FileJobManagement
import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.View

//TODO: Consider merging this with the ModelDsl class
//TODO: Abstract away JobParent details

@CompileStatic
class ModelGenerator {
  JobParent jobParent
  ModelDsl modelDsl
  TypeRegistryMap registry

  ModelGenerator(TypeRegistryMap registry = TypeRegistryMap.defaultTypes,
                 JobParent jp = createJobParent()) {
    this.registry = registry
    this.jobParent = jp
    this.modelDsl = new ModelDsl(jobParent, registry)
  }

  static void main(String[] args) {
    //TODO: Support updating jenkins from non-gradle CLI?
    ModelGenerator modelGen = new ModelGenerator()
    args.each { String filename ->
      executeXmlAction(
              modelGen.generateItems(modelGen.generateFromScript(filename)),
              JenkinsActions.dumpJenkinsXml(new File('jenkins'))
      )
    }
  }

  //Perform action on all items' xml and return result of action if applicable as
  //Map<JenkinsItemType, Map<ITEM_NAME, RETURN_VALUE>>
  static def executeXmlAction(Map<ItemType,Map<String,Node>> items, JenkinsXmlAction action) {
    items.collectEntries { ItemType type, Map<String,Node> itemsOfType ->
      [(type): itemsOfType.collectEntries(action.&xmlAction.curry(type))]
    }
  }


  Closure generateFromScript(File scriptFile) {
    return generateFromScript(scriptFile.text)
  }

  Closure generateFromScript(String scriptText) {
    List<Closure> blocks = []
    def dslProxy = [types: modelDsl.&types, model: { Closure block ->
      blocks.add(block)
    }] as ModelDslMethods

    ClosureGlue.executeWith(dslProxy, scriptText).call()

    return blocks.inject { combined, block ->
      ClosureGlue.combinePreservingDelegate(combined, block)
    }
  }

  Closure generateFromYaml(File yamlFile) {
    assert yamlFile.isFile()
    return generateFromYaml(yamlFile.text)
  }

  Closure generateFromFile(File file) {
    if(file.name.endsWith('.groovy')) {
      return generateFromScript(file.text)
    } else if(file.name.endsWith('.yaml') || file.name.endsWith('.yml')) {
      return generateFromYaml(file.text)
    } else {
      throw new UnsupportedOperationException("Don't know how to parse non-yaml, non-groovy dsl script: ${file.name}")
    }
  }

  Closure generateFromYaml(String yamlText) {
    return YamlParser.parse(yamlText)
  }

  GroupModelElement evaluate(Closure parseable, GroupModelElement parent = null) {
    GroupModelElement tree = modelDsl.parse(parseable)
    if(parent != null) {
      parent.elements.add(tree)
      return parent
    } else {
      return tree
    }
  }

  Map<ItemType,Map<String,Node>> generateItems(Closure parseable) {
    return generateItems(evaluate(parseable))
  }

  Map<ItemType,Map<String,Node>> generateItems(GroupModelElement tree) {
    modelDsl.generate(tree)
    Map<String,Node> items
    Map<String,Node> views
    items = jobParent.referencedJobs.collectEntries { Item item ->
      [(item.name): item.getNode()]
    }
    views = jobParent.referencedViews.collectEntries { View view ->
      [(view.name): view.getNode()]
    }
    return [(ItemType.job): items, (ItemType.view): views]
  }

  private static JobParent createJobParent() {
    FileJobManagement jm = new FileJobManagement(new File('.'))
    jm.parameters.putAll(System.getenv())

    //This version is fine for running closures, but should use the real script loading stuff otherwise
    JobParent jp = new JobParent() {
      Object run() {}
    }

    jp.setJm(jm)
    return jp
  }
}
