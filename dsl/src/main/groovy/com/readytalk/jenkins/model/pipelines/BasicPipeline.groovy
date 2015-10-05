package com.readytalk.jenkins.model.pipelines

import com.readytalk.jenkins.model.ContextAlreadyBoundException
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.PipelineType
import com.readytalk.jenkins.model.types.ParameterizedComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent
import groovy.transform.InheritConstructors

@InheritConstructors
class BasicPipeline extends AbstractPipeline {
  static PipelineType type = new PipelineType('pipeline', BasicPipeline.class)

  final Map stageGraph = [
          build: 'test',
          test: 'deploy',
          deploy: 'functionalTest',
          functionalTest: 'promote',
          promote: 'productionDeploy'
  ]

  protected ItemSource forceScopeOverride(ItemSource item) {
    //TODO: ad-hoc, inline property overwrites like this are fragile
    //      it breaks referential transparency and idempotence
    item.itemContext = item.itemContext.childContext()
    return item
  }

  def configureJobs() {
    def pipelineParams = [:]
    jobsInStage('build').each { ItemSource job ->
      forceScopeOverride(job)

      injectComponent(job, TriggerDownstreamComponent.instance).with {
        parameters = [
                BUILD_COMMIT: '${GIT_COMMIT}',
        ]
      }

      pipelineParams.putAll(job.lookupValue('triggerDownstream', 'parameters'))
    }

    //TODO: This may no longer be relevant - verify it's still needed
    //Strip default values that might only make sense in context of original build jobs
    pipelineParams = pipelineParams.collectEntries { param, value -> [(param): ''] }

    //Disable automatic triggers for downstream jobs
    findJobs { stage, _ -> stage != 'build' }.each { ItemSource job ->
      forceScopeOverride(job) //WARNING: ad-hoc state mutation

      injectComponent(job, ParameterizedComponent.instance)
      job.defaults.with {
        bindAppend(job.defaults, 'parameterized', 'parameters', [BUILD_COMMIT: job.lookupValue('git', 'branches')])
        bind('pullRequest', 'enabled', false)
        bind('common', 'runSchedule', '')
        bind('git', 'trigger', '')
        bind('git', 'branches', '${BUILD_COMMIT}')
        //Ensure rebuild button actually re-uses parameters
        bindPrepend(job.defaults, 'parameterized', 'parameters', pipelineParams)
      }
    }

    (jobsUpstreamOf('promote') + jobsUpstreamOf('productionDeploy')).each { ItemSource job ->
      job.defaults.bind('triggerDownstream', 'manual', true)
    }

    jobs.each { stage, list ->
      list.each { ItemSource job ->
        job.defaults.bind('common', 'description',
                "This job is part of the ${stage} stage of the ${project} pipeline\n")
      }
    }
  }
}
