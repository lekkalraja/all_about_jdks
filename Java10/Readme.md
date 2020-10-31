# JAVA 10

## JEP 286: Local-Variable Type Inference

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

## JEP 296: Consolidate the JDK Forest into a Single Repository
* Combine the numerous repositories of the JDK forest into a single repository in order to simplify and streamline development.

## JEP 304: Garbage Collector Interface
* Improve the source code isolation of different garbage collectors by introducing a clean garbage collector (GC) interface.

### Goals
* Better modularity for HotSpot internal GC code
* Make it simpler to add a new GC to HotSpot without perturbing the current code base
* Make it easier to exclude a GC from a JDK build

## JEP 307: Parallel Full GC for G1
* Improve G1 worst-case latencies by making the full(old gen) GC parallel.
* The G1 garbage collector is designed to avoid full collections, but when the concurrent collections can't reclaim memory fast enough a fall back full GC will occur. The current implementation of the full GC for G1 uses a single threaded mark-sweep-compact algorithm. We intend to parallelize the mark-sweep-compact algorithm and use the same number of threads as the Young and Mixed collections do. The number of threads can be controlled by the `-XX:ParallelGCThreads` option, but this will also affect the number of threads used for Young and Mixed collections.

### Motivation
* The G1 garbage collector was made the default in JDK 9. The previous default, the parallel collector, has a parallel full GC. To minimize the impact for users experiencing full GCs, the G1 full GC should be made parallel as well.

## JEP 310: Application Class-Data Sharing

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
> java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=hello.jsa -Xlog:class+path=info -cp hello.jar 
Hello

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

## JEP 312: Thread-Local Handshakes
* A handshake operation is a callback that is executed for each JavaThread while that thread is in a safepoint safe state. The callback is executed either by the thread itself or by the VM thread while keeping the thread in a blocked state. The big difference between safepointing and handshaking is that the per thread operation will be performed on all threads as soon as possible and they will continue to execute as soon as it’s own operation is completed. If a JavaThread is known to be running, then a handshake can be performed with that single JavaThread as well.

## JEP 313: Remove the Native-Header Generation Tool (javah)
* The tool has been superseded by superior functionality in javac, added in JDK 8 (JDK-7150368). This functionality provides the ability to write native header files at the time that Java source code is compiled, thereby eliminating the need for a separate tool.

* Focusing on the support provided by javac eliminates the need to upgrade javah to support recent new paradigms, such as API access via the Compiler API in javax.tools.*, or the new java.util.spi.ToolProvider SPI added in JDK 9.

## JEP 314: Additional Unicode Language-Tag Extensions

* Enhance java.util.Locale and related APIs to implement additional Unicode extensions of BCP 47 language tags.

## JEP 316: Heap Allocation on Alternative Memory Devices

* Enable the HotSpot VM to allocate the Java object heap on an alternative memory device, such as an NV-DIMM, specified by the user.

#### Motivation
* With the availability of cheap NV-DIMM memory, future systems may be equipped with heterogeneous memory architectures. One example of such technology is Intel's 3D XPoint. Such an architecture, in addition to DRAM, will have one or more types of non-DRAM memory with different characteristics.
* This JEP targets alternative memory devices that have the same semantics as DRAM, including the semantics of atomic operations, and can therefore be used instead of DRAM for the object heap without any change to existing application code. All other memory structures such as the code heap, metaspace, thread stacks, etc., will continue to reside in DRAM.

* Some use cases for this proposal are:

  1. In multi-JVM deployments some JVMs such as daemons, services, etc., have lower priority than others. NV-DIMMs would potentially have higher access latency compared to DRAM. Low-priority processes can use NV-DIMM memory for the heap, allowing high-priority processes to use more DRAM.

  2. Applications such as big data and in-memory databases have an ever-increasing demand for memory. Such applications could use NV-DIMM for the heap since NV-DIMMs would potentially have a larger capacity, at lower cost, compared to DRAM.

#### Description

* Some operating systems already expose non-DRAM memory through the file system. Examples are NTFS DAX mode and ext4 DAX. Memory-mapped files in these file systems bypass the page cache and provide a direct mapping of virtual memory to the physical memory on the device.

* To allocate the heap in such memory we can add a new option, `-XX:AllocateHeapAt=<path>`. This option would take a path to the file system and use memory mapping to achieve the desired result of allocating the object heap on the memory device. The JEP does not intend to share a non-volatile region between multiple running JVMs or re-use the same region for further invocations of the JVM.

* The existing heap related flags such as -Xmx, -Xms, etc., and garbage-collection related flags would continue to work as before.

* To ensure application security the implementation must ensure that file(s) created in the file system are:
  1. Protected by correct permissions, to prevent other users from accessing it.
  2. Removed when the application terminates, in any possible scenario.

## JEP 317: Experimental Java-Based JIT Compiler
* Enable the Java-based JIT compiler, Graal, to be used as an experimental JIT compiler on the Linux/x64 platform.
* Graal, a Java-based JIT compiler, is the basis of the experimental Ahead-of-Time (AOT) compiler introduced in JDK 9. Enabling it to be used as an experimental JIT compiler is one of the initiatives of Project Metropolis, and is the next step in investigating the feasibility of a Java-based JIT for the JDK.
* Enable Graal to be used as an experimental JIT compiler, starting with the Linux/x64 platform. Graal will use the JVM compiler interface (JVMCI) introduced in JDK 9. Graal is already in the JDK, so enabling it as an experimental JIT will primarily be a testing and debugging effort. To enable Graal as the JIT compiler, use the following options on the java command line:
  `java -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler`

##  JEP 319: Root Certificates
* Provide a default set of root Certification Authority (CA) certificates in the JDK.

## JEP 322: Time-Based Release Versioning
* Revise the version-string scheme of the Java SE Platform and the JDK, and related versioning information, for present and future time-based release models.
  `$FEATURE.$INTERIM.$UPDATE.$PATCH`

### Reference : http://openjdk.java.net/projects/jdk/10/ <br> http://openjdk.java.net/projects/amber/LVTIstyle.html <br> http://openjdk.java.net/projects/amber/LVTIFAQ.html