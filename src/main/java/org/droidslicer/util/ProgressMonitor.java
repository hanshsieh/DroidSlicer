package org.droidslicer.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class ProgressMonitor implements IProgressMonitor
{
	private volatile String mTaskName = null;
	private volatile String mSubTaskName = null;
	private volatile int mTotalWork = -1;
	private volatile boolean mCanceled = false;
	private volatile int mWorked = 0;
	private volatile boolean mDone = false;
	private volatile ProgressMonitor mSubProgressMonitor = null;
	@Override
	synchronized public void beginTask(String task, int totalWork)
	{
		if(task == null)
			throw new IllegalArgumentException("Task name cannot be null");
		if(mTaskName != null)
			throw new IllegalStateException("The task has already been started");
		mTaskName = task;
		mTotalWork = totalWork;
		mWorked = 0;
		mSubProgressMonitor = null;
	}

	public String getTaskName()
	{
		return mTaskName;
	}
	public String getSubTaskName()
	{
		return mSubTaskName;
	}
	@Override
	public boolean isCanceled()
	{
		return mCanceled;
	}
	synchronized public void setCanceled(boolean canceled)
	{
		mCanceled = canceled;
		if(mSubProgressMonitor != null)
			mSubProgressMonitor.setCanceled(canceled);
	}

	synchronized public void setSubProgressMonitor(ProgressMonitor subMonitor)
	{
		mSubProgressMonitor = subMonitor;
	}
	public ProgressMonitor getSubProgressMonitor()
	{
		return mSubProgressMonitor;
	}
	public void subTask(String name)
	{
		mSubTaskName = name;
	}
	@Override
	synchronized public void done()
	{
		if(mTotalWork > 0 && mWorked < mTotalWork)
			worked(mTotalWork - mWorked);
		mDone = true;
		mSubProgressMonitor = null;
	}
	public boolean isDone()
	{
		return mDone;
	}

	@Override
	synchronized public void worked(int units)
	{
		if(units < 0)
			throw new IllegalArgumentException("Units of work cannot be negative");
		if(mTotalWork >= 0)
			mWorked = Math.min(mWorked + units, mTotalWork);
		else
			mWorked += units;
	}
	public int getWorked()
	{
		return mWorked;
	}
	public int getTotalWork()
	{
		return mTotalWork;
	}
}
