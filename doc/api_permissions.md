# API Permissions #
To detect access to sensitive data, we need to know which API will access which type of sensitive data. We define sensitive data to be permission protected data and data from the file system. Thus, we need to known which API's are protected by which permissions.

*config/apiXXX/api_permissions.xml* is a file for defining the relation between API's and permissions, where *XXX* is the Android API level. It is a XML file, and its format will be described below. The parser implementation is *org.droidslicer.config.APIPermissionParser*.

## Format ##
### &lt;api-spec&gt; and &lt;class-loader&gt; ###
The root element is *&lt;api-spec&gt;*, and there can be several *&lt;class-loader&gt;* elements. The *&lt;class-loader&gt;* elements are to distinguish the classes that are defined in different namespaces. For the meaning of a class loader here, see [here](class_loader.html). A *&lt;class-loader&gt;* element can have an attribute *name*, which defines the name of the class loader, e.g. Primordial. Because we want to define the permissions of APIs in Android library, normally there should be only one *&lt;class-loader&gt;* element with *name* being *Primordial*.  

Under a *&lt;class-loader&gt;* element, the hierarchy looks like below.

    <classloader>
        <package>
            <class>
                <method />
            </class>
        </package>
    </classloader>

For each of *&lt;package&gt;*, *&lt;class&gt;*, and *&lt;method&gt;* elements, there can be multiple children of the type of its next one. For example, there can be multiple *&lt;class&gt;* elements under a *&lt;package&gt;* element.

### &lt;package&gt; ###
Each *&lt;package&gt;* element describes a package that would be loaded by a class loader. It can have an attribute *name*, which defines the name of the package, e.g. *android.app*.  


### &lt;class&gt; ###
Each *&lt;class&gt;* element decribes a class inside a package. It can have an attribute *name*, which defines the name of the class, e.g. *Activity*.


### &lt;method&gt; ###
Each *&lt;method&gt;* element describes a method inside a class. It can have the following attributes.

- signature  
It describes the signature of the method. E.g. "void dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[])". For further information, see [here](method_sig.html). (Required)
- static
It is used to expressed whether the method is static or not. Its value should be either "true" or "false". (Required)
- permission  
It describes the permission required when the method is invoked. Its value should be a comma-seperated list of permissions. E.g. "android.permission.READ_PHONE_STATE,android.permission.ACCESS_WIFI_STATE". (Optional)
- return  
It describes whether the dataflow *from* the return value should be tracked when the method is invoked. If the value is "track", then the dataflow of the return value will be tracked; otherwise, it won't. (Optional)
- param{i}  
It is a comma-seperated seperated list of options. param{i} describes the options of {i}th parameter. The 0th parameter denotes "this". Each option denotes a boolean predicate, and the default value is false. The following options are allowed.
    - track  
    It defines whether the dataflow *to* the {i}th argument should be tracked.
    - resolve  
    Analysts may be interested of the possible concrete value of an argument when the method is invoked. This option defines whether the value of the {i}th argument should be resolved. It won't affect the analysis precision. The resolved information is just for the reference of analysts.
    - listener  
    It defines whether {i}th parameter is a listener that will be registered by the method. For example, API  *LocationManager.requestLocationUpdates(String, long, float, LocationListener)* will register the 4th parameter as listener.
    - trackListener  
    When a listener is registered, the listener may be invoked later by the Android framework. And when it happens, some data may be passed as arguments of the invocation. This option defines whether the dataflow *from* the arguments of the invocation should be tracked.

## Construction ##
[PScout] is a tool for analyzing the Android permission specification. One of its output is the mapping between permissions and Android API. You may download the file of the mapping from its official website, and utilize our helper class *org.droidslicer.pscout.PScoutAPIPermToXML* to generate the configuration file for API permissions.

For the detail usage of the class, launch our tool with it as main class without arguments.

    java -jar DroidSlicer.jar org.droidslicer.pscout.PScoutAPIPermToXML



[PScout]:http://pscout.csl.toronto.edu/