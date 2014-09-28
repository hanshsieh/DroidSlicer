# Intent Permissions #
In Android, an app can send an Intent to start an Activity or to a Broadcast Receiver. Similarly, an app can also receive Broadcast sent by the Android framework. These are achieved by sending and receiving Intent's. For some types of Intent's, sending it to the Android framework or receiving it from the Android framework requires certain permissions.  

In the configuration file *config/apiXXX/intent_permissions.xml* (XXX is the Android API version), the permission requirement of sending and receiving Intent's is defined. The file is a XML file, and its format is described below.

## Format ##
The root element is *&lt;intent-spec&gt;*, and it can contain multiple *&lt;intent&gt;* children. Each *&lt;intent&gt;* element is used to described a type of Intent, and it can have the following attributes:

- action  
The action of the Intent. E.g. "android.intent.action.CALL".

An *&lt;intent&gt;* element can contain at most one *&lt;sender&gt;* element and at most one *&lt;receiver&gt;* element.

The *&lt;sender&gt;* element is used to describe the permission requirement for an app to send such an Intent, and the *&lt;receiver&gt;* element is used to describe the permission requirement for an app to receive such an Intent.

*&lt;sender&gt;* and *&lt;receiver&gt;* elements can have the following attributes:

- permission  
It defines the permission requirement of the sender/receiver. E.g. "android.permission.CALL_PHONE".

## Construction of the file ##

There's a class *org.droidslicer.pscout.PScoutAPIPermToXML* that can help you to parse the output file of [PScout], and generate this file. For the usage of this class, use this class as the main class without arguments, and it will print out instruction.

    java -jar DroidSlicer.jar org.droidslicer.pscout.PScoutAPIPermToXML

However, it is already known that some of the relations between permissions and Intent's are missing in the output file of PScout. For example, you may want to add the following lines to the generated file.

    <intent action="android.nfc.action.NDEF_DISCOVERED">
	    <receiver permission="android.permission.NFC"/>
    </intent>
    <intent action="android.nfc.action.TECH_DISCOVERED">
	    <receiver permission="android.permission.NFC"/>
    </intent>
    <intent action="android.nfc.action.TAG_DISCOVERED">
    	<receiver permission="android.permission.NFC"/>
    </intent>
    
Also, some types broadcast sent by the Android framework doesn't require any permissions, and the Intent's of these types of broadcast are also missed. Although not required, you may want to manually add the missing relations to the file. For example,

    <intent action="android.intent.action.USER_PRESENT" />
    <intent action="android.intent.action.USER_INITIALIZE" />
    <intent action="android.intent.action.USER_FOREGROUND" />
    <intent action="android.intent.action.USER_BACKGROUND" />
    <intent action="android.intent.action.SCREEN_ON" />
    <intent action="android.intent.action.SCREEN_OFF" />
    <intent action="android.intent.action.SIG_STR" />

[PScout]:http://pscout.csl.toronto.edu/