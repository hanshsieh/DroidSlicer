# API Permissions #
To detect access to sensitive data, we need to know which API will access which type of sensitive data. We define sensitive data to be permission protected data and data from the file system. Thus, we need to known which API's are protected by which permissions.

**config/apiXXX/api_permissions.xml** is a file for defining the relation between API's and permissions. It is a XML file, and its format will be described below. The parser implementation is **org.droidslicer.config.APIPermissionParser**.

## <api-spec> and <class-loader> ##
The root element is **<api-spec>**, and there can be several **<class-loader>** elements. The **<class-loader>** elements are to distinguish the classes that are defined in different namespaces, and they mimics the class loaders in Java. **<class-loader>** can have an attribute **name**, which define the name of the class loader.

Following are the available class loaders.
- Primordial  
A class loader for Android library code.  
- Application  
A class loader for app code in classes.dex (a file in APK for storing app code).  
- Extension  
A class loader for the app code which is not in classes.dex, but is dynamically loaded. (Currently not supported by our system)  

Because we want to define the permissions of APIs in Android library, normally there should be only one **<class-loader>** element with **name** being Primordial.  

Under a **<class-loader>** element, the hierarchy looks like below.

    <classloader>
        <package>
            <class>
                <method />
            </class>
        </package>
    </classloader>

For each of **<package>**, **<class>**, and **<method>** elements, there can be multiple children of the type of its next one. For example, there can be multiple **<class>** elements under a **<package>** element.

## <package> ##
Each **<package>** element describes a package that would be loaded by a class loader. It can have an attribute **name**, which defines the name of the package, e.g. **android.app**.  


## <class> ##
Each **<class>** element decribes a class inside a package. It can have an attribute **name**, which defines the name of the class, e.g. **Activity**.


## <method> ##
Each **<method>** element describes a method inside a class. It can have the following attributes.
- signature  
It describes the signature of the method. It is expressed in the format as what you would write in Java source code. E.g. "void dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[])". However, do not include parameter names in the signature, because our parser currently do not handle it. (Required)
- static
It is used to expressed whether the method is static or not. Its value should be either "true" or "false". (Required)
- permission  
It describes the permission required when the method is invoked. Its value should be a comma-seperated list of permissions. E.g. "android.permission.READ_PHONE_STATE,android.permission.ACCESS_WIFI_STATE". (Optional)
- return  
It describes whether the dataflow *from* the return value should be tracked when the method is invoked. If the value is "track", then the dataflow of the return value will be tracked; otherwise, it won't. (Optional)
- param{i}  
It describes the options of {i}th parameter. The 0th parameter denotes "this". Each option describes a boolean predicate, and the default value is false. The following options are allowed.
    - track  
    It defines whether the dataflow *to* the {i}th argument should be tracked.
    - resolve  
    Analysts may be interested of the possible concrete value of an argument when the method is invoked. This option defines whether the value of the {i}th argument should be resolved. It won't affect the analysis precision. The resolved information is just for the reference of analysts.
    - listener  
    It defines whether {i}th parameter is a listener that will be registered by the method. For example, API  **LocationManager.requestLocationUpdates(String, long, float, LocationListener)** will register the 4th parameter as listener.
    - trackListener  
    When a listener is registered, the listener may be invoked later by the Android framework. And when it happens, some data may be passed as arguments of the invocation. This option defines whether the dataflow *from* the arguments of the invocation should be tracked.

