# Sensitive Behavior Signature #
Sensitive behavior signatures defines the sensitive behaviors to match with an app.  
It is defined in *config/sensitive_behavior_signatures.xml*.

## Definition ##
The file is written in XML format. It is parsed by the class *org.droidslicer.config.BehaviorSignatureParser*. The following example shows the top level elements.


    <?xml version="1.0" ?>
    <spec>
        <data-spec>
        </data-spec>
        <flow-spec>
        </flow-spec>
        <signatures>
        </signatures>
    </spec>

*&lt;data-spec&gt;* element is used to define the sensitive data of interest.  
*&lt;flow-spec&gt;* element is used to define the sensitive dataflow of interest.  
*&lt;signatures&gt;* element is used to define the sensitive behavior signatures.

The following example shows an example of top level elements of *&lt;data-spec&gt;*.

    <data-spec>
        <component isSystem="false" type="any">
            <data id="data_id">
            </data>
        </component>
    </data-spec>

### &lt;data-spec&gt; ###

The *&lt;data-spec&gt;* element can contain multiple *&lt;component&gt;* elements. Each *&lt;component&gt;* element is used to define the component restriction of a piece of sensitive data.  *&lt;component&gt;* element can have the following attributes.  

- isSystem  
It defines whether the program point associated with the sensitive data should be control-flow-reachable from a system component. The value can be *true*, *false*, and *any*. A system component is a component in the Android framework, and a non-system component is a component defined in the app.

- type  
It defines that the program point associated with the sensitive data should be control-flow-reachable from a component of a specific type. The value can be *activity*, *receiver*, *provider*, *service*, *application*, and *any*.  

Each *&lt;component&gt;* element can contain multiple *&lt;data&gt;* elements. Each *&lt;data&gt;* element defines a type of sensitive data.  *&lt;data&gt;* element can contain the following attributes.

- id  
The ID of the data. The ID should be unique among the ID's of the *&lt;data&gt;* elements and *&lt;flow-spec&gt;* elements.  

*&lt;data&gt;* element can contain one or more elements, and each element beneath it describes a type of sensitive-data-use node in the Sensitive Behavior Graph, and the *&lt;data&gt;* element describes the union of these sensitive-data-use nodes. The types of elements that *&lt;data&gt;* can contain are listed below.

- &lt;database&gt;  
It matches the sensitive-data-use point of SQLite database access.
- &lt;file&gt;  
It matches the sensitive-data-use point of normal file access.  
It can contain the following attributes:
    - path  
    The path pattern of the file. See [pattern string] for the detail of the format. (Optional)
- &lt;shared-preferences&gt;  
It matches the sensitive-data-use point of preference file access.
- &lt;socket&gt;  
It matches the sensitive-data-use point of access to a socket.
It can contain the following attributes:
    - addr  
    The pattern of the remote address of the socket. For example, it can be something like *.google.com. See [pattern string] for the detail of the format. (Optional)
    - port  
    The remote port of the socket. (Optional)
- &lt;url-conn&gt;  
It matches the sensitive-data-use point of access to URL connection.
- &lt;icc-param-caller&gt;  
It matches an ICC-parameter-caller node in the Sensitive Behavior Graph. 
- &lt;icc-param-callee&gt;  
It matches an ICC-parameter-callee node in the Sensitive Behavior Graph.
- &lt;icc-ret-caller&gt;  
It matches an ICC-return-caller node in the Sensitive Behavior Graph.
- &lt;icc-ret-callee&gt;  
It matches an ICC-return-callee node in the Sensitive Behavior Graph.

### &lt;flow-spec&gt; ###
The *&lt;flow-spec&gt;* element can contain multiple *&lt;flow&gt;* elements, and each *&lt;flow&gt;* element describes a type of sensitive dataflow.  
The *&lt;flow&gt;* element can contain the following attributes.

- id  
The ID of assigned to the dataflow. This ID should be unique among all the ID's of the *&lt;flow&gt;* elements and *&lt;data&gt;* elements.
- from  
The ID of a *&lt;data&gt;* element, which is the source of the flow.
- to  
The ID of a *&lt;data&gt;* element, which is the destination of the flow.

### &lt;signatures&gt; ###
The *&lt;signatures&gt;* element can contain multiple *&lt;signature&gt;* elements, and each *&lt;signature&gt;* element describes a signature.

The *&lt;signature&gt;* element can contain the following attributes.

- id  
The ID assigned to the signature. It should be unique among all the ID's of the *&lt;signature&gt;* elements.
- def  
The definition of a signature. The value of this attribute should be a Boolean formula describing the definition of the signature. It accepts AND, OR, and NOT operators. Left and right parentheses can be used. The operands should be the ID's of *&lt;data&gt;* and *&lt;flow&gt;* elements. For example, if there's a *&lt;data&gt;* element with ID "DATA1", and a *&lt;flow&gt;* element with ID "FLOW1", then the Boolean formula "DATA1 AND FLOW1" with match any app with a sensitive-data-use node described by "DATA1" and a sensitive dataflow described by FLOW1.

## Example ##
The following is a concrete example of the XML file.

    <spec>
    	<data-spec>
            <component isSystem="true" type="any">
                <data id="DATA_RECEIVE_SMS">
                    <permission permissions="android.permission.RECEIVE_SMS"/>
                </data>
            </component>
            <component isSystem="false" type="any">
                <data id="DATA_INTERNET">
                    <permission permissions="android.permission.INTERNET"/>
                </data>
                <data id="DATA_APK_FILE">
                    <file path="*.apk" />
                </data>
            </component>
        </data-spec>
        <flow-spec>
            <flow 
                id="FLOW_RECEIVE_SMS_TO_INTERNET" 
                from="DATA_RECEIVE_SMS" 
                to="DATA_INTERNET" />
        </flow-spec>
        <signatures>
            <signature 
                id="SIG_SEND_SMS_TO_INTERNET"
                def="FLOW_RECEIVE_SMS_TO_INTERNET AND DATA_APK_FILE" />
        </signatures>
    </spec>
    
[pattern string]:pat_str.html