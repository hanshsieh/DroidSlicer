package org.droidslicer;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public class LoggingPane extends JScrollPane
{
	private static final long serialVersionUID = -5920360753852742178L;
	private class CustomAppender extends AppenderBase<ILoggingEvent>
	{
		@Override
		protected void append(ILoggingEvent event)
		{
			if(mLayout == null)
			{
				error(LoggingPane.class.getName() + ": Layout isn't set");
				return;
			}
			String msg = mLayout.doLayout(event);
			Level level = event.getLevel();
			switch(level.toInt())
			{
			case Level.ERROR_INT:
				error(msg);
				break;
			case Level.WARN_INT:
				warn(msg);
				break;
			case Level.INFO_INT:
				info(msg);
				break;
			case Level.DEBUG_INT:
				debug(msg);
				break;
			case Level.TRACE_INT:
				trace(msg);
				break;
			}
		}
	}
	private final CustomAppender mAppender = new CustomAppender();
	private int mMaxItems = 200;
	private Layout<ILoggingEvent> mLayout = null;
	private Deque<Integer> mItemsLen = new ArrayDeque<Integer>();
	private final JTextPane mPane = new JTextPane();
	private final static SimpleAttributeSet MSG_TRACE_STYLE=
			new SimpleAttributeSet();
	private final static SimpleAttributeSet MSG_DEBUG_STYLE=
			new SimpleAttributeSet();
	private final static SimpleAttributeSet MSG_INFO_STYLE=
		new SimpleAttributeSet();
	private final static SimpleAttributeSet MSG_WARN_STYLE=
		new SimpleAttributeSet();
	private final static SimpleAttributeSet MSG_ERROR_STYLE=
		new SimpleAttributeSet();
	static
	{
		// initialize trace style
		StyleConstants.setFontSize(MSG_TRACE_STYLE, 12);
		StyleConstants.setForeground(MSG_TRACE_STYLE, Color.BLACK);
		
		// initialize debug style
		StyleConstants.setFontSize(MSG_DEBUG_STYLE, 12);
		StyleConstants.setForeground(MSG_DEBUG_STYLE, Color.BLACK);

		// initialize info style
		StyleConstants.setFontSize(MSG_INFO_STYLE, 12);
		StyleConstants.setForeground(MSG_INFO_STYLE, new Color(0x00, 0x80, 0x00));
		
		// initialize warn style
		StyleConstants.setFontSize(MSG_WARN_STYLE, 12);
		StyleConstants.setForeground(MSG_WARN_STYLE, new Color(0xB4, 0x5F, 0x04));
		
		// initialize error style
		StyleConstants.setFontSize(MSG_ERROR_STYLE, 12);
		StyleConstants.setForeground(MSG_ERROR_STYLE, Color.RED);
	}
	public LoggingPane()
	{
		setViewportView(mPane);
		mPane.setEditable(false);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mAppender.start();
	}
	public int getMaximumItems()
	{
		return mMaxItems;
	}
	public void setMaximumItems(int maxItems)
	{
		if(maxItems < 0)
			throw new IllegalArgumentException();
		mMaxItems = maxItems;
		checkItemNum();
	}
	public Appender<ILoggingEvent> getAppender()
	{
		return mAppender;
	}
	public void setLayout(Layout<ILoggingEvent> layout)
	{
		mLayout = layout;
	}

	public void info(String msg)
	{
		addRawMsg(msg, MSG_INFO_STYLE);
	}
	public void error(String msg)
	{
		addRawMsg(msg, MSG_ERROR_STYLE);
	}
	public void debug(String msg)
	{
		addRawMsg( msg, MSG_DEBUG_STYLE);
	}
	public void warn(String msg)
	{
		addRawMsg(msg, MSG_WARN_STYLE);
	}
	public void trace(String msg)
	{
		addRawMsg(msg, MSG_TRACE_STYLE);
	}
	private void checkItemNum()
	{
		int removeLen=0;
		while(mItemsLen.size() > mMaxItems)
			removeLen += mItemsLen.removeFirst();
		if(removeLen > 0)
		{
			Document doc = mPane.getDocument();
			try
			{
				doc.remove(0, removeLen);
			}
			catch(BadLocationException ex)
			{}
		}
	}
	private void addRawMsg(String msg, AttributeSet attr)
	{
		Document doc = mPane.getDocument();
		try
		{
			doc.insertString(doc.getLength(), msg, attr);
			mItemsLen.addLast(msg.length());
			checkItemNum();
			scrollToBottom();
		}
		catch(BadLocationException ex)
		{}
	}
	public void scrollToBottom()
	{
		mPane.setCaretPosition(mPane.getDocument().getLength());
	}
}
