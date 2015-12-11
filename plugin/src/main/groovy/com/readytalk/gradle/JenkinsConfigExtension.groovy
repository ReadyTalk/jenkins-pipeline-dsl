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
  List<Closure> typeBlocks = []
  String url = 'JENKINS_URL'
  String username = 'jenkins'
  String password
  Map<String, Configs> configs

  JenkinsConfigExtension(ReadytalkJenkinsPlugin plugin) {
    this.plugin = plugin
    this.project = plugin.project
    this.configs = [:].withDefault { String path ->
      def conf = new Configs(path)
      conf.scriptFiles = project.files()
      return conf
    }
  }

  static class Configs {
    final String path
    FileCollection scriptFiles
    List<Closure> modelBlocks = []
    Configs(String path) { this.path = path }
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

  void dsl(String path, Closure modelBlock) {
    failIfFrozen()
    configs[path].modelBlocks.add(modelBlock)
  }

  void model(Closure modelBlock) {
    dsl('', modelBlock)
  }

  void dsl(String path, Iterable<File> files) {
    failIfFrozen()
    configs[path].scriptFiles += project.files(files)
  }

  void dsl(String path, File file) {
    failIfFrozen()
    configs[path] += project.files(file)
  }

  def methodMissing(String name, args) {
    if(name == 'dsl' && args.size() == 1) {
      dsl('', args.first())
    } else {
      throw new MissingMethodException(name, this.class, args)
    }
  }

  void freeze() {
    frozen = true
  }
}
