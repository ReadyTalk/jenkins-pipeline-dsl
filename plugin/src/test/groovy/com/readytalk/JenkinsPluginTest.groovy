package com.readytalk

import com.readytalk.gradle.JenkinsTask
import com.readytalk.gradle.ReadytalkJenkinsPlugin
import com.readytalk.jenkins.ItemType
import com.readytalk.jenkins.JenkinsXmlAction
import nebula.test.PluginProjectSpec
import spock.lang.Ignore

class JenkinsPluginTest extends PluginProjectSpec {
  final String pluginName = 'com.readytalk.jenkins'

  def "can evaluate simple dsl block"() {
    boolean works = false
    when:
    project.with {
      plugins.apply pluginName
      jenkins {
        model {
          basicJob('hello-world')
        }
      }

      task('jenkinsTest', type: JenkinsTask) {
        xmlWriter = { path, type, name, xml ->
          works = true
          //passthrough
          new MapEntry(name,xml)
        } as JenkinsXmlAction
      }
    }

    project.tasks.getByName('jenkinsTest').writeXml()

    then:
    works
  }

  def "allows defining defaults for all model blocks"() {
    when:
    project.with {
      plugins.apply pluginName
      file('test.yml').text = """
- common:
    buildHost: default-linux
"""
      jenkins {
        //defaults file('test.yml')
        defaults {
          common {
            buildHost 'default-linux'
          }
        }
        dsl {
          basicJob('hello-world')
        }
      }
    }

    ReadytalkJenkinsPlugin plugin = project.plugins.findPlugin(pluginName)
    plugin.generateItems()
    Node xml = plugin.items.get('').get(ItemType.job).find { it.key == 'hello-world' }.value
    println xml.assignedNode

    then:
    xml.assignedNode[0].value() == 'default-linux'
  }
}
