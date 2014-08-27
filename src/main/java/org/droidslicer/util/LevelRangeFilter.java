package org.droidslicer.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

public class LevelRangeFilter extends AbstractMatcherFilter<ILoggingEvent>
{	
	protected Level level;
	@Override
	public FilterReply decide(ILoggingEvent event)
	{
		if (!isStarted())
			return FilterReply.NEUTRAL;
		if(event.getLevel().isGreaterOrEqual(level))
			return onMatch;
		else
			return onMismatch;
	}
	public void setLevel(Level level)
	{
	    this.level = level;
	}
	public void start()
	{
		if (this.level != null)
	      super.start();
	}
}
