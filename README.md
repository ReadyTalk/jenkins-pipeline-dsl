#ReadyTalk Jenkins DSL

[![Build Status](https://travis-ci.org/ReadyTalk/jenkins-pipeline-dsl.svg?branch=master)](https://travis-ci.org/ReadyTalk/jenkins-pipeline-dsl)

Built on top of the upstream Netflix DSL, but otherwise represents a separate
layer This ensures that we can swap out the underlying system somewhat, such as
making use of the workflow plugin once it matures more.

[DSL README](dsl/README.md)

##Getting started:

For a simple github repo with a shell command:

jobs.yml:

```yaml
- basicJob:
    git:
      repo: owner/repository
    shell:
      command: |
        make
```

Then in your build.gradle (assuming a seed job that sets the APIKEY token):

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "com.readytalk.jenkins:plugin:0.10.0"
  }
}

plugins.apply 'com.readytalk.jenkins'

jenkins {
  url = 'https://jenkins.example.com'
  user = 'jenkins'
  password = System.env.APIKEY

  dsl file('jobs.yml')
}
```

For now, the gradle plugin is the only interface, but a proper CLI-based
interface is planned for those not using gradle.

##Plugin

Integrates the component dsl into Gradle and provides a smart interface to
Jenkins

Features:
  * Doesn't change whether a job is disabled when modifying configs in Jenkins
  * Can dump generated xml to locally as well as push to a running Jenkins
    instance
  * Native support for component dsl model blocks
  * Supports YAML-based syntax in addition to groovy dsl

Requirements:
  * Gradle 2.1+
  * Java 8 (may be relaxed to Java 7, but I'd rather encourage adoption of 8)

Limitations:
  * Supports views in theory, but untested and unsupported by the component dsl
  * No folder support yet
  * Can't replace jobs of different jenkins archetypes (e.g. replace maven job
    type with a regular job)
  * Currently only supports regular "freestyle" jobs

```yaml
- basicJob:
    name: myJob
    pullRequest:
```

```groovy
jenkins {
  url 'https://example.com/jenkins'
  model {
    basicJob('myJob') {
      pullRequest
    }
  }
}
```
It can also read dsl script files:

```
jenkins {
  dsl file('jenkins.groovy')
}
```

```
//jenkins.groovy
model {
  basicJob('myJob')
}
```

Open Source Progress:
---------------------
  * No Jenkins plugin validation - if the DSL references plugins you don't have
    installed on Jenkins, it won't work
  * fitnesse component and jobs are very niche to the point of probably being
    ReadyTalk-specific

Other notes: Plugin validation exists in the upstream DSL when running through
Jenkins directly - we might be able to pull that in
