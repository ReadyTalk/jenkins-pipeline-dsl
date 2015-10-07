package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource
import com.readytalk.util.ClosureGlue

/**
 * Pull Request component for Atlassian Stash
 *
 * Clones job configuration and attempts to disable things like
 */

@Fixed
class PullRequestComponent extends AbstractComponentType {
  final static String stashRefspec = '+refs/pull-requests/*/from:refs/remotes/origin/pull-requests/*/from'
  final static String githubRefspec = '+refs/pull/*:refs/remotes/origin/pr/*'

  String name = 'pullRequest'

  Map<String,?> fields = [
          enabled: true,
          prefix: '',
          suffix: '-pr-builder',
          notifications: false,
          mergeTo: 'master',
          //TODO: How can we support this?
          //overrides: {}, //SPECIAL
  ]

  Closure dslConfig = { vars ->
    if(vars.enabled && vars.git?.provider == 'github') {
      //TODO: Support github pull request plugin?
    }
  }

  //Duplicates job with different name and git refspec
  ItemSource[] postProcess(ItemSource original) {
    if(!(original.context.lookup(name, 'enabled').get())) {
      return [original]
    }

    //NOTE: Components generating multiple jobs like this MUST use the ItemSource(ItemSource,Boolean) constructor
    ItemSource prJob = new ItemSource(original)
    def ogProxy = original.proxyMaker(original.context).generate('git')

    //Overriding at 'user' scope because this job is "hidden" from the user DSL (added post-evaluation)
    //i.e. users wouldn't be able to override things in this scope anyways
    prJob.proxyMaker(prJob.context, prJob.user).generate(name).with {
      prJob.itemName = "${prefix}${original.itemName}${suffix}"
      if(!notifications) {
        ownership.hipchatRooms = ''
        ownership.email = ''
      }

      //Enable simultaneous pr builds unless parent scope explicitly disabled them
      Boolean concurrent = original.user.lookup('common', 'concurrentBuild')
      common.concurrentBuild = concurrent != null ? concurrent : true

      //expand template
      git.repo = original.lookupValue('git','repo')
      if(prJob.components.contains(GitComponent.instance)) {
        switch (git.provider) {
          case 'stash':
            git.branches = 'origin/pull-requests/*/from'
            git.refspec = stashRefspec
            break
          case 'github':
            git.refspec = githubRefspec
            git.branches = '${sha1}'
            break
        }
      }
      enabled = false
      common.description = common.description +
              """\n\nThis job is a pull-request only clone of [${original.itemName}](${base.jenkins}/job/${original.itemName})"""
      //Disable timer for pull request jobs
      common.runSchedule = ''
      triggerDownstream.jobs = ''

      //Merge to specified branch before building, preserve any existing git customizations
      //NOTE: GIT_BRANCH will report as the target branch instead of the pr if this is enabled!
      if(mergeTo) {
        String mergeTarget = mergeTo
        git.dsl = ClosureGlue.combinePreservingDelegate(git.dsl, {
          mergeOptions('origin', mergeTarget)
          localBranch("pr-\${BUILD_NUMBER}-to-${mergeTarget}")
        })
        base.dsl = ClosureGlue.combinePreservingDelegate(base.dsl, {
          configure { Node node ->
            node / 'scm' / 'extensions' / 'hudson.plugins.git.extensions.impl.ChangelogToBranch' / 'options' << {
              compareRemote('origin')
              compareTarget(mergeTarget)
            }
          }
        })
      }
    }

    prJob.defaults.context.putAll(prJob.user.context)

    return [original, prJob]
  }
}
