package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ItemSource

/**
 * Pull Request component for Atlassian Stash
 *
 * Clones job configuration and attempts to disable things like
 */

@Fixed
class PullRequestComponent extends AbstractComponentType {
  final static String stashRefspec = '+refs/pull-requests/*/from:refs/remotes/origin/pr/*'
  final static String githubRefspec = '+refs/pull/*:refs/remotes/origin/pr/*'

  String name = 'pullRequest'

  Map<String,?> fields = [
          enabled: true,
          prefix: '',
          suffix: '-pr-builder',
          notifications: false,
  ]

  Closure dslConfig = { vars ->
    if(vars.enabled && vars.git?.provider == 'github') {
      //TODO: Support github pull request plugin?
    }
  }

  //Duplicates job with different name and git refspec
  ItemSource[] postProcess(ItemSource original) {
    if(!(original.context.lookup(name, 'enabled'))) {
      return [original]
    }

    //NOTE: Components generating multiple jobs like this MUST use the JobSource(JobSource,Boolean) constructor
    ItemSource prJob = new ItemSource(original)
    def ogProxy = original.proxyMaker(original.context).generate('git')

    //Overriding at 'user' scope because this job is "hidden" from the user DSL (added post-evaluation)
    prJob.proxyMaker(prJob.context, prJob.user).generate(name).with {
      prJob.itemName = "${prefix}${original.itemName}${suffix}"
      if(!notifications) {
        ownership.hipchatRooms = ''
        ownership.email = ''
      }
      //expand template
      git.repo = original.lookup('git','repo')
      if(prJob.components.contains(GitComponent.instance)) {
        switch (git.provider) {
          case 'stash':
            git.branches = 'origin/pr/**'
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
              """\n\nThis job is an auto-generated pull-request only clone of [${original.itemName}](${base.jenkins}/job/${original.itemName})"""
      //Disable timer for pull request jobs
      common.runSchedule = ''
      triggerDownstream.jobs = ''
    }

    prJob.defaults.context.putAll(prJob.user.context)

    return [original, prJob]
  }
}
