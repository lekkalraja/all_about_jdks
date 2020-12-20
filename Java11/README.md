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

## JEP 329: ChaCha20 and Poly1305 Cryptographic Algorithms

* Implement the ChaCha20 and ChaCha20-Poly1305 ciphers as specified in RFC 7539. ChaCha20 is a relatively new stream cipher that can replace the older, insecure RC4 stream cipher.
* Provide ChaCha20 and ChaCha20-Poly1305 Cipher implementations. These algorithms will be implemented in the SunJCE provider.
* Provide a KeyGenerator implementation that creates keys suitable for ChaCha20 and ChaCha20-Poly1305 algorithms.
* Provide an AlgorithmParameters implementation for use with the ChaCha20-Poly1305 algorithm.
* The only other widely adopted stream cipher, RC4, has long been deemed insecure. The industry consensus is that ChaCha20-Poly1305 is secure at this point in time, and it has seen fairly wide adoption across TLS implementations as well as in other cryptographic protocols. The JDK needs to be on par with other cryptographic toolkits and TLS implementations.
* Additionally, TLS 1.3 only allows the use of AEAD-based cipher suites. Implementing the ChaCha20-Poly1305 algorithm is the first step in implementing different cipher suites that run in AEAD mode in case there were ever weaknesses to be found in AES or GCM.


```java
//A sample single-part encryption follows:

// Get a Cipher instance and set up the parameters
// Assume SecretKey "key", 12-byte nonce "nonceBytes" and plaintext "pText"
// are coming from outside this code snippet
Cipher mambo = Cipher.getInstance("ChaCha20-Poly1305");
AlgorithmParameterSpec mamboSpec = new IvParameterSpec(nonceBytes);

// Encrypt our input
mambo.init(Cipher.ENCRYPT_MODE, key, mamboSpec);
byte[] encryptedResult = new byte[mambo.getOutputSize(pText.length)];
mambo.doFinal(pText, 0, pText.length, encryptedResult);

```

## JEP 332: Transport Layer Security (TLS) 1.3

* Implements version 1.3 of the Transport Layer Security (TLS) Protocol RFC 8446.
* TLS 1.3 is a major overhaul of the TLS protocol and provides significant security and performance improvements over previous versions. Several early implementations from other vendors are available already. We need to support TLS 1.3 to remain competitive and keep pace with the latest standard.
* TLS 1.3 is a new TLS version which supersedes and obsoletes previous versions of TLS including version 1.2 (RFC 5246). It also obsoletes or changes other TLS features such as the OCSP stapling extensions (RFC 6066, RFC 6961), and the session hash and extended master secret extension (RFC 7627).
* The Java Secure Socket Extension (JSSE) in the JDK provides a framework and a Java implementation of the SSL, TLS, and DTLS protocols. Currently, the JSSE API and JDK implementation supports SSL 3.0, TLS 1.0, TLS 1.1, TLS 1.2, DTLS 1.0 and DTLS 1.2.
* The primary goal of this JEP is a minimal interoperable and compatible TLS 1.3 implementation. A minimal implementation should support:
    * Protocol version negotiation
    * TLS 1.3 full handshake
    * TLS 1.3 session resumption
    * TLS 1.3 key and iv update
    * TLS 1.3 updated OCSP stapling
    * TLS 1.3 backward compatibility mode
    * TLS 1.3 required extensions and algorithms
    * RSASSA-PSS signature algorithms (8146293)
* No new public APIs are required for the minimal implementation. The following new standard algorithm names are required:
    * TLS protocol version name: TLSv1.3
    * javax.net.ssl.SSLContext algorithm name: TLSv1.3
    * TLS cipher suite names for TLS 1.3: TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384.
    * Additionally, the KRB5 cipher suites will be removed from the JDK because they are no longer considered safe to use.

* TLS 1.3 is not directly compatible with previous versions. Although TLS 1.3 can be implemented with a backward-compatibility mode, there are several compatibility risks when using this mode:
    * TLS 1.3 uses a half-close policy, while TLS 1.2 and prior versions use a duplex-close policy. For applications that depend on the duplex-close policy, there may be compatibility issues when upgrading to TLS 1.3.
    * The `signature_algorithms_cert` extension requires that pre-defined signature algorithms are used for certificate authentication. In practice, however, an application may use non-supported signature algorithms.
    * The DSA signature algorithm is not supported in TLS 1.3. If a server is configured to only use DSA certificates, it cannot upgrade to TLS 1.3.
    * The supported cipher suites for TLS 1.3 are not the same as TLS 1.2 and prior versions. If an application hard-codes cipher suites which are no longer supported, it may not be able to use TLS 1.3 without modifying the application code.

* To minimize compatibility risk, this TLS 1.3 implementation will implement and enable the backward-compatibility mode by default.
* An application can turn off the backward-compatibility mode, and turn TLS 1.3 on or off if desired.

## JEP 335: Deprecate the Nashorn JavaScript Engine

* Deprecate the Nashorn JavaScript script engine and APIs, and the jjs tool, with the intent to remove them in a future release.
* The Nashorn JavaScript engine was first incorporated into JDK 8 via JEP 174 as a replacement for the Rhino scripting engine. When it was released, it was a complete implementation of the ECMAScript-262 5.1 standard.
* With the rapid pace at which ECMAScript language constructs, along with APIs, are adapted and modified, it was found Nashorn challenging to maintain.
* Two JDK modules will be terminally deprecated, that is, annotated with `@Deprecated(forRemoval=true)`:
    * jdk.scripting.nashorn -- contains the jdk.nashorn.api.scripting and jdk.nashorn.api.tree packages.
    * jdk.scripting.nashorn.shell -- contains the jjs tool. Running jjs will display a warning:
    * `Warning`: The jjs tool is planned to be removed from a future JDK release.
* A separate JEP will be filed for the actual removal of the types and modules in a future JDK feature release.

## JEP 336: Deprecate the Pack200 Tools and API
* Deprecate the pack200 and unpack200 tools, and the Pack200 API in java.util.jar.

* Pack200 is a compression scheme for JAR files. It was introduced in Java SE 5.0 by JSR 200. Its goal is "to decrease disk and bandwidth requirements for Java application packaging, transmission, and delivery." Developers use a pair of tools -- pack200 and unpack200 -- to compress and uncompress their JAR files. An API is available in the java.util.jar package.

* There are three reasons for wanting to deprecate (and eventually remove) Pack200:
    * Historically, slow downloads of the JDK over 56k modems were an impediment to Java adoption. The relentless growth in JDK functionality caused the download size to swell, further impeding adoption. Compressing the JDK with Pack200 was a way to mitigate the problem. However, time has moved on: download speeds have improved, and JDK 9 introduced new compression schemes for both the Java runtime (JEP 220) and the modules used to build the runtime (JMOD). Consequently, JDK 9 and later do not rely on Pack200; JDK 8 was the last release compressed with pack200 at build time and uncompressed with unpack200 at install time. In summary, a major consumer of Pack200 -- the JDK itself -- no longer needs it.
    * Beyond the JDK, it was attractive to compress client applications, and especially applets, with Pack200. Some deployment technologies, such as Oracle's browser plug-in, would uncompress applet JARs automatically. However, the landscape for client applications has changed, and most browsers have dropped support for plug-ins. Consequently, a major class of consumers of Pack200 -- applets running in browsers -- are no longer a driver for including Pack200 in the JDK.
    * Pack200 is a complex and elaborate technology. Its file format is tightly coupled to the class file format and the JAR file format, both of which have evolved in ways unforeseen by JSR 200. (For example, JEP 309 added a new kind of constant pool entry to the class file format, and JEP 238 added versioning metadata to the JAR file format.) The implementation in the JDK is split between Java and native code, which makes it hard to maintain. The API in java.util.jar.Pack200 was detrimental to the modularization of the Java SE Platform, leading to the removal of four of its methods in Java SE 9. Overall, the cost of maintaining Pack200 is significant, and outweighs the benefit of including it in Java SE and the JDK.

* Three types in the java.base module will be terminally deprecated, that is, annotated with @Deprecated(forRemoval=true):
    * java.util.jar.Pack200
    * java.util.jar.Pack200.Packer
    * java.util.jar.Pack200.Unpacker
* The jdk.pack module, which contains the pack200 and unpack200 tools, will also be terminally deprecated.
* Running pack200 or unpack200 will display a warning about the planned removal of the tool. Running jar -c with the sub-option n (to normalize the archive) will display a warning about the planned removal of the sub-option. The documentation for all three tools will indicate the deprecation and planned removal.
* A separate JEP will be filed for the actual removal of the types and module in a future JDK feature release.

## JEP 331: Low-Overhead Heap Profiling
* Provides a low-overhead way of sampling Java heap allocations, accessible via `JVMTI`.
* Provides a way to get information about Java object heap allocations from the JVM that:
    * Is low-overhead enough to be enabled by default continuously,
    * Is accessible via a well-defined, programmatic interface,
    * Can sample all allocations (i.e., is not limited to allocations that are in one particular heap region or that were allocated in one particular way),
    * Can be defined in an implementation-independent way (i.e., without relying on any particular GC algorithm or VM implementation), and
    * Can give information about both live and dead Java objects.

* There is a deep need for users to understand the contents of their heaps. Poor heap management can lead to problems such as heap exhaustion and GC thrashing. As a result, a number of tools have been developed to allow users to introspect into their heaps, such as the Java Flight Recorder, jmap, YourKit, and VisualVM tools.
* One piece of information that is lacking from most of the existing tooling is the call site for particular allocations. Heap dumps and heap histograms do not contain this information. This information can be critical to debugging memory issues, because it tells developers the exact location in their code particular (and particularly bad) allocations occurred.
* There are currently two ways of getting this information out of HotSpot:
    * First, you can instrument all of the allocations in your application using a bytecode rewriter such as the [Allocation Instrumenter](https://github.com/google/allocation-instrumenter). You can then have the instrumentation take a stack trace (when you want one).
    * Second, you can use Java Flight Recorder, which takes a stack trace on TLAB refills and when allocating directly into the old generation. The downsides of this are that
        * a) it is tied to a particular allocation implementation (TLABs),and misses allocations that don’t meet that pattern;
        * b) it doesn’t allow the user to customize the sampling interval; and
        * c) it only logs allocations, so you cannot distinguish between live and dead objects.

* This proposal mitigates these problems by providing an extensible `JVMTI interface` that allows the user to define the sampling interval and returns a set of live stack traces.

##### New JVMTI event and method
* The user facing API for the heap sampling feature proposed here consists of an extension to JVMTI that allows for heap profiling. The following systems rely on an event notification system that would provide a callback such as:
 ```java
void JNICALL
SampledObjectAlloc(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jobject object,
            jclass object_klass,
            jlong size)

    where:
        thread is the thread allocating the jobject,
        object is the reference to the sampled jobject,
        object_klass is the class for the jobject, and
        size is the size of the allocation.
 ```
* The new API also includes a single new JVMTI method:
    * `jvmtiError  SetHeapSamplingInterval(jvmtiEnv* env, jint sampling_interval)`
        * where `sampling_interval` is the average allocated bytes between a sampling.
* The specification of the method is:
    * If non zero, the sampling interval is updated and will send a callback to the user with the new average sampling interval of sampling_interval bytes
    * For example, if the user wants a sample every megabyte, sampling_interval would be 1024 * 1024.
    * If zero is passed to the method, the sampler samples every allocation once the new interval is taken into account, which might take a certain number of allocations
* Note that the sampling interval is not precise. Each time a sample occurs, the number of bytes before the next sample will be chosen will be pseudo-random with the given average interval. This is to avoid sampling bias; for example, if the same allocations happen every 512KB, a 512KB sampling interval will always sample the same allocations. Therefore, though the sampling interval will not always be the selected interval, after a large number of samples, it will tend towards it.

#### Use-case example
* To enable this, a user would use the usual event notification call to:
    * `jvmti->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL)`
* The event will be sent when the allocation is initialized and set up correctly, so slightly after the actual code performs the allocation. By default, the average sampling interval is 512KB.
* The minimum required to enable the sampling event system is to call `SetEventNotificationMode with JVMTI_ENABLE` and the `event type JVMTI_EVENT_SAMPLED_OBJECT_ALLOC`. To modify the sampling interval, the user calls the `SetHeapSamplingInterval` method.

* To disable the system,
    * `jvmti->SetEventNotificationMode(jvmti, JVMTI_DISABLE, JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL)` disables the event notifications and disables the sampler automatically.
* Calling the sampler again via SetEventNotificationMode will re-enable the sampler with whatever sampling interval was currently set (either 512KB by default or the last value passed by a user via SetHeapSamplingInterval)

#### New capability
* To protect the new feature and make it optional for VM implementations, a new capability named `can_generate_sampled_object_alloc_events` is introduced into the `jvmtiCapabilities`.

#### Global / thread level sampling
* Using the notification system provides a direct means to send events only for specific threads. This is done via `SetEventNotificationMode` and providing a third parameter with the threads to be modified.

#### A full example
* The following section provides code snippets to illustrate the sampler's API. First, the capability and the event notification is enabled:

```java
jvmtiEventCallbacks callbacks;
memset(&callbacks, 0, sizeof(callbacks));
callbacks.SampledObjectAlloc = &SampledObjectAlloc;

jvmtiCapabilities caps;
memset(&caps, 0, sizeof(caps));
caps.can_generate_sampled_object_alloc_events = 1;
if (JVMTI_ERROR_NONE != (*jvmti)->AddCapabilities(jvmti, &caps)) {
  return JNI_ERR;
}

if (JVMTI_ERROR_NONE != (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                                       JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL)) {
  return JNI_ERR;
}

if (JVMTI_ERROR_NONE !=  (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(jvmtiEventCallbacks)) {
  return JNI_ERR;
}

// Set the sampler to 1MB.
if (JVMTI_ERROR_NONE !=  (*jvmti)->SetHeapSamplingInterval(jvmti, 1024 * 1024)) {
  return JNI_ERR;
}
To disable the sampler (disables events and the sampler):

if (JVMTI_ERROR_NONE != (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_DISABLE,
                                       JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL)) {
  return JNI_ERR;
}
To re-enable the sampler with the 1024 * 1024 byte sampling interval , a simple call to enabling the event is required:

if (JVMTI_ERROR_NONE != (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                                       JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL)) {
  return JNI_ERR;
}

```
#### User storage of sampled allocations
* When an event is generated, the callback can capture a stack trace using the JVMTI GetStackTrace method. The jobject reference obtained by the callback can be also wrapped into a JNI weak reference to help determine when the object has been garbage collected. This approach allows the user to gather data on what objects were sampled, as well as which are still considered live, which can be a good means to understand the job's behavior.

* For example, something like this could be done:

```java
extern "C" JNIEXPORT void JNICALL SampledObjectAlloc(jvmtiEnv *env,
                                                     JNIEnv* jni,
                                                     jthread thread,
                                                     jobject object,
                                                     jclass klass,
                                                     jlong size) {
  jvmtiFrameInfo frames[32];
  jint frame_count;
  jvmtiError err;

  err = global_jvmti->GetStackTrace(NULL, 0, 32, frames, &frame_count);
  if (err == JVMTI_ERROR_NONE && frame_count >= 1) {
    jweak ref = jni->NewWeakGlobalRef(object);
    internal_storage.add(jni, ref, size, thread, frames, frame_count);
  }
}
```
* where `internal_storage` is a data structure that can handle the sampled objects, consider if there is a need to clean up any garbage collected sample, etc. The internals of that implementation are usage-specific, and out of scope of this JEP.
* The sampling interval can be used as a means to mitigate profiling overhead. With a sampling interval of 512KB, the overhead should be low eno

#### Implementation details
* The current prototype and implementation proves the feasibility of the approach. It contains five parts:
  * Architecture dependent changes due to a change of a field name in the ThreadLocalAllocationBuffer (TLAB) structure. These changes are minimal as they are just name changes.
  * The TLAB structure is augmented with a `new allocation_end pointer`, to complement the existing end pointer. If the sampling is disabled, the two pointers are always equal and the code performs as before. If the sampling is enabled, end is modified to be where the next sample point is requested. Then, any fast path will "think" the TLAB is full at that point and go down the slow path, which is explained in (3).
  * The gc/shared/collectedHeap code is changed due to its usage as an entry point to the allocation slow path. When a TLAB is considered full (because allocation has passed the end pointer), the code enters collectedHeap and tries to allocate a new TLAB. At this point, the TLAB is set back to its original size and an allocation is attempted. If the allocation succeeds, the code samples the allocation, and then returns. If it does not, it means allocation has reached the end of the TLAB, and a new TLAB is needed. The code path continues its normal allocation of a new TLAB and determines if that allocation requires a sample. If the allocation is considered too big for the TLAB, the system samples the allocation as well, thus covering in TLAB and out of TLAB allocations for sampling.
  * When a sample is requested, there is a collector object set on the stack in a place safe for sending the information to the native agent. The collector keeps track of sampled allocations and, at destruction of its own frame, sends a callback to the agent. This mechanism ensures the object is initialized correctly.
  * If a JVMTI agent has registered a callback for the SampledObjectAlloc event, the event will be triggered and it will obtain sampled allocations. An example implementation can be found in the `libHeapMonitorTest.c` file, which is used for JTreg testing.

## JEP 333: ZGC: A Scalable Low-Latency Garbage Collector (Experimental)

* The Z Garbage Collector, also known as ZGC, is a scalable low-latency garbage collector.
##### Goals
  * GC pause times should not exceed 10ms
  * Handle heaps ranging from relatively small (a few hundreds of megabytes) to very large (many terabytes) in size
  * No more than 15% application throughput reduction compared to using G1
  * Lay a foundation for future GC features and optimizations leveraging colored pointers and load barriers
  * Initially supported platform: Linux/x64
* Garbage collection is one of Java's main strengths. However, when garbage collection pauses become too long they start to affect application response times negatively. By removing or drastically reducing the length of GC pauses, we'll make Java a more attractive platform for an even wider set of applications.
* Furthermore, the amount of memory available in modern systems continues to grow. Users and application developers expect the JVM to be equipped to take full advantage of this memory in an efficient manner, and without long GC pause times.

* At a glance, `ZGC is a concurrent, single-generation, region-based, NUMA-aware, compacting collector`. Stop-the-world phases are limited to root scanning, so GC pause times do not increase with the size of the heap or the live set.
* A core design principle/choice in ZGC is the use of load barriers in combination with colored object pointers (i.e., colored oops). This is what enables ZGC to do concurrent operations, such as object relocation, while Java application threads are running.
* From a Java thread's perspective, the act of loading a reference field in a Java object is subject to a load barrier. In addition to an object address, a colored object pointer contains information used by the load barrier to determine if some action needs to be taken before allowing a Java thread to use the pointer. For example, the object might have been relocated, in which case the load barrier will detect the situation and take appropriate action.
* Compared to alternative techniques, we believe the colored-pointers scheme offers some very attractive properties. In particular:
    * It allows us to reclaim and reuse memory during the relocation/compaction phase, before pointers pointing into the reclaimed/reused regions have been fixed. This helps keep the general heap overhead down. It also means that there is no need to implement a separate mark-compact algorithm to handle a full GC.
    * It allows us to have relatively few and simple GC barriers. This helps keep the runtime overhead down. It also means that it's easier to implement, optimize and maintain the GC barrier code in our interpreter and JIT compilers.
    * We currently store marking and relocation related information in the colored pointers. However, the versatile nature of this scheme allows us to store any type of information (as long as we can fit it into the pointer) and let the load barrier take any action it wants to based on that information. We believe this will lay the foundation for many future features. 
    * To pick one example, in a heterogeneous memory environment, this could be used to track heap access patterns to guide GC relocation decisions to move rarely used objects to cold storage.

###### Performance
* Regular performance measurements have been done using SPECjbb® 2015. Performance is looking good, both from a throughput and latency point of view. Below are typical benchmark scores (in percent, normalized against ZGC's max-jOPS), comparing ZGC and G1, in composite mode using a 128G heap.

```java

(Higher is better)

ZGC
       max-jOPS: 100%
  critical-jOPS: 76.1%

G1
       max-jOPS: 91.2%
  critical-jOPS: 54.7%

```

* Below are typical GC pause times from the same benchmark. ZGC manages to stay well below the 10ms goal. Note that exact numbers can vary (both up and down, but not significantly) depending on the exact machine and setup used.

```java

(Lower is better)

ZGC
                avg: 1.091ms (+/-0.215ms)
    95th percentile: 1.380ms
    99th percentile: 1.512ms
  99.9th percentile: 1.663ms
 99.99th percentile: 1.681ms
                max: 1.681ms

G1
                avg: 156.806ms (+/-71.126ms)
    95th percentile: 316.672ms
    99th percentile: 428.095ms
  99.9th percentile: 543.846ms
 99.99th percentile: 543.846ms
                max: 543.846ms
```

* Ad-hoc performance measurements have also been done on various other SPEC® benchmarks and internal workloads. In general, ZGC manages to maintain single-digit millisecond pause times.
* [1] SPECjbb® 2015 is a registered trademark of the Standard Performance Evaluation Corporation (spec.org). The actual results are not represented as compliant because the SUT may not meet SPEC's requirements for general availability.

##### Limitations
* The initial experimental version of ZGC will not have support for class unloading. The ClassUnloading and ClassUnloadingWithConcurrentMark options will be disabled by default. Enabling them will have no effect.
* Also, ZGC will initially not have support for JVMCI (i.e. Graal). An error message will be printed if the EnableJVMCI option is enabled.
* These limitations will be addressed at a later stage in this project.

##### Building and Invoking
* By convention, experimental features in the JVM are disabled by default by the build system. ZGC, being an experimental feature, will therefore not be present in a JDK build unless explicitly enabled at compile-time using the configure option `--with-jvm-features=zgc`.(ZGC will be present in all Linux/x64 JDK builds produced by Oracle)
* Experimental features in the JVM also need to be explicitly unlocked at run-time. To enable/use ZGC, the following JVM options will therefore be needed: `-XX:+UnlockExperimentalVMOptions` `-XX:+UseZGC`.
* Please see the [ZGC Project Wiki]([https://link](https://wiki.openjdk.java.net/display/zgc/Main)) for more information on how to setup and tune ZGC.
##### Alternatives
* An obvious alternative is to add concurrent compaction capabilities to G1. This alternative was extensively prototyped but eventually abandoned. We found it unfeasible to shoehorn this functionality into a code base that was never designed for this purpose and, at the same time, preserve G1's stability and other good properties.
* A theoretical alternative would be to improve CMS one way or another. There are however several reasons why basing a low latency collector on the CMS algorithm is neither an attractive nor viable option. Reasons include no support for compaction, the unbound remark phase, a complicated code base, and the fact that it has already been deprecated (JEP 291).
* The Shenandoah Project is exploring the use of Brooks pointers to achieve concurrent operations (JEP 189).

## JEP 330: Launch Single-File Source-Code Programs
* Enhance the java launcher to run a program supplied as a single file of Java source code, including usage from within a script by means of "shebang" files and related techniques.

##### Motivation
* Single-file programs -- where the whole program fits in a single source file -- are common in the early stages of learning Java, and when writing small utility programs. In this context, it is pure ceremony to have to compile the program before running it. In addition, a single source file may compile to multiple class files, which adds packaging overhead to the simple goal of "run this program". It is desirable to be able to run the program directly from source with the java launcher:
    `java HelloWorld.java`

##### Description
* As of JDK 10, the java launcher operates in three modes:
    * launching a class file,
    * launching the main class of a JAR file, or
    * launching the main class of a module.
    * Here adding a new, `fourth mode: launching a class declared in a source file`.

* Source-file mode is determined by considering two items on the command line:
    * The first item on the command line that is neither an option nor part of an option. (In other words, the item that previously has been the class name.)
    * The --source version option, if present.
* If the "class name" identifies an existing file with the .java extension, source-file mode is selected, with that file to be compiled and run. The --source option may be used to specify the source version of the source code.
* If the file does not have the .java extension, the --source option must be used to force source-file mode. This is for cases such as when the source file is a "script" to be executed and the name of the source file does not follow the normal naming conventions for Java source files. (See "shebang" files below.)
* The --source option must also be used to specify the source version of the source code when the --enable-preview option is used. (See JEP 12.)
* In source-file mode, the effect is as if the source file is compiled into memory, and the first class found in the source file is executed. For example, if a file called HelloWorld.java contains a class called hello.World, then the command

```java
    java HelloWorld.java

    is informally equivalent to

    javac -d <memory> HelloWorld.java
    java -cp <memory> hello.World
```
* Any arguments placed after the name of the source file in the original command line are passed to the compiled class when it is executed. For example, if a file called Factorial.java contains a class called Factorial to calculate the factorials of its arguments, then the command

```java
    java Factorial.java 3 4 5

    is informally equivalent to

    javac -d <memory> Factorial.java
    java -cp <memory> Factorial 3 4 5
```
* In source-file mode, any `additional command-line options are processed` as follows:
  * The launcher scans the options specified before the source file for any that are relevant in order to compile the source file. This includes: --class-path, --module-path, --add-exports, --add-modules, --limit-modules, --patch-module, --upgrade-module-path, and any variant forms of those options. It also includes the new --enable-preview option, described in JEP 12.
  * No provision is made to pass any additional options to the compiler, such as -processor or -Werror.
  * Command-line argument files (@-files) may be used in the standard way. Long lists of arguments for either the VM or the program being invoked may be placed in files which are specified on the command-line by prefixing the filename with an @ character.

* In source-file mode, `compilation proceeds` as follows:
    * Any command-line options that are relevant to the compilation environment are taken into account.
    * No other source files are found and compiled, as if the source path is set to an empty value.
    * Annotation processing is disabled, as if `-proc:none` is in effect.
    * If a version is specified, via the --source option, the value is used as the argument for an implicit --release option for the compilation. This sets both the source version accepted by compiler and the system API that may be used by the code in the source file.
    * The source file is compiled in the context of an unnamed module.
    * The source file should contain one or more top-level classes, the first of which is taken as the class to be executed.
    * The compiler does not enforce the optional restriction defined at the end of JLS §7.6, that a type in a named package should exist in a file whose name is composed from the type name followed by the .java extension.
    * If the source file contains errors, appropriate error messages are written to the standard error stream, and the launcher exits with a non-zero exit code.
* In source-file mode, `execution proceeds` as follows:
    * The class to be executed is the first top-level class found in the source file. It must contain a declaration of the standard public static void main(String[]) method.
    * The compiled classes are loaded by a custom class loader, that delegates to the application class loader. (This implies that classes appearing on the application class path cannot refer to any classes declared in the source file.)
    * The compiled classes are executed in the context of an unnamed module, and as if --add-modules=ALL-DEFAULT is in effect (in addition to any other --add-module options that may be have been specified on the command line.)
    * Any arguments appearing after the name of the file on the command line are passed to the standard main method in the obvious way.
    * It is an error if there is a class on the application class path whose name is the same as that of the class to be executed.

* Note that there is a potential minor ambiguity when using a simple command-line like java HelloWorld.java. Previously, HelloWorld.java would have been interpreted as a class called java in a package called HelloWorld, but which is now resolved in favor of a file called HelloWorld.java if such a file exists. Given that such a class name and such a package name both violate the nearly-universally-followed naming conventions, and given the unlikeliness of such a class being on the class path and a like-named file being in the current directory, this seems an acceptable compromise.

##### Implementation

* Source-file mode requires the presence of the jdk.compiler module. When source-file mode for a file Foo.java is requested, the launcher behaves as if the command line were translated to:

    `java [VM args] -m jdk.compiler/<source-launcher-implementation-class> Foo.java [program args]`

* The source-launcher implementation class programmatically invokes the compiler, which compiles the source to an in-memory representation. The source-launcher implementation class then creates a class loader to load compiled classes from that in-memory representation, and invokes the standard main(String[]) method of the first top-level class found in the source file.
* The source-launcher implementation class has access to any relevant command-line options, such as those to define the class path, module path, and the module graph, and passes those options to the compiler to configure the compilation environment.
* If the class that is invoked throws an exception, that exception is passed back to the launcher for handling in the normal way. However, the initial stackframes leading up to the execution of the class are removed from the stacktrace of the exception. The intent is that the handling of the exception is similar to the handling if the class had been executed directly by the launcher itself. The initial stackframes will be visible in any direct access to the stack, including (for example) Thread.dumpStack().
* The class loader that is used to load the compiled classes itself uses an implementation-specific protocol for any URLs that refer to resources defined by the class loader. The only way to get such URLs is by using methods like getResource or getResources; creating any such URL from a string is not supported.

##### "Shebang" files

* Single-file programs are also common when the task at hand needs a small utility program. In this context, it is desirable to be able to run a program directly from source using the `"#!"` mechanism on Unix-derived systems, such as macOS and Linux. This is a mechanism provided by the operating system which allows a single-file program (such as a script or source code) to be placed in any conveniently named executable file whose first line begins with #! and which specifies the name of a program to "execute" the contents of the file. Such files are called "shebang files".
* It is desirable to be able to execute Java programs with this mechanism.
* A shebang file to invoke the Java launcher using source-file mode must begin with something like:
    `#!/path/to/java --source version`
* For example, we could take the source code for a "Hello World" program, and put it in a file called hello, after an initial line of `#!/path/to/java --source 10`, and then mark the file as executable. Then, if the file is in the current directory, we could execute it with:
    `$ ./hello`
* Or, if the file is in a directory in the user's PATH, we could execute it with:
    `$ hello`
* Any arguments for the command are passed to the main method of the class that is executed. For example, if we put the source code for a program to compute factorials into a shebang file called factorial, we could execute it with a command like:
    `$ factorial 6`
* The --source option must be used in shebang files in the following situations:
* The name of the shebang file does not follow the standard naming conventions for Java source files.It is desired to specify additional VM options on the first line of the shebang file. In this case, the --source option should be specified first, after the name of the executable.
* It is desired to specify the version of the Java language used for the source code in the file.
* A shebang file can also be invoked explicitly by the launcher, perhaps with additional options, with a command like:
    `$ java -Dtrace=true --source 10 factorial 3`
* The Java launcher's source-file mode makes two accommodations for shebang files:
    * When the launcher reads the source file, if the file is not a Java source file (i.e. it is not a file whose name ends with .java) and if the first line begins with #!, then the contents of that line up to but not including the first newline are ignored when determining the source code to be passed to the compiler. The content of the file that appears after the first line must consist of a valid `CompilationUnit` as defined by §7.3 in the edition of the Java Language Specification that is appropriate to the version of the platform given in the --source option, if present, or the version of the platform being used to run the program if the --source option is not present.
    * The newline at the end of the first line is preserved so that the line numbers in any compiler error messages are meaningful in the shebang file.

* Some operating systems pass the text on the first line after the name of the executable as a single argument to the executable. With that in mind, if the launcher encounters an option beginning --source and containing whitespace, it is split into a series of words, separated by whitespace, before being further analyzed by the launcher. This allows additional arguments to be put on the first line, although some operating system may impose a limit on the overall length of the line. Using quotes to preserve whitespace in such values is not supported.
* No changes to the JLS are required in support of this feature.
* In a shebang file, the first two bytes must be `0x23 0x21, the two-character ASCII encoding of #!`. All subsequent bytes are read with the default platform character encoding that is in effect.
* A first line beginning #! is only required when it is desired to execute the file with the operating system's shebang mechanism. There is no need for any special first line when the Java launcher is used explicitly to run the code in a source file, as in the HelloWorld.java and Factorial.java examples, given above. Indeed, the use of the shebang mechanism to execute files that follow the standard naming convention for Java source files is not permitted.


## Reference : [Java 11](http://openjdk.java.net/projects/jdk/11/)