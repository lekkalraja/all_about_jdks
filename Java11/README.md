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

## JEP 321: HTTP Client

* Define a new HTTP client API that implements HTTP/2 and WebSocket, and can replace the legacy HttpURLConnection API
* The existing HttpURLConnection API and its implementation have numerous problems:
    * The base URLConnection API was designed with multiple protocols in mind, nearly all of which are now defunct (ftp, gopher, etc.).
    * The API predates HTTP/1.1 and is too abstract.
    * It is hard to use, with many undocumented behaviors.
    * It works in blocking mode only (i.e., one thread per request/response).
    * It is very hard to maintain.
#### Goals

* Must be easy to use for common cases, including a simple blocking mode.
* Must provide notification of events such as "headers received", errors, and "response body received". This notification is not necessarily based on callbacks but can use an asynchronous mechanism like `CompletableFuture`.
* A simple and concise API which caters for 80-90% of application needs. This probably means a relatively small API footprint that does not necessarily expose all the capabilities of the protocol.
* Must expose all relevant aspects of the HTTP protocol request to a server, and the response from a server (headers, body, status codes, etc.).
* Must support standard and common authentication mechanisms. This will initially be limited to just Basic authentication.
* Must be able to easily set up the WebSocket handshake.
* Must support HTTP/2. (The application-level semantics of HTTP/2 are mostly the same as 1.1, though the wire protocol is completely different.)
* Must be able to negotiate an upgrade from 1.1 to 2 (or not), or select 2 from the start.
* Must support server push, i.e., the ability of the server to push resources to the client without an explicit request by the client.
* Must perform security checks consistent with the existing networking API.
* Should be friendly towards new language features such as lambda expressions.
* Should be friendly towards embedded-system requirements, in particular the avoidance of permanently running timer threads.
* Must support HTTPS/TLS.
    * Performance requirements for HTTP/1.1:
    * Performance must be on par with the existing HttpURLConnection implementation.
    * Performance must be on par with the Apache HttpClient library and with Netty and Jetty when used as a client API.
    * Memory consumption of the new API must be on par or lower than that of HttpURLConnection, Apache HttpClient, and Netty and Jetty when used as a client API.
* Performance requirements for HTTP/2:
* Performance must be better than HTTP/1.1 in the ways expected by the new protocol (i.e., in scalability and latency), notwithstanding any platform limitations (e.g., TCP segment ack windows).
* Performance must be on par with Netty and Jetty when used as a client API for HTTP/2.
* Memory consumption of the new API must be on par or lower than when using HttpURLConnection, Apache HttpClient, and Netty and Jetty when used as a client API.
* Provide a standardized API, in the java.net.http package, based upon the incubated API, and
Remove the incubated API. 

* The API provides non-blocking request and response semantics through CompletableFutures, which can be chained to trigger dependent actions. 
* Back-pressure and flow-control of request and response bodies is provided for via the Platform's `reactive-streams` support in the java.util.concurrent.Flow API.
* The implementation is now completely asynchronous (the previous HTTP/1.1 implementation was blocking)
* Use of the RX Flow concept has been pushed down into the implementation, which eliminated many of the original custom concepts needed to support HTTP/2. 
* The flow of data can now be more easily traced, from the user-level request publishers and response subscribers all the way down to the underlying socket. 
* This significantly reduces the number of concepts and complexity in the code, and maximizes the possibility of reuse between HTTP/1.1 and HTTP/2.
* The module name and the package name of the standard API will be java.net.http.

##### Changes over what was incubated in JDK 10
  1. The predefined implementation of BodyPublisher, BodyHandler, and BodySubscriber, created through static factory methods, have been moved out to separate non-instantiable utility factory classes, following the pluralized naming convention. This improves readability of these relatively small interfaces.
  2. The names of the static factory methods have also been updated along the following broad categories:
     1. fromXxx: Adapters from standard Subscriber, e.g. takes a Flow.Subscriber returns a BodySubscriber
     2. ofXxx: Factories that create a new pre-defined Body[Publisher|Handler|Subscriber] that perform useful common tasks, such as handling the response body as a String, or streaming the body to a File.
     3. other: Combinators (takes a BodySubscriber returns a BodySubscriber) and other useful operations.
  3. A few BodyHandlers and corresponding BodySubscribers have been added, to improve usability in common scenarios:
     1. discard(Object replacement) combined discarding/ignoring the response body and allowing a given replacement. Feedback has indicated that this could appear confusing. It has been removed and replaced with two separate handlers: 1) discarding(), and 2) replacing(Object replacement).
     2. Added ofLines() that returns a BodyHandler<Stream<String>>, to support streaming of response body as a Stream of lines, line by line. Provides similar semantics to that of BufferedReader.lines().
     3. Added fromLineSubscriber​, that supports adaptation of response body to a Flow.Subscriber of String lines.
     4. Added BodySubscriber.mapping for general purpose mapping from one response body type to another.
     5. The push promise support has been re-worked to reduce its impact on the API and bring it more in line with regular request/responses. Specifically, the MultiSubscriber and MultiResultMap have been removed. Push promises are now handled through a functional interface, PushPromiseHandler, that is optionally given during a send operation.
     6. The HttpClient.Redirect policy has been simplified, by replacing SAME_PROTOCOL and SECURE policies, with NORMAL. It has been observed that the previously named SECURE was not really appropriately named and should be renamed to NORMAL, since it will likely be suitable for most normal cases. Given the newly named, aforementioned, NORMAL, SAME_PROTOCOL appears oddly named, possibly confusing, and not likely to be used.
     7. WebSocket.MessagePart has been removed. This enum was used on the receiving side to indicate whether the delivery of a message is complete, or not. It is asymmetric with the sending side, which uses a simple boolean for this purpose. Additionally, it has been observed that handling received messages with a simple boolean significantly reduces and simplifies the receiving code logic. Determination of messages being delivered as a WHOLE, one of the benefits and the main purposes for the aforementioned MessagePart, has proved to not carry its own weight.

###### Sample
    ```java

    > java Java11/NewHttpClient.java  => Have Both Sync and Async versions of HttpClient-Get Calls
    
    ```

##### Alternatives

* A number of existing HTTP client APIs and implementations exist, e.g., Jetty and the Apache HttpClient. Both of these are both rather heavy-weight in terms of the numbers of packages and classes, and they don't take advantage of newer language features such as lambda expressions.

## JEP 323: Local-Variable Syntax for Lambda Parameters
* Allow var to be used when declaring the formal parameters of implicitly typed lambda expressions.
* Align the syntax of a formal parameter declaration in an implicitly typed lambda expression with the syntax of a local variable declaration.
* For uniformity with local variables, we wish to allow 'var' for the formal parameters of an implicitly typed lambda expression:
  `(var x, var y) -> x.process(y)   // implicit typed lambda expression`
* One benefit of uniformity is that modifiers, notably annotations, can be applied to local variables and lambda formals without losing brevity:

```java
@Nonnull var x = new Foo();
(@Nonnull var x, @Nullable var y) -> x.process(y)
```
* For formal parameters of implicitly typed lambda expressions, allow the reserved type name var to be used, so that:
```java
(var x, var y) -> x.process(y) is equivalent to: (x, y) -> x.process(y)
```

* An implicitly typed lambda expression must use var for all its formal parameters or for none of them. In addition, var is permitted only for the formal parameters of implicitly typed lambda expressions --- explicitly typed lambda expressions continue to specify manifest types for all their formal parameters, so it is not permitted for some formal parameters to have manifest types while others use var. 

* The following examples are illegal:
(var x, y) -> x.process(y)         // Cannot mix 'var' and 'no var' in implicitly typed lambda expression
(var x, int y) -> x.process(y)     // Cannot mix 'var' and manifest types in explicitly typed lambda expression

###### Sample

```java
 > java Java11/LocalVariableSyntaxForLambda.java
```

## JEP 324: Key Agreement with Curve25519 and Curve448

* RFC 7748 defines a key agreement scheme that is more efficient and secure than the existing elliptic curve Diffie-Hellman (ECDH) scheme. The primary goal of this JEP is an API and an implementation for this standard. Additional implementation goals are:
    * Develop a platform-independent, all-Java implementation with better performance than the existing ECC (native C) code at the same security strength.
    * Ensure that the timing is independent of secrets, assuming the platform performs 64-bit integer addition/multiplication in constant time. In addition, the implementation will not branch on secrets. These properties are valuable for preventing side-channel attacks.
* Cryptography using Curve25519 and Curve448 is in demand due to their security and performance properties. Key exchange using these curves is already supported in many other crypto libraries such as OpenSSL, BoringSSL, and BouncyCastle. This key exchange mechanism is an optional component of TLS 1.3, and is enabled in earlier TLS versions through commonly-used extensions.

* The X25519 and X448 functions will be implemented as described in RFC 7748, and these functions will be used to implement new KeyAgreement, KeyFactory, and KeyPairGenerator services in the existing SunEC provider. 
* The implementation will use the constant-time Montgomery ladder method described in RFC 7748 in order to prevent side channel attacks. The implementation will ensure contributory behavior by comparing the result to 0 as described in the RFC.

```java

Example API usage:

KeyPairGenerator kpg = KeyPairGenerator.getInstance("XDH");
NamedParameterSpec paramSpec = new NamedParameterSpec("X25519");
kpg.initialize(paramSpec); // equivalent to kpg.initialize(255)
// alternatively: kpg = KeyPairGenerator.getInstance("X25519")
KeyPair kp = kpg.generateKeyPair();

KeyFactory kf = KeyFactory.getInstance("XDH");
BigInteger u = ...
XECPublicKeySpec pubSpec = new XECPublicKeySpec(paramSpec, u);
PublicKey pubKey = kf.generatePublic(pubSpec);

KeyAgreement ka = KeyAgreement.getInstance("XDH");
ka.init(kp.getPrivate());
ka.doPhase(pubKey, true);
byte[] secret = ka.generateSecret();

```

## JEP 327: Unicode 10

* Upgrade existing platform APIs to support version 10.0 of the Unicode Standard.
* Support the latest version of Unicode, mainly in the following classes:
    * Character and String in the java.lang package,
    * NumericShaper in the java.awt.font package, and
    * Bidi, BreakIterator, and Normalizer in the java.text package.
* Java SE 10 implements Unicode 8.0. Unicode 9.0 adds 7,500 characters and six new scripts, and Unicode 10.0.0 adds 8,518 characters and four new scripts. This upgrade will include the Unicode 9.0 changes, and thus will add a total of 16,018 characters and ten new scripts.

## JEP 328: Flight Recorder

* Provides a low-overhead data collection framework for troubleshooting Java applications and the HotSpot JVM.
* Provide APIs for producing and consuming data as events
* Provide a buffer mechanism and a binary data format
* Allow the configuration and filtering of events
* Provide events for the OS, the HotSpot JVM, and the JDK libraries

###### Non-Goals
* Provide visualization or analysis of collected data
* Enable data collection by default

#### Motivation
* Troubleshooting, monitoring and profiling are integral parts of the development lifecycle, but some problems occur only in production, under heavy load involving real data.
* Flight Recorder records events originating from applications, the JVM and the OS. Events are stored in a single file that can be attached to bug reports and examined by support engineers, allowing after-the-fact analysis of issues in the period leading up to a problem. 
* Tools(`Java Mission Control (JMC)`) can use an API to extract information from recording files.

* JEP 167: Event-Based JVM Tracing added an initial set of events to the HotSpot JVM. Flight Recorder will extend the ability to create events to Java.
* JEP 167 also added a rudimentary backend, where data from events are printed to stdout. Flight Recorder will provide a single high-performance backend for writing events in a binary format.
  
##### Modules:
    * jdk.jfr
        * API and internals
        * Requires only java.base (suitable for resource constrained devices)
    * jdk.management.jfr
        * JMX capabilities
        * Requires jdk.jfr and jdk.management

* Flight Recorder can be started on the command line: `$ java -XX:StartFlightRecording ...`
  
* Recordings may also be started and controlled using the bin/jcmd tool:
```shell
$ jcmd <pid> JFR.start
$ jcmd <pid> JFR.dump filename=recording.jfr
$ jcmd <pid> JFR.stop

```
* This functionality is provided remotely over JMX, useful for tools such as `Mission Control`.

##### Producing and consuming events
There is an API for users to create their own events:

```java

import jdk.jfr.*;

@Label("Hello World")
@Description("Helps the programmer getting started")
class HelloWorld extends Event {
   @Label("Message")
   String message;
}

public static void main(String... args) throws IOException {
    HelloWorld event = new HelloWorld();
    event.message = "hello, world!";
    event.commit();
}
```
* Data can be extracted from recording files using classes available in jdk.jfr.consumer:

```java

import java.nio.file.*;
import jdk.jfr.consumer.*;

Path p = Paths.get("recording.jfr");
for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
   System.out.println(e.getStartTime() + " : " + e.getValue("message"));
}

```
##### Buffer mechanism and binary data format

* Threads write events, lock-free, to thread-local buffers. Once a thread-local buffer fills up, it is promoted to a global in-memory circular buffer system which maintains the most recent event data. 
* Depending on configuration, the oldest data is either discarded or written to disk allowing the history to be continuously saved. 
* Binary files on disk have the extension `.jfr` and are maintained and controlled using a `retention policy`.
* The event model is implemented in a self-describing binary format, encoded in little endian base 128 (except for the file header and some additional sections). 
* The binary data format is not to be used directly as it is subject to change. Instead, APIs will be provided for interacting with recording files.
  
* As an illustrative example, the class load event contains a time stamp describing when it occurred, a duration describing the timespan, the thread, a stack trace as well as three event specific payload fields, the loaded class and the associated class loaders. 
* The size of the event is 24 bytes in total.

```language

<memory address>: 98 80 80 00 87 02 95 ae e4 b2 92 03 a2 f7 ae 9a 94 02 02 01 8d 11 00 00
Event size [98 80 80 00]
Event ID [87 02]
Timestamp [95 ae e4 b2 92 03]
Duration [a2 f7 ae 9a 94 02]
Thread ID [02]
Stack trace ID [01]
Payload [fields]
Loaded Class: [0x8d11]
Defining ClassLoader: [0]
Initiating ClassLoader: [0]

```

#### Configure and filter events
* Events can be enabled, disabled, and filtered to reduce overhead and the amount of space needed for storage. This can be accomplished using the following settings:
  - `enabled` - should the event be recorded
  - `threshold` - the duration below which an event is not recorded
  - `stackTrace` - if the stack trace from the Event.commit() method should be recorded
  - `period` - the interval at which the event is emitted, if it is periodic

* There are two configuration sets that are tailored to configure Flight Recorder for the low-overhead, out-of-the-box use case. A user can easily create their own specific event configuration.

##### OS, JVM and JDK library events

Events will be added covering the following areas:
- `OS`  : Memory, CPU Load and CPU information, native libraries, process information
- `JVM` : Flags, GC configuration, compiler configuration, Method profiling event, Memory leak event
- `JDK` : Socket IO, File IO, Exceptions and Errors, modules

* An alternative to Flight Recorder is logging. Although JEP 158: Unified JVM Logging provides some level of uniformity across subsystems in the HotSpot JVM, it does not extend to Java applications and the JDK libraries. Traditionally, logging usually lacks an explicit model and metadata making it free form with the consequence that consumers must be tightly coupled to internal formats. Without a relational model, it is difficult to keep data compact and normalized.
  
* Flight Recorder has existed for many years and was previously a commercial feature of the Oracle JDK. This JEP moves the source code to the open repository to make the feature generally available. Hence, the risk to compatibility, performance, regressions and stability is low.


# Reference : [Java 11](http://openjdk.java.net/projects/jdk/11/)