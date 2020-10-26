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

### Reference : http://openjdk.java.net/projects/jdk/10/ <br> http://openjdk.java.net/projects/amber/LVTIstyle.html <br> http://openjdk.java.net/projects/amber/LVTIFAQ.html
