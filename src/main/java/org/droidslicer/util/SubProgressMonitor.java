package org.droidslicer.util;

public class SubProgressMonitor extends ProgressMonitor
{
	private final ProgressMonitor mSuperMonitor;
	private final int mSuperTaskUnits;
	private int mSuperWorked = 0;
	public SubProgressMonitor(ProgressMonitor superMonitor, int superTaskUnits)
	{
		if(superMonitor == null)
			throw new IllegalArgumentException();
		mSuperMonitor = superMonitor;
		mSuperTaskUnits = superTaskUnits;
		superMonitor.setSubProgressMonitor(this);
	}
	@Override
	synchronized public void worked(int units)
	{
		super.worked(units);
		if(mSuperTaskUnits >= 0)
		{
			int subTotal = getTotalWork();
			int workUnits;
			if(subTotal > 0)
				workUnits = (int)Math.ceil((double)mSuperTaskUnits * units / subTotal);
			else
				workUnits = 0;
			workUnits = Math.min(workUnits, mSuperTaskUnits - mSuperWorked);
			assert mSuperTaskUnits >= mSuperWorked;
			mSuperWorked += workUnits;
			mSuperMonitor.worked(workUnits);
		}
		else
			mSuperMonitor.worked(units);
	}
	@Override
	synchronized public void done()
	{
		super.done();
		assert mSuperTaskUnits >= mSuperWorked;
		int workUnits = mSuperTaskUnits - mSuperWorked;
		if(workUnits > 0)
			mSuperMonitor.worked(workUnits);
		mSuperMonitor.setSubProgressMonitor(null);
	}
}
