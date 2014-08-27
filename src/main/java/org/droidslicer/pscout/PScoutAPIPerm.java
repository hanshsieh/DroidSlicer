package org.droidslicer.pscout;

public class PScoutAPIPerm
{
	private String packageName;
	private String className;
	private String returnType;
	private String methodName;
	private String[] argumentTypes;
	private String permission;
	public String getPackageName()
	{
		return packageName;
	}
	public void setPackageName(String pkgName)
	{
		packageName = pkgName;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getReturnType() {
		return returnType;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public String[] getArgumentTypes() {
		return argumentTypes;
	}
	public void setArgumentTypes(String[] argumentTypes) {
		this.argumentTypes = argumentTypes;
	}
	public String getPermission() {
		return permission;
	}
	public void setPermission(String permission) {
		this.permission = permission;
	}
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Permission: ");
		builder.append(permission);
		builder.append(' ');
		builder.append(className);
		builder.append(' ');
		builder.append(returnType);
		builder.append(' ');
		builder.append(methodName);
		builder.append('(');
		for(int i = 0; i < argumentTypes.length; ++i)
		{
			if(i != 0)
				builder.append(", ");
			builder.append(argumentTypes[i]);
		}
		builder.append(')');
		return builder.toString();
	}
}
