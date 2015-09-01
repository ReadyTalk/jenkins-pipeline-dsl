package com.readytalk.gradle

import com.readytalk.jenkins.JenkinsActions
import com.readytalk.jenkins.ItemType
import com.readytalk.jenkins.ModelGenerator
import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.GroupModelElement
import com.readytalk.jenkins.model.ModelDsl
import com.readytalk.util.ClosureGlue

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.LogLevel

/**
 * FUTURE: add support for spinning up the jenkins test container and automatically pointing at it to test out the DSL
 * FUTURE: delete old jobs / support job renaming
 * FUTURE: support for separate dsl files
 */

class ReadytalkJenkinsPlugin implements Plugin<Project> {
  Project project
  JenkinsConfigExtension config
  ModelGenerator modelGen

  private Map<ItemType,Map<String,Node>> items

  //TODO: Find a way to enforce restricting this to task execution phase only
  def generateItems() {
    if (items == null) {
      //Fail loudly instead of silently if configuration added after xml already generated
      config.freeze()
      System.setProperty('printJenkinsDiffs', project.logger.isEnabled(LogLevel.INFO).toString())

      items = [:]

      config.typeBlocks.each(modelGen.modelDsl.&types)

      //Set defaults
      modelGen.modelDsl.defaults(ClosureGlue.wrapWithErrorContext(config.getDefaults(),
          "gradle plugin defaults block from ${project.buildFile.absolutePath}"))

      //Collect all parsed trees under a single root for convenience
      GroupModelElement rootTree = modelGen.evaluate{}
      Closure evaluator =
              ClosureGlue.wrapWithErrorContext(modelGen.&evaluate.rcurry(rootTree),
                      "dsl closure in gradle file ${project.buildFile.absolutePath}")
      Closure fileEvaluator =
              ClosureGlue.wrapWithErrorContext(evaluator << modelGen.&generateFromFile,
                      "file specified by gradle plugin from ${project.buildFile.absolutePath}")

      config.modelBlocks.each(evaluator)

      config.scriptFiles.filter { File file ->
        file.isFile() && (file.name.matches(/.*\.(groovy|yaml|yml)$/))
      }.collect(fileEvaluator)

      items = modelGen.generateItems(rootTree)
    }
    return items
  }

  void apply(final Project project) {
    this.project = project
    config = new JenkinsConfigExtension(this)
    modelGen = new ModelGenerator()
    project.extensions.add('jenkins', config)

    project.tasks.create('evaluateJenkinsDsl') { evalTask ->
      evalTask.inputs.property('modelDsl', config.modelBlocks)
      evalTask.inputs.files(config.scriptFiles)
      project.tasks.withType(JenkinsTask) { task ->
        task.dependsOn evalTask
      }
      evalTask.doLast {
        generateItems()
      }
    }

    project.tasks.create('printJenkinsXml', JenkinsTask).doFirst { JenkinsTask task ->
      task.xmlWriter = JenkinsActions.jenkinsXmlString
    }

    project.tasks.create('dumpJenkinsXml', JenkinsTask).doFirst { JenkinsTask task ->
      task.xmlWriter = JenkinsActions.dumpJenkinsXml(project.buildDir)
    }

    project.tasks.create('updateJenkinsXml', JenkinsTask).doFirst { JenkinsTask task ->
      logger.quiet("Updating jobs for Jenkins instance at ${config.url}:")
    }
  }
}
