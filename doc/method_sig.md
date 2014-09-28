# Method Signature #
In some of the configuration files (e.g.  *config/apiXXX/api_permissions.xml*), we use a string to describe a method signature. The string is very much like the method definition you would write in Java source code, except that there's no parameter name.

For example, a method signature may look like *void saveAttachment(android.content.Context, java.io.InputStream, com.android.emailcommon.provider.EmailContent$Attachment)*.

Notice that for the last parameter type, we write *com.android.emailcommon.provider.EmailContent$Attachment*, instead of *com.android.emailcommon.provider.EmailContent.Attachment*. This because that *Attachment* is a [nested class](http://docs.oracle.com/javase/tutorial/java/javaOO/nested.html) of the class *EmailContent*.
