package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ModelSpecification
import com.readytalk.jenkins.model.YamlParser

class WorkflowTest extends ModelSpecification {
  def setup() {
    types {
      add WorkflowSettings.instance
      job('testJob', ['base', 'workflowSettings'])
    }
  }

  def "Can set workflow job type"() {
    when:
    def jobs = generate(YamlParser.parse("""
- testJob:
    name: workflow-thing
    base:
      type: multibranchWorkflowJob
    workflowSettings:
      remote: 'fake-repo'
"""))

    Node job = jobs.find { it.name == 'workflow-thing' }.getNode()

    then:
    job.name() == 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject'
  }
}
