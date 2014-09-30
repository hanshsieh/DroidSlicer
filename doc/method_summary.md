# Method Summary #
Our system do not analyze all the library code. Instead, we use method summary to summarize the behaviors of the library methods that are important for our analysis. 

The method summary is defined in *config/apiXXX/method_summary.xml*, which is a XML file. The implementation the parser for this file is in *org.droidslicer.config.XMLBypassSummaryReader*. The format is described below.

## Format ##
The root element is *&lt;summary-spec&gt;*, and it can have several *&lt;class-loader&gt;* child elements for the method summary of the classes in each type of class loaderS. For the meaning of class loader here, see [class loader](class_loader.html). A *&lt;class-loader&gt;* element can have a *name* attribute defining the name of the class loader, e.g. Primordial.

Under a *&lt;class-loader&gt;* element, the hierarchy is like below.

    <package>
        <class>
            <field />
            <method>
            </method>
        </class>
    </package>


### &lt;package&gt; ###
Under a *&lt;class-loader&gt;* element, there can be several *&lt;package&gt;* elements, and each element can have the following attributes:

- name  
It defines the name of the package. (Required)
- ignore  
It defines whether the library code of the methods under thie package should be ignored. If the library code of a method is ignored, and its behavior isn't summaried in this file, then our tool will automatically generate a default method summary for it. For what the default summary will do, see the paper, or the source code *org.droidslicer.android.model.AndroidModelMethodTargetSelector*. (Optional, default: true)


By default, every library method will be ignored, and it is decided by the static field *org.droidslicer.analysis.AndroidAnalysisContext#DEFAULT_METHOD_IGNORE*.

### &lt;class&gt; ###
Under a *&lt;package&gt;* element, there can be several *&lt;class&gt;* elements, and each element can have the following attributes:

- name  
It defines the name of the class. (Required)
- ignore  
It is similar to the *ignore* attribute of a *&lt;package&gt;* element, but it defines whether the library code of the methods under this class should be ignored. (Optional, default: true)
- allocatable  
It defines that whether our tool should make this class as if it is allocatable. In Java, interface and abstract class are non-alloctable. However, when defining method summary, sometimes it would be helpful to make a non-alloctable class alloctable. For example, *Executors.newFixedThreadPool(int)* returns an instance extending *ExecutorService*, which is an interface. When writing its method summary, it would be convenient to define it as if it will directly allocate and return an instance of *ExecutorService*. However, because *ExecutorService* isn't really alloctable, if *ExecutorService.execute(Runnable)* is invoked over the returned instance in the app code, there won't be any callee, and it will make the invocation ignored by our system. In this situation, a simple solution is to make the *ExecutorService* allocatable. You may imagine that we will replace the original definition of *ExecutorService* with a class (non-abstract, non-interface) having empty method definition. (Optional, default: false)

### &lt;field&gt; ###
Under a *&lt;class&gt;* element, there can be several *&lt;field&gt;* elements. A *&lt;field&gt;* element defines an extra field that will be added to the class. It can have the following attributes.
- name  
The name of the field. (Required)
- type  
The type of the field. E.g. "int", "java.lang.String". (Required)
- final  
Whether the field is *final*. The attribtue value can be either "true" or "false". (Optional, default: false)
- public  
Whether the field is *public*. The attribtue value can be either "true" or "false". (Optional, default: false)
- private  
Whether the field is *private*. The attribtue value can be either "true" or "false". (Optional, default: false)
- protected  
Whether the field is *protected*. The attribtue value can be either "true" or "false". (Optional, default: false)
- volatile  
Whether the field is *volatile*. The attribtue value can be either "true" or "false". (Optional, default: false)
- static  
Whether the field is *static*. The attribtue value can be either "true" or "false". (Optional, default: false)

### &lt;method&gt; ###
Under a *&lt;class&gt;* element, there can also be several *&lt;method&gt;* elements. A *&lt;method&gt;* element defines the method summary of a method in the class. It can have the following attributes.

- signature  
It defines the method signature. See [method signature](method_sig.html) for its format. (Required)
- static  
Whether the method is static. The attribute value can be either "true" or "false". (Optional, default: false)
- factory  
Whether our system should treat this method as a factory method. If a method is treated as a factory method, during the construction of call graph, we will assume that the type of the returned instance can any class which is a descendant of the return type. For example, you may want to define *Object newArray(java.lang.Class, int)* in class *java.lang.reflect.Array* as factory. The attribute value can be either "true" or "false". (Optional, default: false)

Under a *&lt;method&gt;* element, the method summary of the method is defined. Each child element of a *&lt;method&gt;* defines an instruction in the method. The instructions are very similar to the Java bytecode except that they use local variables instead of stack. The instructions are in [SSA](http://en.wikipedia.org/wiki/Static_single_assignment_form) form; that is, each varaible is assigned exactly one, and each must be defined before use. For a method with N parameters, if the method is static, then arg0, arg1, ..., argN-1 are pre-defined varaibles to represent the arguments; otherwise, arg0 represent the implicit "this" argument, and arg1, arg2, ... argN represent the arguments of the method.

The allowed instructions are defined below.

### Instruction &lt;constant&gt; ###
This instruction define a new varaible with constant value. It can have the following attributes.

- name  
The name of the variable.
- type  
The type of the variable. It can be the following values:
    - int
    - short
    - long
    - float
    - double
    - char
    - string
    - boolean
- value  
    The value of the constant. For *int*, *short*, and *long* types, it can be an integer in decimal format, or hexadecimal format with '0x' prefix. For *float* and *double* types, it can be a string acceptable by *java.lang.Float(String)* in Java. For *char* type, it must be a one-character string. For *string* type, it directly specify the value of the string. For *boolean* type, the acceptable values include "true" and "false".

### Instruction &lt;call&gt; ###
It defines a method invocation instruction. It can have the following attributes.

- type  
The type of the invocation. It can be either "virtual", "special", "interface", or "static", and each corresponds to the Java bytecode instructions *invokevirtual*, *inovkespecial*, *invokeinterface*, and *invokestatic*, respectively. (Required)
- signature  
It defines the signature of the invoked method. See [method signature](method_sig.html) for more detail. (Required)
- class  
The fully qualified name of the class of the invoked method. E.g. "java.lang.Object". (Required)
- exception  
Define a new variable to store the exception object if exception is thrown by the invocation. (Required)
- arg{i}  
The {i}th argument of the invocation. Except for static invocation, arg0 will denote the implicit "this" argument; otherwise, it will denote the first argument of the method. All the arguments of the invoked method must be specified. 
- def  
Define a new variable to hold the return value. For method with return value, it is optional; otherwise, it mustn't be specified. (Optional)

### Instruction &lt;new&gt; ###
It defines an instruction for allocating a new instance. It can have the following attributes.

- class  
The fully qualified class name of the allocated class. It may be an array type, e.g. "java.lang.String[]". (Required)
- def  
Define a new varaible to hold the allocated instance. (Required)
- size  
If the allocated instance is an array, then this attribute must be specified to define the size of the array. The attribute value should be the name of a variable. (Optional)

### Instruction &lt;return&gt; ###
It defines a *return* instruction. It can have the following attributes.

- value  
The name of the variable to return. This attribute is required iff the method returns non-void value. (Optional)

### Instruction &lt;putstatic&gt; ###
It defines an instruction for putting a value into a static class field. It can have the following attributes.

- class  
It defines the fully qualified class name. (Required)
- field  
The name of the field. (Required)
- fieldType  
The type of the field. E.g. "java.lang.String", "int". (Required)
- value  
The name of the variable to store to the field. (Required)

### Instruction &lt;getstatic&gt; ###
It defines an instruction for getting the value of a static class field. It can have the following attributes.

- class  
It defines the fully qualified class name. (Required)
- field  
The name of the field. (Required)
- fieldType  
The type of the field. (Required)
- def  
Define a new varaible to hold the obtained value. (Required)

### Instruction &lt;putfield&gt; ###
It defines an instruction for putting a value to an instance field. It can have the following attributes.

- ref  
The name of the variable holding the reference to the instance. (Required)
- class  
It defines the fully qualified class name. (Required)
- field  
The name of the field. (Required)
- fieldType  
The type of the field. (Required)
- value  
The name of the variable to store to the field. (Required)

### Instruction &lt;getfield&gt; ###
It defines an instruction for getting the value of an instance class field. It can have the following attributes.

- ref  
The name of the variable holding the reference to the instance. (Required)
- class  
It defines the fully qualified class name. (Required)
- field  
The name of the field. (Required)
- fieldType  
The type of the field. (Required)
- def  
Define a new varaible to hold the obtained value. (Required)

### Instruction &lt;aastore&gt; ###
It defines an instruction for storing a value into an array. It can have the following attributes.

- ref  
The name of the variable holding the reference to the array. (Required)
- index  
The name of the variable holding the index of the array to store the value. (Required)
- eleType  
The element type of the array. For an array of type "java.lang.String[]", it should be "java.lang.String". (Required)
- value  
The name of the variable holding the value to store to the array. (Required)

### Instruction &lt;aaload&gt; ###
It defines an instruction for loading a value from an array. It can have the following attributes.

- ref  
The name of the variable holding the reference to the array. (Required)
- index  
The name of the variable holding the index of the array to load the value. (Required)
- eleType  
The element type of the array. For an array of type "java.lang.String[]", it should be "java.lang.String". (Required)
- def  
The name of the variable to store the value from the array. (Required)

### Instruction &lt;throw&gt; ###
It defines an instruction for throwing an exception. It can have the following attributes.

- value  
The name of the variable holding the value to be thrown. (Required)

### Instruction &lt;phi&gt; ###
In SSA form, because each variable can be assigned only once, it is required to have a special *phi* function to express conditional branches. However, in our current implementation, we doesn't allow branch instructions to be specified in the method summary. Thus, this instruction is just implemented for extension in the future. This element can have the following attributes.

- def  
Define a new variable to hold the result. (Required)
- numArgs  
Number of arguments of the phi function. (Required)
- arg{i}  
The {i}th argument of the phi function. (Required)

## Helper Jar ##
For simple method summary, it would be easier to express in the above XML format. However, if more complicated method summary is needed, it would be cumbersome. Thus, we also allow you to specify part of the method summary directly in Java source code. You may write the complicated part of your method summary in Java source code. To avoid confliction with the classes in Android library and app code, it is recommended to put your code in a special Java package. Then, compile your code into a Jar file, and move it to *config/apiXXX/method_summary_helper.jar*. To use the methods defined in the Jar file, just add *call* instructions to invoke your methods in the XML file. For example, 

    <package name="java.lang">
        <class name="System">
            <method signature="void arraycopy(java.lang.Object, int, java.lang.Object, int, int)"
                <call type="static"
                    signature="void arraycopy(java.lang.Object, java.lang.Object)"
            		class="org.droidslicer.methodsummary.java.lang.System" arg0="arg0"
            		arg1="arg2" exception="ex"/>
                <return />
            </method>
        </class>
    </package>

Here, the class *org.droidslicer.methodsummary.java.lang.System* is implemented in the Jar file.