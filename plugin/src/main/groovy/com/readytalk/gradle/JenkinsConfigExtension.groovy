package com.readytalk.gradle

import com.readytalk.jenkins.model.ModelDslMethods
import com.readytalk.jenkins.model.TemplateStr
import com.readytalk.jenkins.model.YamlParser
import com.readytalk.util.ClosureGlue
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class JenkinsConfigExtension implements ModelDslMethods {
  private final ReadytalkJenkinsPlugin plugin
  private final Project project

  private boolean frozen = false

  Closure defaults = {
    base {
      jenkins = getOwner().url
    }
  }
  FileCollection scriptFiles
  List<Closure> modelBlocks = []
  List<Closure> typeBlocks = []
  String url
  String username
  String password

  JenkinsConfigExtension(ReadytalkJenkinsPlugin plugin) {
    this.plugin = plugin
    this.project = plugin.project
    this.scriptFiles = project.files()
  }

  private void failIfFrozen() {
    if(frozen) {
      throw new GradleException("Can't modify jenkins model dsl after it has been generated")
    }
  }

  void types(Closure typeBlock) {
    failIfFrozen()
    typeBlocks.add(typeBlock)
  }

  void model(Closure modelBlock) {
    failIfFrozen()
    modelBlocks.add(modelBlock)
  }

  void defaults(Closure defaultsBlock) {
    if(this.defaults == null) {
      this.defaults == defaultsBlock
    } else {
      this.defaults = ClosureGlue.combinePreservingDelegate(this.defaults, defaultsBlock)
    }
  }

  void defaults(File scriptFile) {
    if(scriptFile.name.endsWith('.groovy')) {
      defaults(ClosureGlue.asClosure(scriptFile.text))
    } else if(scriptFile.name.matches(/.*\.ya?ml$/)) {
      defaults(YamlParser.parse(scriptFile.text))
    }
  }

  void dsl(Closure modelBlock) {
    model(modelBlock)
  }

  void dsl(Iterable<File> files) {
    failIfFrozen()
    scriptFiles = scriptFiles + project.files(files)
  }

  void dsl(File file) {
    failIfFrozen()
    scriptFiles = scriptFiles + project.files(file)
  }

  void freeze() {
    frozen = true
  }
}
