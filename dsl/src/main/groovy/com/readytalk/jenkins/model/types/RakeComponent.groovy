package com.readytalk.jenkins.model.types

import com.readytalk.jenkins.model.AbstractComponentType
import com.readytalk.jenkins.model.Fixed

@Fixed
class RakeComponent extends AbstractComponentType {
  String name = 'rake'

  Map<String,?> fields = [
    rubyVersion: '', // use local version by default (.ruby-version)
    tasks: ['ci']
  ]

  Closure dslConfig = { vars->
    wrappers {
      rbenv(vars.rubyVersion) {
        gems('bundler', 'rake')
      }
    }

    steps {
      shell("""bundle install
rbenv rehash
bundle exec rake ${vars.tasks.join(' ')}""")
    }
  }
}
