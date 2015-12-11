package com.readytalk.gradle

import com.readytalk.jenkins.JenkinsClient
import com.readytalk.jenkins.ItemType
import com.readytalk.jenkins.JenkinsXmlAction
import com.readytalk.jenkins.JenkinsActions
import com.readytalk.jenkins.ModelGenerator
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

//generateItems() memoizes, and all JenkinsTask tasks depend on the initial generation task
@ParallelizableTask
class JenkinsTask extends AbstractTask {
  private ReadytalkJenkinsPlugin plugin
  private Boolean jobsChanged = false
  private JenkinsClient client = null
  JenkinsXmlAction xmlWriter = null

  @TaskAction
  void writeXml() {
    //TODO: Should we immediately fail if a REST call fails? Or go ahead and try to update the rest of the jobs anyways?
    plugin = project.plugins.findPlugin(ReadytalkJenkinsPlugin)
    client = new JenkinsClient(plugin.config.url, plugin.config.username, plugin.config.password)
    JenkinsXmlAction defaultAction = JenkinsActions.postJenkinsXml(client)
    xmlWriter = xmlWriter!=null ? xmlWriter : defaultAction

    plugin.generateItems().each { String path, items ->
      def results = ModelGenerator.executeXmlAction(path, items, xmlWriter)
      if(xmlWriter == defaultAction) {
        jobsChanged |= results.get(ItemType.job).inject(false) { boolean collector, name, result ->
          collector || (result == JenkinsActions.UpdateResult.changed)
        }
      }
    }
  }
}
