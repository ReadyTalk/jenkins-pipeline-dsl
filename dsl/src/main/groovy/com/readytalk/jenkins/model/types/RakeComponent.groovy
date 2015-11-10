package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed
import com.readytalk.jenkins.model.ProxyDelegate

@Fixed
class RakeComponent extends AbstractComponentType {
  String name = 'rake'

  Map<String,?> fields = [
    rubyVersion: '', // use local version by default (.ruby-version)
    gems: [:],
    tasks: ['ci']
  ]

  Closure dslConfig = { ProxyDelegate vars ->
    String preinstallGems = vars.gems.collect { gem, version ->
      version ? "${gem} -v ${version}" : gem
    }.collect { arg ->
      "gem install ${arg}"
    }.join('\n')

    wrappers {
      rbenv(vars.rubyVersion)
    }

    steps {
      shell("""${preinstallGems}
bundle install
rbenv rehash
bundle exec rake ${vars.tasks.join(' ')}""")
    }
  }
}
