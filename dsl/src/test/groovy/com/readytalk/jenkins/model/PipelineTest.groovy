package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.pipelines.BasicPipeline
import com.readytalk.jenkins.model.pipelines.SequentialPipeline
import com.readytalk.jenkins.model.types.CloneWorkspaceComponent
import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent
import spock.lang.Ignore

class PipelineTest extends ModelSpecification {
  def setup() {
    registry.add(ParameterizedComponent.instance)
    registry.add(TriggerDownstreamComponent.instance)
    registry.add(BasicPipeline.getType())
    registry.add(SequentialPipeline.getType())
  }

  def "evaluates minimal pipeline with single job"() {
    when:
    def jobs = eval {
      pipeline('fake') {
        build(type: 'Jobbers') {
          fauxComponent {
            field 'someValue'
          }
        }
      }
    }

    println "Single Job Pipeline Jobs: ${jobs}"
    println "Single Job Pipeline Jobs names: ${jobs.collect{it.itemName}}"

    then:
    jobs.find { it.itemName == 'fake-build' } != null
  }

  def "Evaluates simple pipeline with two jobs"() {
    when:
    def jobs = eval {
      pipeline('fake') {
        fauxComponent {
          field 'commonValue'
        }
        build(type: 'Jobbers')
        deploy(type: 'DerJob') {
          fauxComponent {
            field 'deployValue'
          }
        }
      }
    }
    def buildJob = jobs.find { it.itemName == 'fake-build' }
    def deployJob = jobs.find { it.itemName == 'fake-deploy' }
    Collection<String> buildDownstream = buildJob?.lookup('triggerDownstream','jobs')

    println "Two Job Pipeline Jobs names: ${jobs.collect{it.itemName}}"

    then:
    buildJob != null
    deployJob != null
    buildJob.lookup('fauxComponent', 'field') == 'commonValue'
    deployJob.lookup('fauxComponent','field') == 'deployValue'
    buildDownstream != null
    buildDownstream.contains('fake-deploy')
  }

  def "promote stage in basic pipeline has manual trigger by default"() {
    when:
    def jobs = eval {
      pipeline('direct') {
        functionalTest(type: 'testJob')
        promote(type: 'promoteJob')
      }
      pipeline('indirect') {
        test(type: 'testJob')
        promote(type: 'promoteJob')
      }
    }

    def directJob = jobs.find { it.itemName == 'direct-functionalTest' }
    def indirectJob = jobs.find { it.itemName == 'indirect-test' }

    then:
    directJob != null
    indirectJob != null
    directJob.lookup('triggerDownstream','manual') == true
    indirectJob.lookup('triggerDownstream','manual') == true
  }

  def "sequential pipeline uses simple ordering"() {
    when:
    def jobs = eval {
      sequential('count') {
        one(type: 'basicJob')
        two(type: 'basicJob')
        three(type: 'basicJob')
      }
    }

    then:
    jobs.find { it.itemName == 'count-one' }.lookup('triggerDownstream', 'jobs').contains('count-two')
    jobs.find { it.itemName == 'count-two' }.lookup('triggerDownstream', 'jobs').contains('count-three')
  }

  //TODO: We should probably check for circular dependencies in general
  def "rejects clone workspace request for non-upstream job"() {
    when:
    registry.add(CloneWorkspaceComponent.instance)
    registry.add(GitComponent.instance)

    def jobs = eval {
      pipeline('clone') {
        build(type: 'fakeJob')
        deploy(type: 'fakeJob') {
          cloneWorkspace {
            upstream 'clone-promote'
          }
        }
      }
    }

    then:
    def e = thrown(RuntimeException)
    e.message.contains('requires workspace')
    e.message.contains('non-upstream job')
  }

  def "correctly calculates upstream jobs across transitive edges"() {
    when:
    List<ItemSource> jobs = eval {
      fakeJob('build')
      fakeJob('deploy')
      fakeJob('promote')
    }

    BasicPipeline pipeline = new BasicPipeline('pipe')
    pipeline.configure(jobs)
    ItemSource job = jobs.find { it.itemName == 'pipe-promote' }

    def upstreams = pipeline.transitiveJobsUpstreamOf(job)

    then:
    jobs.findAll { it.itemName == 'pipe-deploy' || it.itemName == 'pipe-build' }.each {
      assert upstreams.contains(it)
    }
  }

  def "injects clone workspace configuration"() {
    when:
    registry.add(CloneWorkspaceComponent.instance)
    registry.add(GitComponent.instance)

    def jobs = eval {
      pipeline('clone') {
        build(type: 'fakeJob')
        deploy(type: 'fakeJob')
        promote(type: 'fakeJob') {
          cloneWorkspace {
            upstream 'clone-build'
          }
        }
      }
    }
    def downstream = jobs.find { it.itemName == 'clone-promote' }

    then:
    registry.lookup('cloneWorkspace') == CloneWorkspaceComponent.instance
    jobs.find { it.itemName == 'clone-build' }.lookup('triggerDownstream', 'copyWorkspace') == true
    jobs.find { it.itemName == 'clone-deploy' }.lookup('triggerDownstream', 'copyWorkspace') == false
    downstream.components.contains(CloneWorkspaceComponent.instance)
    !downstream.components.contains(GitComponent.instance)
  }
}
