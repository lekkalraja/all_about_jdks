# JAVA 10

##### Deprecations & Removals
*   Removed command-line tools/options
    * - javah -> `javac -h <dir>`
    - policytool
    - -X:prof -> jmap & 3rd party profilers

*   API's
    *   - java.security.acl -> java.security
    - java.security.{Certificate, Identity, IdentityScope, Signer}
    - javax.security.auth.Policy -> java.security.Policy


# Features

## Local-Variable Type Inference

##### Java SE 10 introduced type inference for local variables. Previously, all local variable declarations required an explicit (manifest) type on the left-hand side.

* Enhance the Java Language to extend **type inference to declarations of local variables** with initializers.
* `var` restricted to 
  - local variables with initializers, 
  - indexes in the enhanced for-loop, 
  - and locals declared in a traditional for-loop; 
* it would not be available for 
  - method formals, 
  - constructor formals, 
  - method return types,
  -  fields, 
  -  catch formals, 
  -  or any other kind of variable declaration.

* `var` is not Keyword.. it is `Reserved word`

### Non-denotable types

* Sometimes the type of the initializer is a non-denotable type, such as a capture variable type, intersection type, or anonymous class type. In such cases, we have a choice of whether to 
    * i) infer the type, 
    * ii) reject the expression, or 
    * iii) infer a denotable supertype.

### Risks and Assumptions
*   Risk: Because Java already does significant type inference on the RHS (lambda formals, generic method type arguments, diamond), there is a risk that attempting to use var on the LHS of such an expression will fail, and possibly with difficult-to-read error messages.

### Principles
*   P1. Reading code is more important than writing code.
*   P2. Code should be clear from local reasoning.
*   P3. Code readability shouldn't depend on IDEs.
*   P4. Explicit types are a tradeoff.

### Guidelines
* G1. Choose variable names that provide useful information.
* G2. Minimize the scope of local variables.
* G3. Consider var when the initializer provides sufficient information to the reader.
* G4. Use var to break up chained or nested expressions with local variables.
* G5. Don't worry too much about "programming to the interface" with local variables.
* G6. Take care when using var with diamond or generic methods.
* G7. Take care when using var with literals.

## Consolidate the JDK Forest into a Single Repository
* Combine the numerous repositories of the JDK forest into a single repository in order to simplify and streamline development.

## Garbage Collector Interface
* Improve the source code isolation of different garbage collectors by introducing a clean garbage collector (GC) interface.

### Goals
* Better modularity for HotSpot internal GC code
* Make it simpler to add a new GC to HotSpot without perturbing the current code base
* Make it easier to exclude a GC from a JDK build

## Parallel Full GC for G1
* Improve G1 worst-case latencies by making the full(old gen) GC parallel.
* The G1 garbage collector is designed to avoid full collections, but when the concurrent collections can't reclaim memory fast enough a fall back full GC will occur. The current implementation of the full GC for G1 uses a single threaded mark-sweep-compact algorithm. We intend to parallelize the mark-sweep-compact algorithm and use the same number of threads as the Young and Mixed collections do. The number of threads can be controlled by the `-XX:ParallelGCThreads` option, but this will also affect the number of threads used for Young and Mixed collections.

### Motivation
* The G1 garbage collector was made the default in JDK 9. The previous default, the parallel collector, has a parallel full GC. To minimize the impact for users experiencing full GCs, the G1 full GC should be made parallel as well.


### Reference : http://openjdk.java.net/projects/jdk/10/ <br> http://openjdk.java.net/projects/amber/LVTIstyle.html <br> http://openjdk.java.net/projects/amber/LVTIFAQ.html