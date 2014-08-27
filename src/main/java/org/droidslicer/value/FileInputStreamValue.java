package org.droidslicer.value;

public class FileInputStreamValue extends AbstractFileStreamValue
{
	private final ConcreteValue mPath;
	
	/**
	 * The {@code path} should represent the absolute path of the file, not a 
	 * {@link java.io.File} instance.
	 * @param path
	 */
	public FileInputStreamValue(ConcreteValue path)
	{
		mPath = path;
	}
	@Override
	public ConcreteValue getPath()
	{
		return mPath;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// Maybe we should do better
		return this;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[FILE_INPUT_STREAM ");
		builder.append("path=");
		builder.append(mPath.toString());
		builder.append(']');
		return builder.toString();
	}
}
