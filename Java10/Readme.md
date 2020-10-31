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

## Application Class-Data Sharing

* To improve startup and footprint, extend the existing Class-Data Sharing ("CDS") feature to allow application classes to be placed in the shared archive.

### Goals
* Reduce footprint by sharing common class metadata across different Java processes.
* Improve startup time.
* Extend CDS to allow archived classes from the JDK's run-time image file ($JAVA_HOME/lib/modules) and the application class path to be loaded into the built-in platform and system class loaders.
* Extend CDS to allow archived classes to be loaded into custom class loaders.

#### Description
* Class-Data Sharing, introduced in JDK 5
* Currently CDS only allows the bootstrap class loader to load archived classes. Application CDS ("AppCDS") extends CDS to allow the built-in system class loader (a.k.a., the "app class loader"), the built-in platform class loader, and custom class loaders to load archived classes.
* Analysis of the memory usage of large-scale enterprise applications shows that such applications often load tens of thousands of classes into the application class loader. Applying AppCDS to these applications will result in memory savings of tens to hundreds of megabytes per JVM process.
* Analysis of serverless cloud services shows that many of them load several thousand application classes at start-up. AppCDS can allow these services to start up quickly and improve the overall system response time.

##### Enabling AppCDS
* By default, Class-Data Sharing is enabled only for the JVM's bootstrap class loader. Specify the `-XX:+UseAppCDS` command-line option to enable class data sharing for the system class loader (a.k.a. "app class loader"), the platform class loader, and other user-defined class loaders.
  
* An application may be packaged with a large number of classes but use only a fraction of them during normal operation. By archiving only the classes that are used, we can reduce the file storage size and runtime memory usage. To do this, first run the application normally with `-Xshare:off`, and use the `-XX:DumpLoadedClassList` command-line option to record all the classes that are loaded.
  
* Note that `-XX:DumpLoadedClassList` by default includes only the classes loaded by the bootstrap class loader. You should specify the -XX:+UseAppCDS (No need from Java11) option so that classes loaded by the system class loader and platform class loader are also included.

* To create the AppCDS archive, specify the `-Xshare:dump` `-XX:+UseAppCDS` command line options, pass the list of classes with the `-XX:SharedClassListFile` option, and set the classpath to be the same as used by your application.
* Use the `-XX:SharedArchiveFile` option to specify the name of the archive file to store the classes. Note the if `-XX:SharedArchiveFile` is not specified then the archived classes will be stored into the JDK's installation directory, which is typically not what you want to do.
* Once the AppCDS archive is created, you can use it when starting the application. Do this by specifying the `-Xshare:on` `-XX:+UseAppCDS` command line options, with the `-XX:SharedArchiveFile` option to indicate the name of the archive file.
  

```java
cd Java10

> javac Hello.java

> jar cmvf META-INF/MANIFEST.MF hello.jar Hello.class

> java -cp hello.jar Hello

> java -Xshare:off -XX:+UseAppCDS -XX:DumpLoadedClassList=hello.lst -cp hello.jar Hello

> java -Xshare:dump -XX:+UseAppCDS -XX:SharedClassListFile=hello.lst -XX:SharedArchiveFile=hello.jsa -cp hello.jar Hello

Java HotSpot(TM) 64-Bit Server VM warning: Ignoring obsolete option UseAppCDS; AppCDS is automatically enabled
narrow_klass_base = 0x0000000800000000, narrow_klass_shift = 3
Allocated temporary class space: 1073741824 bytes at 0x00000008c0000000
Allocated shared space: 3221225472 bytes at 0x0000000800000000
Loading classes to share ...
Loading classes to share: done.
Rewriting and linking classes ...
Rewriting and linking classes: done
Number of classes 648
    instance classes   =   570
    obj array classes  =    70
    type array classes =     8
Updating ConstMethods ... done. 
Removing unshareable information ... done. 
Scanning all metaspace objects ... 
Allocating RW objects ... 
Allocating RO objects ... 
Relocating embedded pointers ... 
Relocating external roots ... 
Dumping symbol table ...
Dumping objects to closed archive heap region ...
Dumping objects to open archive heap region ...
Relocating SystemDictionary::_well_known_klasses[] ... 
Removing java_mirror ... done. 
mc  space:      5488 [  0.1% of total] out of      8192 bytes [ 67.0% used] at 0x0000000800000000
rw  space:   2004912 [ 22.1% of total] out of   2007040 bytes [ 99.9% used] at 0x0000000800002000
ro  space:   3638280 [ 40.2% of total] out of   3641344 bytes [ 99.9% used] at 0x00000008001ec000
md  space:      2560 [  0.0% of total] out of      4096 bytes [ 62.5% used] at 0x0000000800565000
od  space:   3083824 [ 34.0% of total] out of   3084288 bytes [100.0% used] at 0x0000000800566000
st0 space:    208896 [  2.3% of total] out of    208896 bytes [100.0% used] at 0x00000000fff00000
oa0 space:    106496 [  1.2% of total] out of    106496 bytes [100.0% used] at 0x00000000ffe00000
total    :   9050456 [100.0% of total] out of   9060352 bytes [ 99.9% used]

> java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=hello.jsa -cp hello.jar Hello

```

* To analyse class-path related stuff add `-Xlog:class+path=info`

```java
java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=hello.jsa -Xlog:class+path=info -cp hello.jar Hello
Java HotSpot(TM) 64-Bit Server VM warning: Ignoring obsolete option UseAppCDS; AppCDS is automatically enabled
[0.003s][info][class,path] bootstrap loader class path=/usr/lib/jvm/jdk-11/lib/modules
[0.003s][info][class,path] opened: /usr/lib/jvm/jdk-11/lib/modules
[0.006s][info][class,path] type=BOOT 
[0.006s][info][class,path] Expecting BOOT path=/usr/lib/jvm/jdk-11/lib/modules
[0.006s][info][class,path] ok
[0.006s][info][class,path] type=APP 
[0.006s][info][class,path] Expecting -Djava.class.path=hello.jar
[0.006s][info][class,path] 
[0.006s][info][class,path] ok
[0.022s][info][class,path] checking shared classpath entry: /usr/lib/jvm/jdk-11/lib/modules
[0.022s][info][class,path] ok
[0.022s][info][class,path] checking shared classpath entry: hello.jar
[0.023s][info][class,path] ok
Hello, World!
```

* AppCDS works by memory-mapping the contents of the archive at a fixed address. On some operating systems, especifally when address space layout randomization (ASLR) is enabled, the memory-mapping operation may occassionally fail when the required address space is not available. To overcome this issue use `-Xshare:auto`

* To List the classes loaded from the AppCDS Archive use `-Xlog:class+load=info`

```java

> java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=hello.jsa -Xlog:class+load=info -cp hello.jar Hello

[0.106s][info][class,load] sun.security.util.Debug source: shared objects file
[0.107s][info][class,load] Hello source: shared objects file
[0.107s][info][class,load] java.lang.PublicMethods$MethodList source: shared objects file
[0.107s][info][class,load] java.lang.PublicMethods$Key source: shared objects file
[0.107s][info][class,load] java.lang.Void source: shared objects file
Hello, World!
[0.107s][info][class,load] java.util.IdentityHashMap$IdentityHashMapIterator source: shared objects file
[0.107s][info][class,load] java.util.IdentityHashMap$KeyIterator source: shared objects file
[0.107s][info][

```

* Platform and system class loaders: The HotSpot VM recognizes class-loading requests from the built-in platform and system class loaders. When these loaders request a class that exists in the CDS archive then the VM skips the usual class-file parsing and verification steps and loads the archived copied of the class.
* Custom class loaders: When a custom class loader invokes ClassLoader::defineClass, the VM attempts to match the contents of the class file with an archived class by comparing fingerprints of the class-file data. If a match is found, the VM skips the class-file parsing and verification steps and directly loads the archived copy of the class.

### Reference : http://openjdk.java.net/projects/jdk/10/ <br> http://openjdk.java.net/projects/amber/LVTIstyle.html <br> http://openjdk.java.net/projects/amber/LVTIFAQ.html