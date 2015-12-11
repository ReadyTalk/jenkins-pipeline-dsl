package com.readytalk.jenkins

import spock.lang.Specification

class JenkinsTest extends Specification{
  def "maps folder paths to correct REST prefix"() {
    expect:
    JenkinsActions.folderUriPath(folderPath) == restPrefix

    where:
    folderPath    | restPrefix
    ''            | ''
    '/'           | ''
    '/folder'     | '/job/folder'
    'folder'      | '/job/folder'
    'outer/inner' | '/job/outer/job/inner'
  }
}
