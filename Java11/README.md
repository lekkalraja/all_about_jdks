# JAVA 11

## JEP 181: Nest-Based Access Control

* `Nests` an access control context that aligns with the existing notion of nested types in the java.
* `Nests` allow classes that are logically part of the same code entity, but which are compilled to distinct class files, to access each others private members `without the need for compilers to insert accessibility-broadening bridge methods`.

#### Motivation

* Many JVM languages support multiple classes in a single source file (such as java's inner(nested) classes), or translate non-class source artifacts to class files.
* From a user perspective, however these are generally considered to be all in the same class and therefore users expect them to share a common access control regime.
* To presever these expectations, compilers frequently have to broden the access of private members to package, through the addition of access bridges. an invocation of a private member is compilled into a invocation of a compiler-generated package-private method in the target class, which in turn accesses the intended private member.
* These brdiges subvert encapsulation, slightly increase the size of a deployed application, and can confuse users and tools.
* A formal notion of a group of class files forming a `nest`, where nest mates share a common access control mechanism, allows the desired result to be directly achieved in a simpler, more secure, more transparent manner.
* The notion of a common access control context arises in other places as well, such as the host class mechanism in `Unsafe.defineAnonymousClass()`, where a dynamically loaded class can use the access control context of a host.
* A formal notion of nest membership would put this mechanism on firmer ground.

#### Description
* The Java Language Specification allows classes and interfaces to be nested within each other. Within the scope of a top-level declaration, any number of types can appear nested.
* These nested types have unrestricted access to each other, including to private fields, methods and constructors.
* we can descrie a top-level type, plus all types nested within it, as forming a `nest`, and two members of a nest are described as `nestmates`.
* The private access is complete within the whole declaration of the containing top-level type. (one can think of this as a top-level type defining a sort of `mini-package`, within which extra access is granted, even beyond that provided to other members of the same java package).
* Today private access between nestmates is not permitted by the JVM access rules. To provide the permitted access, a java source code compiler has to introduce a level of indirection. (FOr ex, an invocation of a private member is compilled into an invocation of a compiler-generated package-private, bridging method in the target class, which in turn invokes the intended private method)

#### Nest Class File Attributes

* The existing classfile format defines the InnerClasses and EnclosingMethod attributes to allow Java source code compilers (`javac`) to reify the source level nesting relationship.
* Each nested type is compiled to it's own class file, with different class files `linked` by the value of these attributes.
* While these attributes are enough for the JVM to determine nestmate-ness.
* To allow for a broader, more general, notion of nestmates beyond simply java nested types, and for the sake of efficient access control checking, it is proposed to modify the class file format to define `two new attributes`.
* One nest member (typically the top-level class) is designated as the `NestHost` 
* Another attribute (NestMembers) to identify the other statically known nest members. Each of the other nest members has an attribute (`NestHost`) to identify it's nest host

#### JVM Access Control for Nestmates

* A field or method R is accessible to a class or interface D if and only if any if R is private and is declared in a different class or interface C, and C and D, are nestmates.
* For types C and D to be nestmates they must have the same nest host. A type C claims to be a member of the nest hosted by D, if it lists D in it's NestHost attribute.
* The membership is validated if D also lists C in it's NestMembers attribute. D is implicitly a member of the nest that it hosts
* A class with no NestHost or NestMembers attribute, implicitly forms a nest with itself as the nest host, and sole nest member

```java
    > Output from the NestBasedAcccessControl Class
    raja@raja-Latitude-3460:~/Documents/coding/all_about_jdks/Java11$ java NestBasedAccessControl 
    Inner Class Nest Host : NestBasedAccessControl
    Outer Class Nest Host : NestBasedAccessControl
    Inner Class Nest Members : NestBasedAccessControl  NestBasedAccessControl$InnerClass
    Outer Class Nest Members : NestBasedAccessControl  NestBasedAccessControl$InnerClass
```

#### Nestmate Reflection API
* java.lang.Class:getNestHost, getNestMembers and isNestmateOf


## JEP 309: Dynamic Class-File Constants

* Extend the java class-file format to support a new constant-pool form, CONSTANT_Dynamic. Loading a CONSTAT_Dynamic will delegate creation to a bootstrap method, just as linking an invokedynamic call site delegates linkage to a bootstrap method.

## JEP 315: Improve Aarch64 Intrinsics

* Imporve the existing string and array `intrinsics`, and implement new intrinsics for the java.lang.Math sin, cos and log functions on AArch64 processors.
* `Intrinsics` are used to leverage CPU architecture-specific assembly code which is executed instead of generic java code for a given method to imporve performance.
* While most of the intrinsics are already implemented in AArch64 port, optimized intrinsics for the sin, cos, log java.lang.Math methods are still missing.
* This JEP is intended to cover gap by implementing optimized intrinsics for above methods at the same time, while most of the intrinsics are already implemented in the AArch64 port, the current implementation of some intrinsics may not be optimal.
* Specifically, some intrinsics for AArch64 architectures may benefit from software prefetching instructions, memory address alignment, instructions placement for mult-pipeline CPUs, and the replacement of certain instruction patterns with faster ones or with SIMD instructions.
* This includes (but is not limited to) such typical operations as String::compareTo, String::indexOf, StringCoding::hasNegatives, Arrays::equals, StringUTF16::compress, StringLatin1::inflate and various checksum claculations.
  
## JEP 318: Epsilon: A No-Op Garbage Collector (Experimental)

* A Garbage Collector that handles memory allocaiton but does not implement any actual memory reclamation mechanism. Once the available Java heap is exhausted, the JVM will shut down.
* Provides a Completely passive GC implementation with a bounded allocation limit and the lowest latency overhead possible, at the expense of memory footprint and memory throughput.
* A successful implementation is an isolated code change, does not touch other GCs, and makes minimal changes in the rest of JVM.

* There are a few use cases where a trivial no-op GC proves useful:
  - Performance testing
  - Memory pressure testing
  - VM interface testing
  - Extremely short lived jobs
  - Last-drop latency improvements
  - Last-drop throughput improvements

* Epsilon GC looks and feels like any other OpenJDK GC, enabled with `-XX:+UseEpsionGC`
* Epsilon GC works by implementing linear allocaiton in a single contiguous chunk of allocated memory.
* The barrier set used by Epsilon is completely empty/no-op, because the GC does not do any GC cycles, and therefore does not care about the object graph, object marking, object copying, etc..
* Since the only important part of the runtime interface for Epsilon is that for issuing TLAB (thread-local allocated buffer), it's latency largely depends on the TLAB size issued. With arbitarily large TLABs and arbitarily large heap, the latency overhead can be described b an arbitarily low positive value, hence the name. (Alternative origin story: `epsilon` frequently means `empty symbol`), which is aligned with the no-op nature of this GC)
* Once the Java heap is exhausted, no allocaiton is possible, no memory reclamation is possible,  and therefore we have to fail. There are several options at that point; most are in line with what existing GCs do
    * Throw OutOfMemoryError with a descriptive message
    * Perform a heap dump (enabled, as usual, with `-XX:+HeapDumpOnOutOfMemoryError`)
    * Fail the JVM hard and optionally perform external action (through the usual `-XX:OnOutOfMemoryError=...`)

* There is nothing to be done on `System.gc()` call, because no memory relcamation code is implemented.

## JEP 320: Remove the Java EE and CORBA Modules

* Java SE 6 included a full Web Services stack for the convenience of Java developers. The stack consisted of four technologies that were originally developed for the Java EE Platform
    *  JAX-WS (Java API for XML-Based Web Services)
    *  JAXB (Java Architecture for XML Binding)
    *  JAF (the JavaBeans Activation Framework)
    *  Common Annotations
* The Java EE technologies are readily available from third-party sites, such as Maven Central, there is no need for the Java SE Platform or the JDK to include them.

* In Java SE 9, the Java SE modules that contain Java EE and CORBA technologies are annotated as deprecated for removal, indicating the intent to remove them in a future release:

    1. java.xml.ws (JAX-WS, plus the related technologies SAAJ and Web Services Metadata)
    2. java.xml.bind (JAXB)
    3. java.activation (JAF)
    4. java.xml.ws.annotation (Common Annotations)
    5. java.corba (CORBA)
    6. java.transaction (JTA)
* Related modules in Java SE 9 are also deprecated for removal:
    7. java.se.ee (Aggregator module for the six modules above)
    8. jdk.xml.ws (Tools for JAX-WS)
    9. jdk.xml.bind (Tools for JAXB)

* This JEP will remove the nine modules listed above:
    * Their source code will be deleted from the OpenJDK repository.
    * Their classes will not exist in the JDK runtime image.
    * Their tools will no longer be available:
    * wsgen and wsimport (from jdk.xml.ws)
    * schemagen and xjc (from jdk.xml.bind)
    * idlj, orbd, servertool, and tnamesrv (from java.corba)
    * The JNDI CosNaming provider (from java.corba) will no longer be available.
    * No command line flag will be capable of enabling them, as --add-modules does on JDK 9.
    * The rmic compiler will be updated to remove the -idl and -iiop options. Consequently, rmic will no longer be able to generate IDL or IIOP stubs and tie classes.


# Reference : [Java 11](http://openjdk.java.net/projects/jdk/11/)