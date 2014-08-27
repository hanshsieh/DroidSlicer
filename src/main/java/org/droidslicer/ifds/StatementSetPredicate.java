package org.droidslicer.ifds;

import java.util.Collection;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.Predicate;

public class StatementSetPredicate extends Predicate<Statement>
{
	public final Collection<Statement> mStms;
	public StatementSetPredicate(Collection<Statement> stms)
	{
		mStms = stms;
	}
	@Override
	public boolean test(Statement stm)
	{
		return mStms.contains(stm);
	}

}
