# testControlByteCode

## History and use case

_Since 2011_

Tool created after multiple runtime problem.

Use case : 

* Identify missing libraries on runtime
* Identify duplicate classes in the classpath
* Check if all methods called in application are present in the dependencies with the right signatures
  * Example : _public void method1(String arg1, String arg2)_ become _public void method1(String... args)_

## Running the tool

* Add the maven/gradle dependencies in your project test scope  

```
<dependency>
    <groupId>com.efluid.oss</groupId>
    <artifactId>test-control-byte-code</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

* Create new Junit test who just extending _TestControleByteCode_ or _TestControleClasseEnDoublon_ (see the examples here https://github.com/efluid/testControlByteCode/tree/master/src/test/java/com/efluid/example)
* Add yaml configuration in _src/test/resources_ like this examples : https://github.com/efluid/testControlByteCode/tree/master/src/test/resources
* Run Junit tests
