# Class Loader #
In the configuration file of our system, to distinguish the classes that are defined in different namespaces, we use the concept of "class loader", which mimics the class loaders in Java. This concept is inherited from [Wala].

Following are the available class loaders.

- Primordial  
A class loader for Android library code.  
- Application  
A class loader for app code in classes.dex (a file in APK for storing app code).  
- Extension  
A class loader for the app code which is not in classes.dex, but is dynamically loaded. (Currently not supported by our system)  

[Wala]:http://wala.sourceforge.net