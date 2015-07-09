package com.readytalk.jenkins.model.pipelines

import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.PipelineType
import groovy.transform.InheritConstructors

/**
 * Inverse of build flow's parallel directive
 * All jobs are implicitly sequential
 */
@InheritConstructors
class SequentialPipeline extends AbstractPipeline {
  static PipelineType type = new PipelineType('sequential', SequentialPipeline.class)

  Map<String, String> stageGraph = [:]

  @Override
  Map<String,List<ItemSource>> mapJobs(List<ItemSource> pipelineJobs) {
    int stageCounter = 0
    return pipelineJobs.collectEntries { ItemSource item ->
      stageGraph.put("${stageCounter}", "${stageCounter + 1}")
      stageCounter += 1
      item.itemName = "${project}-${item.itemName}"
      return ["${stageCounter}": [item]]
    }
  }

  def configureJobs() { }
}
