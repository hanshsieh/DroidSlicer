package org.droidslicer.util;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public enum TypeId 
{
	INVALID,

	// java.io.*
	FILE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/File")),
	INPUT_STREAM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/InputStream")),
	OUTPUT_STREAM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/OutputStream")),
	FILE_INPUT_STREAM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/FileInputStream")),
	FILE_OUTPUT_STREAM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/FileOutputStream")),
	FILE_DESCRIPTOR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/FileDescriptor")),
	IO_FILE_SYSTEM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/FileSystem")), // This class is undocumented
	
	// java.nio.*
	FILE_SYSTEM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/nio/file/FileSystem")),
	
	// java.lang.*
	OBJECT(TypeReference.JavaLangObject),
	STRING(TypeReference.JavaLangString),
	STR_BUILDER(TypeReference.JavaLangStringBuilder),
	CLASS(TypeReference.JavaLangClass),
	RUNTIME(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Runtime")),
	PROCESS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Process")),
	RUNNABLE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Runnable")),
	SYSTEM(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/System")),
	
	// java.util.ArrayList
	ARRAY_LIST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/ArrayList")),
	LIST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/List")),
	COLLECTION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/Collection")),
	ARRAYS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/Arrays")),
	
	// java.util.concurrent.*
	EXECUTOR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/concurrent/Executor")),
	
	// java.net.*
	URI(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/URI")),
	URL(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/URL")),
	URL_CONNECTION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/URLConnection")),
	HTTP_URL_CONNECTION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/HttpURLConnection")),
	JAR_URL_CONNECTION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/JarURLConnection")),
	URL_STREAM_HANDLER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/URLStreamHandler")),
	PROXY(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/Proxy")),
	SOCKET(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/Socket")),
	SERVER_SOCKET(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/ServerSocket")),
	INET_ADDRESS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/InetAddress")),
	SOCKET_ADDRESS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/SocketAddress")),
	INET_SOCKET_ADDRESS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/InetSocketAddress")),
	
	// javax.net.*
	HTTPS_URL_CONNECTION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljavax/net/ssl/HttpsURLConnection")),
	SOCKET_FACTORY(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljavax/net/SocketFactory")),
	
	// android.app.*
	ANDROID_ACTIVITY(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Activity")),
	ANDROID_SERVICE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Service")),
	ANDROID_APPLICATION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Application")),
	ANDROID_ACTIVITY_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/ActivityManager")),
	ANDROID_ALARM_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/AlarmManager")),
	ANDROID_NOTIFICATION_MAANGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/NotificationManager")),
	ANDROID_KEYGUARD_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/KeyguardManager")),
	ANDROID_SEARCH_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/SearchManager")),
	ANDROID_UI_MODE_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/UiModeManager")),
	ANDROID_DOWNLOAD_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/DownloadManager")),
	ANDROID_PENDING_INTENT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/PendingIntent")),
	
	// android.util.*
	ANDROID_ATTRIBUTE_SET(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/util/AttributeSet")),
	
	// android.net.*
	ANDROID_URI(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/net/Uri")),
	ANDROID_URI_BUILDER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/net/Uri$Builder")),
	
	// android.database.*
	ANDROID_SQLITE_DB(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/sqlite/SQLiteDatabase")),
	ANDROID_SQLITE_STATEMENT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/sqlite/SQLiteStatement")),
	ANDROID_SQLITE_DB_CURSOR_FACTORY(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/sqlite/SQLiteDatabase$CursorFactory")),
	ANDROID_CURSOR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/Cursor")),
	ANDROID_DB_ERR_HANDLER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/DatabaseErrorHandler")),
	ANDROID_SQLITE_OPEN_HELPER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/database/sqlite/SQLiteOpenHelper")),
	
	// android.content.*
	ANDROID_PROVIDER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ContentProvider")),
	ANDROID_RECEIVER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/BroadcastReceiver")),
	ANDROID_CONTENT_VALUES(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ContentValues")),
	ANDROID_INTENT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/Intent")),
	ANDROID_CONTEXT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/Context")),
	ANDROID_CONTEXT_WRAPPER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ContextWrapper")),
	ANDROID_APP_INFO(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/pm/ApplicationInfo")),
	ANDROID_CONTENT_RESOLVER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ContentResolver")),
	ANDROID_RESOURCES(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/res/Resources")),
	ANDROID_COMPONENT_NAME(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ComponentName")),
	ANDROID_INTENT_FILTER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/IntentFilter")),
	ANDROID_SHARED_PREFERENCES(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/SharedPreferences")),
	ANDROID_SHARED_PREFERENCES_EDITOR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/SharedPreferences$Editor")),
	ANDROID_ASSET_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/res/AssetManager")),
	
	// android.os.*
	ANDROID_PARCEL_FILE_FD(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/ParcelFileDescriptor")),
	ANDROID_CANCELLATION_SIGNAL(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/CancellationSignal")),
	ANDROID_POWER_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/PowerManager")),
	ANDROID_VIBRATOR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Vibrator")),
	ANDROID_BUNDLE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Bundle")),
	ANDROID_USER_HANDLE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/UserHandle")),
	ANDROID_HANDLER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Handler")),
	ANDROID_ENVIRONMENT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Environment")),
	ANDROID_ASYNC_TASK(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/AsyncTask")),
	ANDROID_PARCEL(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Parcel")),
	
	// android.view.*
	ANDROID_WINDOW_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/view/WindowManager")),
	ANDROID_LAYOUT_INFLATER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/view/LayoutInflater")),
	ANDROID_INPUT_METHOD_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/view/inputmethod/InputMethodManager")),
	
	// android.location.*
	ANDROID_LOCATION_MANAGER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/location/LocationManager")),
	ANDROID_LOCATION_LISTENER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/location/LocationListener")),
	ANDROID_LOCATION(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/location/Location")),
	
	// android.net.*
	ANDROID_CONNECTIVITY_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/net/ConnectivityManager")),
	ANDROID_WIFI_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/net/wifi/WifiManager")),

	// android.telephony.*
	ANDROID_SMS_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/telephony/SmsManager")),
	ANDROID_TELEPHONY_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/telephony/TelephonyManager")),
	
	// android.preference.*
	ANDROID_PREFERENCE_MGR(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/preference/PreferenceManager")),

	// org.apache.http.*
	APACHE_HTTP_HOST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/HttpHost")),
	APACHE_HTTP_REQUEST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/HttpRequest")),
	APACHE_HTTP_URI_REQUEST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpUriRequest")),
	APACHE_HTTP_RESPONSE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/HttpResponse")),
	APACHE_RESPONSE_HANDLER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/ResponseHandler")),
	APACHE_HTTP_REQUEST_BASE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpRequestBase")),
	APACHE_HTTP_DELETE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpDelete")),
	APACHE_HTTP_GET(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpGet")),
	APACHE_HTTP_HEAD(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpHead")),
	APACHE_HTTP_OPTIONS(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpOptions")),
	APACHE_HTTP_TRACE(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpTrace")),
	APACHE_HTTP_POST(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpPost")),
	APACHE_HTTP_PUT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/methods/HttpPut")),
	APACHE_HTTP_CLIENT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/HttpClient")),
	APACHE_HTTP_CONTEXT(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/protocol/HttpContext")),
	APACHE_HTTP_ENTITY(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/HttpEntity")),

	// org.xmlpull.*
	XML_PULL_PARSER(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/xmlpull/v1/XmlPullParser"));
	
	private final TypeReference mTypeRef;
	private TypeId()
	{
		mTypeRef = null;
	}
	private TypeId(TypeReference typeRef)
	{
		mTypeRef = typeRef;
	}
	public TypeReference getTypeReference()
	{
		return mTypeRef;
	}
		
	private final static Map<TypeName, TypeId> TYPE_MAP = 
			new HashMap<TypeName, TypeId>();
	static
	{
		for(TypeId type : TypeId.values())
		{
			TypeReference typeRef = type.getTypeReference();
			if(typeRef != null)
				TYPE_MAP.put(typeRef.getName(), type);
		}
	}
	public static TypeId getTypeId(TypeName typeName)
	{
		TypeId typeId = TYPE_MAP.get(typeName);
		return typeId == null ? INVALID : typeId;
	}
}

