# Sensitive Behavior Signature #
Sensitive behavior signatures defines the sensitive behaviors to match with an app.  

## Definition ##
The file is written in XML format. It is parsed by the class **org.droidslicer.config.BehaviorSignatureParser**. The following example shows the top level elements.


    <?xml version="1.0" ?>
    <spec>
        <data-spec>
        </data-spec>
        <flow-spec>
        </flow-spec>
        <signatures>
        </signatures>
    </spec>

**<data-spec>** element is used to define the sensitive data of interest.  
**<flow-spec>** element is used to define the sensitive dataflow of interest.  
**<signatures>** element is used to define the sensitive behavior signatures.

The following example shows an example of top level elements of **<data-spec>**.

    <data-spec>
        <component isSystem="false" type="any">
            <data id="data_id">
            </data>
        </component>
    </data-spec>

### <data-spec> ###

The **<data-spec>** element can contain multiple **<component>** elements. Each **<component>** element is used to define the component restriction of a piece of sensitive data.  **<component>** element can have the following attributes.  
- isSystem  
It defines whether the program point associated with the sensitive data should be control-flow-reachable from a system component. The value can be **true**, **false**, and **any**. A system component is a component in the Android framework, and a non-system component is a component defined in the app.

- type  
It defines that the program point associated with the sensitive data should be control-flow-reachable from a component of a specific type. The value can be **activity**, **receiver**, **provider**, **service**, **application**, and **any**.  

Each **<component>** element can contain multiple **<data>** elements. Each **<data>** element defines a type of sensitive data.  **<data>** element can contain the following attributes.
- id  
The ID of the data. The ID should be unique among the ID's of the **<data>** elements and **<flow-spec>** elements.  

**<data>** element can contain one or more elements, and each element beneath it describes a type of sensitive-data-use node in the Sensitive Behavior Graph, and the **<data>** element describes the union of these sensitive-data-use nodes. The types of elements that **<data>** can contain are listed below.
- <database>  
It matches the sensitive-data-use point of SQLite database access.
- <file>  
It matches the sensitive-data-use point of normal file access.  
It can contain the following attributes:
    - path  
    The path pattern of the file. See [pattern string] for the detail of the format. (Optional)
- <shared-preferences>  
It matches the sensitive-data-use point of preference file access.
- <socket>  
It matches the sensitive-data-use point of access to a socket.
It can contain the following attributes:
    - addr  
    The pattern of the remote address of the socket. For example, it can be something like *.google.com. See [pattern string] for the detail of the format. (Optional)
    - port  
    The remote port of the socket. (Optional)
- <url-conn>  
It matches the sensitive-data-use point of access to URL connection.
- <icc-param-caller>  
It matches an ICC-parameter-caller node in the Sensitive Behavior Graph. 
- <icc-param-callee>  
It matches an ICC-parameter-callee node in the Sensitive Behavior Graph.
- <icc-ret-caller>  
It matches an ICC-return-caller node in the Sensitive Behavior Graph.
- <icc-ret-callee>  
It matches an ICC-return-callee node in the Sensitive Behavior Graph.

### <flow-spec> ###
The **<flow-spec>** element can contain multiple **<flow>** elements, and each **<flow>** element describes a type of sensitive dataflow.

The **<flow>** element can contain the following attributes.
- id  
The ID of assigned to the dataflow. This ID should be unique among all the ID's of the **<flow>** elements and **<data>** elements.
- from  
The ID of a **<data>** element, which is the source of the flow.
- to  
The ID of a **<data>** element, which is the destination of the flow.

### <signatures> ###
The **<signatures>** element can contain multiple **<signature>** elements, and each **<signature>** element describes a signature.

The **<signature>** element can contain the following attributes.
- id  
The ID assigned to the signature. It should be unique among all the ID's of the **<signature>** elements.
- def  
The definition of a signature. The value of this attribute should be a Boolean formula describing the definition of the signature. It accepts AND, OR, and NOT operators. Left and right parentheses can be used. The operands should be the ID's of **<data>** and **<flow>** elements. For example, if there's a **<data>** element with ID "DATA1", and a **<flow>** element with ID "FLOW1", then the Boolean formula "DATA1 AND FLOW1" with match any app with a sensitive-data-use node described by "DATA1" and a sensitive dataflow described by FLOW1.

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