package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.jenkins.model.ProxyDelegate
import com.readytalk.jenkins.model.TemplateStr
import com.readytalk.util.StringUtils

import java.util.regex.Matcher

/**
 * Git component with syntactic sugar for Atlassian Stash and Github
 *
 * repo: 'project/repository', '~user.name/repository', or full ssh URL
 *       If blank, will base repo url on ownership.team and job as repository name
 * branches: list or space delimited string of branches to watch (defaults to master)
 * refspec: manually set refspec; overrides 'watch' parameter
 * trigger: set to empty string to disable automatic commit triggering
 * clean: whether to run git clean after checkout
 */
@Fixed
class GitComponent extends AbstractComponentType implements ExternalizedFields {
  String name = 'git'
  //Try not to report build status until all other publishers are done, except for trigger downstream
  int priority = 90

  Map<String,?> fields = [
          repo:           new TemplateStr('${ownership.vcsProject}/${base.name}'),
          branches:       'master',
          clean:          true,
          clearWorkspace: false,
          refspec:        '',
          trigger:        '#Git triggered polling',
          credentials:    '',
          server:         'github.com',
          provider:       new TemplateStr('${server == "github.com" ? "github" : ""}'),
          dsl:            {},
  ]

  //TODO: warn that git branch can't be externalized with multiple branches
  Map<String,String> externalizedFields = [
          branches: 'BRANCH'
  ]

  static Map<String,String> defaults(ProxyDelegate vars) {
    Map<String, String> result = [:]
    result.server = vars.server

    if (vars.repo =~ /^([^\/]+)\/([^\/]+)$/) {
      result.group = Matcher.lastMatcher[0][1]
      result.project = Matcher.lastMatcher[0][2]
    } else if (vars.repo =~ /^(?:https:\/\/|(?:(?:ssh:\/\/)?git@))?([^\/]+)(?::|\/)(\w+)\/([^\/]+?)(?:\.git)?$/) {
      result.server = Matcher.lastMatcher[0][1]
      result.group = Matcher.lastMatcher[0][2]
      result.project = Matcher.lastMatcher[0][3]
    }

    if(vars.provider == 'stash' && result.containsKey('group') && result.containsKey('project')) {
      String category = result.group.startsWith('~') ? 'users' : 'projects'
      result.browser = "https://${result.server}/${category}/${result.group}/repos/${result.project}"
      result.url = "ssh://git@${result.server}/${result.group}/${result.project}.git".toLowerCase()
    }

    if(vars.provider != 'github' && !result.containsKey('url')) {
      result.url = vars.repo
    }

    return result
  }

  Closure dslConfig = { vars->
    String repoRefspec

    if(vars.refspec == '') {
      repoRefspec = ['+refs/heads/*:refs/heads/origin/*',
       '+refs/heads/tags/*:refs/heads/origin/tags/*',
      ].join(' ')
    } else {
      repoRefspec = StringUtils.asString(vars.refspec, ' ')
    }

    String[] branchList = vars.branches instanceof String ? [vars.branches] : vars.branches

//    if(branchList.contains('pullRequest')) {
//      branchList.remove('pullRequest')
//      branchList.add()
//    }

    def map = GitComponent.defaults(vars)

    switch(vars.provider) {
      case 'stash':
        publishers {
          stashNotifier()
        }
        break
      case 'github':
        publishers {
          githubCommitNotifier()
        }
        break
    }

    scm {
      git {
        remote {
          if(vars.provider != 'github') {
            url(map.url)
            refspec(repoRefspec)
          } else {
            github("${map.group}/${map.project}", 'ssh')
            refspec(repoRefspec)
          }
          if(vars.credentials != '') {
            credentials(vars.credentials)
          }
        }
        branches(*branchList)
        if (vars.clean) clean()
        wipeOutWorkspace(vars.clearWorkspace)

        def dslBlock = vars.dsl.clone()
        dslBlock.setDelegate(getDelegate())
        dslBlock.call(vars)
      }
    }

    if(vars.credentials != '') {
      configure { node ->
        node / scm / userRemoteConfigs / 'hudson.plugins.git.UserRemoteConfig'
      }
    }

    if(vars.provider == 'stash' && map.containsKey('browser')) {
      configure { node ->
        node / scm / browser(class: 'hudson.plugins.git.browser.Stash') / url(map.browser)
      }
    }

    if (!vars.trigger.equals('')) {
      triggers {
        scm(vars.trigger)
      }
    }
  }
}
