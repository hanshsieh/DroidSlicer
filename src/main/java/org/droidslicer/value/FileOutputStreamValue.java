package org.droidslicer.value;


public class FileOutputStreamValue extends AbstractFileStreamValue
{
	private final ConcreteValue mPath;
	/**
	 * The {@code path} should represent the absolute path of the file. It shouldn't represent a
	 * {@link java.io.File} instance.
	 * @param path the path of the file
	 */
	public FileOutputStreamValue(ConcreteValue path)
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
		builder.append('[');
		builder.append(FileOutputStreamValue.class.getSimpleName());
		builder.append(" path=");
		builder.append(mPath.toString());
		builder.append(']');
		return builder.toString();
	}
	
}