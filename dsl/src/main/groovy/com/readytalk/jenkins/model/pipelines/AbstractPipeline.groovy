package com.readytalk.jenkins.model.pipelines

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.ContextAlreadyBoundException
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.PipelineType
import com.readytalk.jenkins.model.ProxyDelegate
import com.readytalk.util.StringUtils
import com.readytalk.jenkins.model.types.CloneWorkspaceComponent
import com.readytalk.jenkins.model.types.GitComponent
import com.readytalk.jenkins.model.types.TriggerDownstreamComponent

import java.lang.reflect.Method
import java.lang.reflect.Modifier

//TODO: Allow different syntaxes for pipelines by making the parser live here (can be overridden)
abstract class AbstractPipeline {
  final String project

  AbstractPipeline(String project) {
    this.project = project
  }

  abstract Map<String,String> getStageGraph()
  Map<String,List<ItemSource>> jobs

  abstract def configureJobs()

  void configure(List<ItemSource> pipelineJobs) {
    jobs = mapJobs(pipelineJobs)

    //TODO: Support multi-job stages
    if(jobs.any { _, stageJobs -> stageJobs.size() > 1}) {
      throw new UnsupportedOperationException("Currently only one job per stage is supported.")
    }

    //NOTE: this is expected to be an idempotent operation
    configureJobs()
    getDependencies().each(this.&connectJobs)
    configureWorkspacePassing(jobs.values().flatten())
  }

  protected Map<String,List<ItemSource>> mapJobs(List<ItemSource> jobs) {
    //Default mapping uses the job 'name' as the stage name
    Map<String,List<ItemSource>> byStage = [:]
    jobs.each { job ->
      String stage = job.itemName

      try {
        job.itemName = "${project}-${stage}"
      } catch(ContextAlreadyBoundException e) { /* force override */ }

      if(byStage.get(stage) == null) byStage.put(stage,[])
      byStage.get(stage).add(job)
    }
    return byStage
  }

  /**
   * Configure upstream job to trigger downstream job
   * This method is expected to be idempotent
   */
  protected void connectJobs(ItemSource upstream, ItemSource downstream) {
    def trigger = injectComponent(upstream, TriggerDownstreamComponent.instance)
    def downstreamJobs = StringUtils.asList(trigger.jobs,',').toSet()
    downstreamJobs.add(downstream.itemName)
    trigger.jobs = downstreamJobs.toList()
    trigger.sameParameters = true
  }

  /**
   * For each edge A->B, jobs in stage A trigger jobs in stage B
   * If stage B contains no jobs, then jobs in stage A trigger jobs in stage C for all edges B->C
   * This method is idempotent as long as connectJobs(ItemSource,ItemSource) is also idempotent
   */
  protected Map<ItemSource,ItemSource> getDependencies() {
    Map<ItemSource,ItemSource> dependencies = [:]
    Closure connect
    connect = { ItemSource stageAJob, String stageB ->
      def stageBJobs = findJobs{stage, _ -> stage == stageB}
      if(stageBJobs?.size() == 0) {
        stageGraph.findAll { fromStage, _ -> fromStage == stageB }.values().each { stageC ->
          connect(stageAJob, stageC)
        }
      } else {
        stageBJobs.each { ItemSource toJob ->
          dependencies.put(stageAJob, toJob)
        }
      }
    }

    stageGraph.each { stageA, stageB ->
      findJobs{stage, _ -> stage == stageA}.each(connect.rcurry(stageB))
    }
    return dependencies
  }

  //Convenience methods

  //This method is expected to be idempotent
  protected ProxyDelegate injectComponent(ItemSource job, AbstractComponentType component) {
    job.components.add(component)
    return job.proxyMaker(job.context, job.defaults).generate(component.getName())
  }

  protected def findJobs(Closure pattern) {
    return jobs.findAll(pattern).values().flatten()
  }

  //Return jobs which are directly upstream of jobs in the given stage
  protected ItemSource[] jobsUpstreamOf(String downstreamStage) {
    return findJobs{ stage, _ -> stage == downstreamStage }.collect { ItemSource job ->
      jobsUpstreamOf(job)
    }.flatten()
  }

  protected ItemSource[] jobsUpstreamOf(ItemSource downstreamJob) {
    return getDependencies().collect { up, down ->
      down==downstreamJob ? up : []
    }.flatten()
  }

  protected ItemSource[] transitiveJobsUpstreamOf(ItemSource downstreamJob) {
    Closure<List<ItemSource>> recursiveUpstream
    recursiveUpstream = { ItemSource downstream ->
      def directUpstreamJobs = jobsUpstreamOf(downstream)
      if(directUpstreamJobs.size() == 0) {
        return []
      } else {
        return (directUpstreamJobs +
               directUpstreamJobs.collect(recursiveUpstream) +
               downstream).flatten()
      }
    }
    return jobsUpstreamOf(downstreamJob).collect(recursiveUpstream).flatten()
  }

  protected ItemSource[] jobsInStage(String stage) {
    findJobs { st, _ -> st == stage }
  }

  protected Set<String> getStages() {
    Set<String> set = []
    stageGraph.each { k, v ->
      set.add(k)
      set.add(v)
    }
    return set
  }

  protected void configureWorkspacePassing(Collection<ItemSource> jobs) {
    jobs.each { ItemSource job ->
      //Configuration for passing workspaces between stages
      //TODO: This ought to work with any other job in the dsl, not just the pipeline
      if (job.components.contains(CloneWorkspaceComponent.instance)) {
        String upstreamName = job.lookupValue('cloneWorkspace', 'upstream')
        def upstreamJobs = transitiveJobsUpstreamOf(job).findAll { ItemSource item ->
          return item.components.contains(TriggerDownstreamComponent.instance) &&
                  item.itemName == upstreamName
        }
        if(upstreamJobs.size() == 0) {
          throw new RuntimeException("Pipeline error: '${job.itemName}' requires workspace of non-upstream job '${upstreamName}'")
        }
        upstreamJobs.each { ItemSource upstream ->
          upstream.defaults.bind('triggerDownstream', 'copyWorkspace', true)
        }
        //TODO: This ought to be part of the clone workspace component
        if (job.components.contains(GitComponent.instance)) {
          println "WARNING: Git component cannot be used with clone workspace, disabling git component on ${job.itemName} job"
          job.components.remove(GitComponent.instance)
        }
        job.components.add(CloneWorkspaceComponent.instance)
      }
    }
  }
}
