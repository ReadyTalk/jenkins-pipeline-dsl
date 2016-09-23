package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ModelSpecification
import com.readytalk.jenkins.model.YamlParser
import spock.lang.Ignore

class WorkflowTest extends ModelSpecification {
  def setup() {
    types {
      add MultibranchSettings.instance
      add PipelineSettings.instance
      job('multibranchJob', ['base', 'multibranchSettings'])
      job('pipelineJob', ['base', 'pipelineSettings'])
    }
  }

  def "Can set workflow job type"() {
    when:
    def jobs = generate(YamlParser.parse("""
- testJob:
    name: workflow-thing
    base:
      type: multibranchWorkflowJob
    multibranchSettings:
      remote: 'fake-repo'
"""))

    Node job = jobs.find { it.name == 'workflow-thing' }.getNode()

    then:
    job.name() == 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject'
  }

  def "Can set credentials via dsl block"() {
    when:
    def jobs = generate(YamlParser.parse("""
- multibranchJob:
    name: workflow-thing
    base:
      type: multibranchWorkflowJob
    multibranchSettings:
      remote: 'fake-repo'
      dsl: |
        credentialsId('fake-creds-id')
"""))

    Node job = jobs.find { it.name == 'workflow-thing' }.getNode()

    then:
    //Should only be one BranchSource with this setup
    job.sources.data.size() == 1
    job.sources.data.'jenkins.branch.BranchSource'[0].source.credentialsId[0].value() == 'fake-creds-id'
  }

  @Ignore
  def "Can set inline pipeline script"() {
    when:
    String script = "echo 'Hello world!'"
    def jobs = generate(YamlParser.parse("""
- pipelineJob:
    name: pipeinline
    base:
      type: workflowJob
    pipelineSettings:
      script: |
        ${script}
      groovySandbox: true
"""))

    Node pipeinline = jobs.find { it.name == 'pipeinline' }.getNode()


    then:
    pipeinline.definition.script[0].value().trim() == script
    pipeinline.definition.sandbox[0].value() == true
  }
}
