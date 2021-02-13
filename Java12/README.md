# JAVA 12

## JEP 189: Shenandoah: A Low-Pause-Time Garbage Collector (Experimental)
* A new garbage collection (GC) algorithm named Shenandoah which reduces GC pause times by doing evacuation work concurrently with the running Java threads. Pause times with Shenandoah are independent of heap size, meaning you will have the same consistent pause times whether your heap is 200 MB or 200 GB.

* Modern machines have more memory and more processors than ever before. Service Level Agreement (SLA) applications guarantee response times of 10-500ms. In order to meet the l`ower end of that goal` we need garbage collection algorithms which are efficient enough to allow programs to run in the available memory, but also optimized to never interrupt the running program for more than a handful of milliseconds. 
* `Shenandoah` is an open-source low-pause time collector for OpenJDK designed to move us closer to those goals.
* `Shenandoah trades concurrent cpu cycles and space for pause time improvements`. We've added an indirection pointer to every Java object which enables the GC threads to compact the heap while the Java threads are running. Marking and compacting are performed concurrently so we only need to pause the Java threads long enough to scan the thread stacks to find and update the roots of the object graph.

* Shenandoah has been implemented and will be supported by Red Hat for aarch64 and for amd64.

* The on-going development for Shenandoah is done in the OpenJDK [Shenandoah project](http://openjdk.java.net/projects/shenandoah/).

#### Alternatives
- Zing/Azul has a pauseless collector, however that work has not been contributed to OpenJDK.
- ZGC has a low pause collector based on colored pointers. We look forward to comparing the performance of the two strategies.
- G1 does some parallel and concurrent work, but it does not do concurrent evacuation.
- CMS does concurrent marking, but it performs young generation copying at pause times, and never compacts the old generation. This results in more time spent managing free space in the old generation as well as fragmentation issues.

#### Building and Invoking
* As experimental feature, Shenandoah will require `-XX:+UnlockExperimentalVMOptions` in the command line.
* The Shenandoah build system disables the build on unsupported configurations automatically. Downstream builders may choose to disable building Shenandoah with `--with-jvm-features=-shenandoahgc` on otherwise supported platforms.
* To enable/use Shenandoah GC, the following JVM options will be needed: `-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC.`

## Reference [JAVA-12](http://openjdk.java.net/projects/jdk/12/)