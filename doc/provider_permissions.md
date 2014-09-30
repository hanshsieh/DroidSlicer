# Provider Permissions #
In the Android framework, there're many Content Provider. An app may query these Content Provider to access the system information. In configuration file *config/apiXXX/provider_permissions.xml* (XXX is the Android API level), Content Providers in the Android framework and the permissions requirement to query them are defined. The configuration file is a XML file. The parser implementaion of this file is the class *org.droidslicer.config.ProviderPermissionParser*, and the format of this file is described below.

## Format ##
The root element is *&lt;provider-spec&gt;*, and the hierarchy is like below

    <provider-spec>
        <provider>
            <path-permission />
        </provider>
    </provider-spec>
There can be contain multiple *&lt;provider&gt;* elements. Each *&lt;provider&gt;* element describes a Content Provider in the Android framework, and it can have the following attributes:

- authorities  
Semicolon-seperated list of authorities, e.g. "com.android.contacts;contacts".
- readPermission  
The permission required to read from the provider, e.g. "android.permission.READ_CONTACTS".
- writePermission  
The permission required to write to the provider, e.g. "android.permission.WRITE_CONTACTS".

A *&lt;provider&gt;* element can contain multiple *&lt;path-permission&gt;* elements. Each *&lt;path-permission&gt;* describe a permission requirement for a URI path. See the Android document for [&lt;path-permission&gt;] for more information. A *&lt;path-permission&gt;* element can contain the following attributes:

- path
- pathPrefix
- pathPattern
- readPermission
- writePermission
- permission

They correspond to the attributes described in [&lt;path-permission&gt;].


[&lt;path-permission&gt;]:http://developer.android.com/guide/topics/manifest/path-permission-element.html
