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


# Reference : [Java 11](http://openjdk.java.net/projects/jdk/11/)
