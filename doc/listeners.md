# Listeners #
In Android framework, there're many listener classes that allow developers to register an instance of them to be invoked when some event occurs.  

The file *config/apiXXX/listeners.txt* (XXX is the Android API level), list of listener classes are defined. The format of this file is described below.

## Format ##
For each line, if the first non-whitespace character is '#', then that line is treated as a comment. Otherwise, after trimming the whitespace characters off from the begining and the end, the line is interpreted as the fully-qualified name of a listener class. An example of the file is shown below.

    # This is a comment.
    android.location.LocationListener
    android.view.View$OnClickListener

## Construction ##
You may define the file manually, or utilize the helper class *org.droidslicer.config.ListenerFindingHelper* to generate it. Launch our tool using this class as the main class without any arguments to see its usage.

    java -jar DroidSlicer.jar org.droidslicer.config.ListenerFindingHelper

This helper class uses some simple heuristics to find the classes in the Android framework that *looks like* listener classes.
