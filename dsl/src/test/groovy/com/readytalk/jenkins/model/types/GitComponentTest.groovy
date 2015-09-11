package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.ContextMap
import com.readytalk.jenkins.model.ContextProxy
import com.readytalk.jenkins.model.DefaultContext
import com.readytalk.jenkins.model.ModelSpecification
import com.readytalk.jenkins.model.YamlParser
import spock.lang.Ignore

class GitComponentTest extends ModelSpecification {
  final static String DOMAIN = 'git.example.com'
  final static String GROUP = 'group'
  final static String PROJECT = 'project'
  final static String SSH = "ssh://git@${DOMAIN}"
  final static String HUB = "git@${DOMAIN}"

  def setup() {
    types {
      add ParameterizedComponent.instance
      add BasicJob.instance
      add GitComponent.instance
      add CommonComponent.instance
      add OwnershipComponent.instance
    }
  }

  @Ignore
  def "implicit project and repo from explicit url"(String url) {
    when:
    ContextMap context = itemDefaults(BasicJob.instance, [git: [repo: url], ownership: [team: '', vcsProject: '']])

    def git = new ContextProxy(registry, context).generate('git')
    def proxy = new ContextProxy(registry, context).generate()

    then:
    git.project.equals(PROJECT)
    git.group.equals(GROUP)
    git.server.equals(DOMAIN)

    where:
    url << ["ssh://git@${DOMAIN}/${GROUP}/${PROJECT}",
            "ssh://git@${DOMAIN}/${GROUP}/${PROJECT}.git",
            "https://${DOMAIN}/${GROUP}/${PROJECT}.git",]
  }

  def "can define stash repository via syntactic sugar"() {
    when:
    def context = componentContext ownership: [team: '', project: ''],
                                   git: [repo: shortUrl, provider: 'stash', server: DOMAIN]

    def git = new ContextProxy(registry, context).generate('git')
    def result = GitComponent.defaults(git)

    then:
    result.url =~ /^${repoUrl}(\.git)?$/

    where:
    shortUrl           | repoUrl
    'PROJECT/a-_x.4'   | "${SSH}/project/a-_x.4"
    '~user.name/repo'  | "${SSH}/~user.name/repo"
  }

  def "default repository defined from context"(Map<String,String> owner) {
    when:
    def context = componentContext base: [name: 'job'],
                                   git: [provider: 'stash', server: DOMAIN],
                                   ownership: owner

    def git = autoproxy(itemDefaults(BasicJob.instance, context)).git
    def url = GitComponent.defaults(git).url

    println "MAP: ${owner}"

    then:
    url =~ "team/job(.git)?\$"

    where:
    owner << [[vcsProject: 'TEAM'], [team: 'TEAM'], [team: 'WRONG', vcsProject: 'TEAM']]
  }

  //TODO: Can the github plugins in jenkins have domains other than github.com?
  def "sets values in upstream xml"() {
    when:
    def jobs = generate {
      basicJob('fake') {
        git {
          repo 'group/repo'
          server 'git.example.com'
          provider = hosting
        }
      }
    }

    def job = jobs.find { it.name.equals('fake') }.getNode()

    then:
    job.scm.userRemoteConfigs.'hudson.plugins.git.UserRemoteConfig'.url[0].value() == "${repoUrlPrefix}group/repo.git"
    job.scm.browser.url[0].value() == browserUrl

    where:
    hosting  | browserUrl                                      | repoUrlPrefix
    'stash'  | "https://${DOMAIN}/projects/group/repos/repo"   | "ssh://git@${DOMAIN}/"
    'github' | "https://github.com/group/repo/"                | "git@github.com:"
  }

  def "sets implicit values in upstream xml"() {
    when:
    def jobs = (generate << YamlParser.&parse).call("""
- basicJob:
    name: repo
    ownership:
      team: group
    git:
      server: ${DOMAIN}
      provider: ${hosting}
""")

    def job = jobs.find { it.name.equals('repo') }.getNode()

    then:
    job.scm.userRemoteConfigs.'hudson.plugins.git.UserRemoteConfig'.url[0].value() == "${repoUrlPrefix}group/repo.git"
    job.scm.browser.url[0].value() == browserUrl

    where:
    hosting  | browserUrl                                      | repoUrlPrefix
    'stash'  | "https://${DOMAIN}/projects/group/repos/repo"   | "ssh://git@${DOMAIN}/"
    'github' | "https://github.com/group/repo/"                | "git@github.com:"
  }

  def "externalizes branch parameter correctly"() {
    when:
    def jobs = generate {
      basicJob('repo') {
        git {
          branches = 'development'
        }
      }
    }

    def job = jobs.find { it.name.equals('repo') }.getNode()
    def paramXml = job.properties.'hudson.model.ParametersDefinitionProperty'.parameterDefinitions
    println paramXml

    then:
    paramXml.'hudson.model.StringParameterDefinition'.name[0].value() == 'BRANCH'
    paramXml.'hudson.model.StringParameterDefinition'.defaultValue[0].value() == 'development'
  }

  def "forces more reliable workspace cloning by default"() {
    when:
    def jobs = generate {
      basicJob('faux')
    }
    def job = jobs.find { it.name.equals('faux') }.getNode()

    then:
    job.scm.extensions.'hudson.plugins.git.extensions.impl.DisableRemotePoll'[0] != null
  }

  def "allows setting includedRegions for polling"() {
    when:
    def jobs = generate {
      basicJob('faux') {
        git {
          includedRegion 'subdirectory'
        }
      }
    }
    def job = jobs.find { it.name.equals('faux') }.getNode()

    then:
    job.scm.extensions.'hudson.plugins.git.extensions.impl.PathRestriction'.includedRegions[0].value() == 'subdirectory'
  }
}
