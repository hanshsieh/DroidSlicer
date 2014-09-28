# Entry Points #
When a component receives an ICC, some of its methods may be invoked by the Android framework. We call such methods as ICC entry points.  
The entry points of each type component is defined in the XML file */config/apiXXX/entry_points.xml*, and its format is defined below.  

## Format ##
The root element is *&lt;entry-spec&gt;*, and its can contain the following elements:

- &lt;application&gt;  
- &lt;activity&gt;
- &lt;receiver&gt;
- &lt;provider&gt;
- &lt;service&gt;

Each child element of *&lt;entry-spec&gt;* defines the entry points of a type of component, and each can further contain multiple *&lt;method&gt;* elements.  
Each *&lt;method&gt;* element defines an entry point of the component. It can contain the following attributes.

- signature  
The signature of the method. E.g. *void onActionModeFinished(android.view.ActionMode)*. For further information, see [here](method_sig.html). (Required)
- static  
It defines whether the method is static or not. The allowed values are "true" and "false". (Required)