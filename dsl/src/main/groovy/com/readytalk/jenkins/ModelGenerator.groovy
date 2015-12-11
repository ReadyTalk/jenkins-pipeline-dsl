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
  ModelDsl modelDsl

  ModelGenerator(TypeRegistryMap registry = TypeRegistryMap.defaultTypes,
                 JobParent jp = defaultJobParent()) {
    this.modelDsl = new ModelDsl(jp, registry)
  }

  JobParent getJobParent() {
    return modelDsl.jp
  }
  void setJobParent(JobParent jp) {
    modelDsl.jp = jp
  }
  TypeRegistryMap getRegistry() {
    return modelDsl.getRegistry()
  }

  static void main(String[] args) {
    //TODO: Support updating jenkins from non-gradle CLI?
    ModelGenerator modelGen = new ModelGenerator()
    args.each { String filename ->
      executeXmlAction(
              '',
              modelGen.buildXmlFromTree(modelGen.fromScript(filename)),
              JenkinsActions.dumpJenkinsXml(new File('jenkins'))
      )
    }
  }

  //Perform action on all items' xml and return result of action if applicable as
  //Map<JenkinsItemType, Map<ITEM_NAME, RETURN_VALUE>>
  static def executeXmlAction(Map<ItemType,Map<String,Node>> items, JenkinsXmlAction action) {
    executeXmlAction('', items, action)
  }
  static def executeXmlAction(String path, Map<ItemType,Map<String,Node>> items, JenkinsXmlAction action) {
    def configuredAction = action.&xmlAction.curry(path)
    items.collectEntries { ItemType type, Map<String,Node> itemsOfType ->
      [(type): itemsOfType.collectEntries(configuredAction.curry(type))]
    }
  }


  Closure fromScript(File scriptFile) {
    return ClosureGlue.wrapWithErrorContext(fromScript(scriptFile.text),
            "file: ${scriptFile.absolutePath}")
  }

  Closure fromScript(String scriptText) {
    List<Closure> blocks = []
    def dslProxy = [types: modelDsl.&types, model: { Closure block ->
      blocks.add(block)
    }] as ModelDslMethods

    ClosureGlue.executeWith(dslProxy, scriptText).call()

    return blocks.inject { combined, block ->
      ClosureGlue.combinePreservingDelegate(combined, block)
    }
  }

  Closure fromYaml(File yamlFile) {
    assert yamlFile.isFile()
    return ClosureGlue.wrapWithErrorContext(fromYaml(yamlFile.text),
            "yaml file: ${yamlFile.absolutePath}")
  }

  Closure fromFile(File file) {
    if(file.name.endsWith('.groovy')) {
      return fromScript(file)
    } else if(file.name.endsWith('.yaml') || file.name.endsWith('.yml')) {
      return fromYaml(file)
    } else {
      throw new UnsupportedOperationException("Don't know how to parse non-yaml, non-groovy dsl script: ${file.name}")
    }
  }

  Closure fromYaml(String yamlText) {
    return YamlParser.parse(yamlText)
  }

  GroupModelElement buildTree(Closure parseable, GroupModelElement parent = null) {
    GroupModelElement tree = modelDsl.parse(parseable)
    if(parent != null) {
      parent.elements.add(tree)
      return parent
    } else {
      return tree
    }
  }

  Map<ItemType,Map<String,Node>> buildXmlFromTree(Closure parseable) {
    return buildXmlFromTree(buildTree(parseable))
  }

  Map<ItemType,Map<String,Node>> buildXmlFromTree(GroupModelElement tree) {
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

  static JobParent defaultJobParent() {
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
