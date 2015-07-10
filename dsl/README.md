##Jenkins Component DSL
A set of abstractions around the jenkins-job-dsl project (from Netflix) to
provide modularity and conventions

Instead of providing syntactic sugar over the jenkins job XML, the component DSL
attempts to make it easy to create opinionated defaults and conventions that are
easy to override when necessary.

Example:
--------

```yaml
- ownership:
    team: CI
    email: ci@example.com

- basicJob:
    name: job-name
    shell:
      command: |
        echo 'Hello world!'

- gradleProject:
    name: gradle-plugins
    git:
      repo: team/repository
```


Groovy DSL equivalent:

```groovy
model {
  ownership {
    team 'CI'
    email 'ci@example.com'
  }

  basicJob('job-name') {
    //inherits default ownership from root
    shell {
      command = "echo 'Hello world!'"
    }
  }

  gradleProject('gradle-plugins') {
    git {
      repo = 'team/repository'
    }
  }
}
```

Syntax overview:

```yaml
#Creating a job
- JOB_TYPE:
    name: job-name-string
    #component configuration

#Adding a component to a job with default configuration
- JOB_TYPE:
    name: job-name
    COMPONENT_NAME:

#Adding/configuring a component on a job
- JOB_TYPE:
    name: job-name
    COMPONENT_NAME:
      FIELD: VALUE

#Configuring a component
- COMPONENT_NAME:
    FIELD: VALUE

#Configuring a component with a map value
- COMPONENT_NAME:
    MAPFIELD:
      KEY: VALUE
      KEY: VALUE

#Pipeline
- pipeline:
    name: pipeline-name
    STAGE_NAME:
      type: JOBTYPE
      COMPONENT_NAME:
        FIELD: VALUE
```

Groovy DSL equivalents:

Creating a job:

    JOB_TYPE('job-name-string')
    JOB_TYPE('job-name-string') {
      //component configuration
    }

Adding a component to a job with default configuration:

    JOB_TYPE('job-name') {
      COMPONENT_NAME
    }

Configuring a component:

    COMPONENT_NAME {
      FIELD VALUE
      //or
      FIELD = VALUE
      //or
      FIELD = [ KEY: VALUE, KEY: VALUE, ...]
    }

Pipeline: (STAGENAME varies by pipeline type)

    PIPELINE_TYPE('pipeline-name') {
      //pipeline-wide component configuration

      //pipeline jobs
      STAGE_NAME(type: 'JOBTYPE')
      STAGE_NAME(type: 'JOBTYPE') {
        //component configuration
      }
    }

On generation, it spits out a list of each job, the components applied to that
job, and the values calculated for each field of those components. It's also
color-coded (grey for component default, dark green for job defaults, and
brighter green for user-level context).


###Pipelines
1. `pipeline`
  * build -> test -> deploy -> functionalTest -> promote

2. `sequential`
  * Special: uses stage name as job suffix, automatically wires
    upstream/downstream jobs in order of declaration

```groovy
model {
  pipeline('myPipeline') {
    git {
      repo 'team/myRepository'
    }
    build(type: 'gradleProject')
    functionalTest(type: 'basicProject') {
      gradle 'e2eTest'
    }
  }
}
```

###Design notes

The DSL has two layers - the main user-facing layer is declarative and looks
similar to the original DSL

The second layer is type definition. The type DSL is an abstraction on top of
the upstream Jenkins DSL that allows modular composition of configuration logic
and default values.

Components make up the base layer, and are the only elements that can contain
upstream DSL logic. Each component has a set of named values with defaults that
can be transparently overridden by the user DSL.

Jobs are sets of components, and can optionally set their own default values for
those components.

When the job is constructed, each component looks up values first from the user
DSL (in ascending order of scope), then in job defaults (in ascending order of
scope) and finally from the default values defined by the component.

Pipelines are collections of jobs with additional inter-job configuration. As
they're more complex to implement, pipelines are defined as full classes and not
exposed in the type DSL.

For example:
------------
The constraints.buildHost default value is '', and the simpleGradle job type
overrides this default value to 'wheezyoffice'. However, the user has set the
value to 'squeeze' at the model root, which means it takes precedence over any
default value.

If the buildHost was again overridden by the 'example-job' declaration, then
that would take even higher precedence over the model root value.

The 'vars' parameter passed to the component closure is a proxy object that
transparently returns the correct value following the precedence rules outlined
above, and can also be used to reference values from other components.

```groovy
//Example only!
types {
  component('constraints', [buildHost: '']) { vars ->
    label(vars.buildHost)
  }

  job('simpleJob', ['constraints']) {
    constraints {
      buildHost 'wheezyoffice'
    }
  }
}

model {
  constraints {
    buildHost = 'squeeze'
  }
  simpleJob('example-job')
}
```

###Implementation: types

All types are named, and derive from NamedElementType. Type instances are
intended to be fixed data structures that act as a reference object, much like
Class objects in the JVM.

Whenever a type is referenced, its name is looked up in the type registry, which
is an instance of TypeRegistryMap. This map is instantiated by using reflection
to search for matching classes under the package 'com.readytalk.model':

* Any immutable singleton deriving from NamedElementType
* Any class deriving from AbstractPipeline with a corresponding getType method
* Any class implementing TypeRegistryBundle with a generateTypes method
  returning a TypeRegistryMap (merges with existing types, does not overwrite)

###Implementation: order of operations

0. Instantiate the type mapping by using reflection to search for types
1. Parse type dsl closures and add generated types to registry
2. Parse user model dsl into a tree of ModelElements
3. Evaluate ModelElements into a list of ItemSource bundles (proto-jobs
   containing components + context values)
    1. In each scope, components always get evaluated first
    2. Pipelines inject configuration after evaluating components and jobs
       belonging to that pipeline
4. Apply post-processing blocks to each item / job (ComponentTrait's
   `injectItem` method applied before `postProcess()`)
5. Execute component DSL blocks for each job using the bundled context against
   the upstream Job objects. ComponentTrait `injectContext` method called before
   passing ProxyDelegate to closure
   

NOTES:
Components' dsl block execution order is determined by the component priority
value; lower values mean higher priority
Pipeline logic can't rely on the results of post-processing blocks as it gets
injected first

###Component implementation notes

Most components should be straightforward mapping of parameters into the upstream DSL.

However, some components will need special behavior, such as pull requests
generating multiple jobs, or aggregating across scopes If it's absolutely
necessary, there is an optional `postProcess` method on components for these
special cases

###Cleanup work and polish

* There are still some edge cases that can cause order to matter unexpectedly
  - if two components use post-processing to the modify the same parameter
    defaults on another component, order of application will matter
  - if a component uses post-processing, any returned job must have the same
    component set as the original

###Future

* Explicit support for creating views, especially from pipelines
    - Enable restricting components by item type, e.g. job components shouldn't
      be applied to views

* Explicit support for folders

* Better validation and more user friendly error messages

* Explicit support for disabling/enabling components
    - This is currently done ad-hoc in some components by checking if a certain
      parameter is empty or false

* Duplicate job name detection
    - Only possible for jobs generated in the same run, otherwise we'd have to
      query the actual jenkins instance
    - Might be better handled by gradle plugin or a new jenkins plugin

###Further ideas for improvements

* Allow setting things as "defaults" in the user DSL
    - Otherwise, someone setting something like runSchedule for a group of
      projects will cause that value to be used even in downstream pipeline jobs
      that shouldn't trigger on a timer, since pipelines inject into the default
      context. Pipelines *could* inject into the user context, but that gets
      messy. One, they'd have to inject before any real user values are bound,
      which cements the post-processing issue. Two, it would further break things
      like checking the git repo for deployAction Alternatively, inject into
      user context after post-processing, but only if not already set by the
      immediate job context e.g. ignore outer scope

* Debug logging flag
    * It would be useful for both us and DSL users to see exactly what happens
      as the model is evaluated for debugging

* Extensible component types
    - Components are isolated and self-contained
    - There's no way to extend or copy component types

* DSL Escape Hatch [partial]
    - Partially supported now via the common.dsl component, though only allows a
      single closure to be used and cannot be composed

* Transparent translation to upstream DSL
    - In addition to our own components that combine various bits of logic, it
      would be interesting if we could transparently refer to the upstream DSL
      as if they were components
    - This would require some interesting bit of reflection trickery to inspect
      the upstream DSL's capabilities

* Optional type annotations / specification for component fields
    - Component field values are pretty ad-hoc, and can be anything from strings
      to lists to closures
    - So it would be useful if we could actual specify the set of allowed types
      while parsing

###Future (Workflow Plugin)

Once the jenkins workflow plugin is further developed, it would be neat if we
could adapt this to integrate with it as well as the existing jenkins job
DSL. This would allow us to make much more self-contained workflows that don't
require generating lots and lots of pipeline stage jobs, as well as easier
parallelization.

###Implementation high-level overview

Each element in the DSL has a named type, which belong to a few key type
categories.  Each named element type has a single instance representing that
type that can be referenced by name via the TypeRegistry instance.

- Components: encapsulate jenkins config behind a set of a parameters. All
  components derive from AbstractBaseComponent.
- Items: encapsulate collections of components along with a set of default
  values
- Groups: encapsulate collections of items; in the case of pipelines they also
  include inter-job configuration and wiring

All use of the upstream DSL is restricted to the component dslConfig
closures. The only direct interface point between the upstream DSL and this DSL
is the implementation of the DslDelegate interface used to generate each item.

The general rule is that any parameter should be overridable by the user if
needed.  Virtually all mutable data is represented as a tree of namespaced
parameter maps. Each scope has an associated context, which is further divided
into a 'user' context and 'defaults' context. User context always takes priority
over the defaults context values, regardless of scope. User DSL values always
bind to the user context, while job and pipeline values bind to the defaults
context. Post-process blocks in components usually bind to defaults, but are
allowed to bind to user context if needed (e.g. to forcibly override values in
pullRequest jobs).

Component types can also inject more complex behavior via a ComponentTrait -
this is useful for things like customizing how parameters get passed into the
config closure for the component. For example, ExternalizedFields enables
components to transparently "lift" string parameters into actual job parameter.

To ensure scoping is followed, getting and setting context values is abstracted
through ContextLookup and ContextBind.  Proxy objects are used as syntactic
sugar to mimic normal variable usage and hide the scoping abstractions.

###Implementation - YAML Support

Experimental support for YAML syntax has been added by translating YAML
structures to the groovy dsl parser. This avoids having to reimplement the
parser logic, and also allows us to reuse the same convenient test methods to
test yaml syntax as we can to test the original dsl. In theory, this same
pattern could be used to translate from any other JSON-compatible structures as
well.

Since YAML doesn't have methods, things that are method parameters in the groovy
dsl are exposed as direct properties:

```yaml
- gradleJob:
    name: hello-world
    gradle:
      tasks: build

- gradleJob: goodbye-world
```

```yaml
- pipeline:
    name: my-pipeline
    git:
      repo: my-pipeline-project
    build:
      type: gradleProject
```

Motivation:
-----------

The jenkins-job-dsl project provides a convenient way to build up jenkins
configuration without manually constructing raw xml, but it lacks convenient
ways to construct modular templates, organizational defaults, or multi-job flows
like pipelines.

The buildflow plugin provides a way to orchestrate pipelines, but it's rather
opaque and difficult to test locally, and still requires constructing the
individual jobs anyways. The workflow plugin also allows better pipeline
definitions, but it's very new and also doens't provide a nice way to setup
modular templates.

JJB (jenkins job builder) does provide a nice way to setup modular templates,
but it's still hard to setup cross-cutting defaults and opinionated pipelines
since the extension mechanism is locked to the XML-tree structure.

This project instead follows the gradle idea of strong
convention-over-configuration without having to sacrafice flexibility and
modularity.  While it uses the jenkins-job-dsl project under the hood, it's
meant to abstract away from the specific details of how jenkins plugins are
setup and interact.

For example, Jenkins has multiple plugins for triggering downstream builds, and
one of them sets it up as a manual trigger in a pipeline view. It's functionally
identical to a regular trigger aside from being manual, but it's a completely
different plugin with a different path in the xml and netflix dsl. Instead of
having to know about this quirk, we can just make a boolean parameter on the
triggerDownstream component that swaps them out automatically.
