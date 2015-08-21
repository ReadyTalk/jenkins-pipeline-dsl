package com.readytalk.jenkins.model

import com.readytalk.jenkins.model.meta.JobTypeOverride

class MetaComponentTest extends ModelSpecification {

  def "job type override conflict detected"() {
    when:
    types {
      add fakeJobTypeOverride('fauxBuildFlow', 'flowJob')
      add fakeJobTypeOverride('fauxMatrix', 'matrixJob')
    }

    generate {
      basicJob('fakeJob') {
        fauxBuildFlow
        fauxMatrix
      }
    }

    then:
    def e = thrown(RuntimeException)
    e.message.contains("incompatible job types")
    e.message.contains("flowJob")
    e.message.contains("matrixJob")
  }

  AbstractComponentType fakeJobTypeOverride(String name, String job) {
    def fakeComponent = new ComponentType(name, [:], {})
    def jobTypeName = job
    fakeComponent.traits = [
            new JobTypeOverride() {
              @Delegate AbstractComponentType _ = fakeComponent
              String jobType = jobTypeName
            }
    ]
    assert fakeComponent instanceof AbstractComponentType
    return fakeComponent
  }
}
