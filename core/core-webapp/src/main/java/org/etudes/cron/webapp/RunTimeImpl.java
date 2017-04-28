package org.etudes.cron.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cron.api.RunTime;
import static org.etudes.util.StringUtil.split;

public class RunTimeImpl implements RunTime
{
	/** */
	protected static int MINUTE_THRESHOLD = 5;

	/** Our log. */
	private static Log M_log = LogFactory.getLog(RunTimeImpl.class);

	/** The 0..23 hour of day. */
	protected int hour = 0;

	/** The 0..59 minute of hour. */
	protected int minute = 0;

	/**
	 * Construct
	 * 
	 * @param hour
	 *        The hour.
	 * @param minute
	 *        The minute.
	 */
	public RunTimeImpl(int hour, int minute)
	{
		this.hour = hour;
		this.minute = minute;
	}

	/**
	 * Construct from an HH:MM time string (24 hour format)
	 * 
	 * @param time
	 *        The time string.
	 */
	public RunTimeImpl(String time)
	{
		try
		{
			String[] parts = split(time, ":");
			this.hour = Integer.parseInt(parts[0]);
			this.minute = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			M_log.warn("invalid time spec:" + time);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			M_log.warn("invalid time spec:" + time);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getHour()
	{
		return this.hour;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getMinute()
	{
		return this.minute;
	}

	/**
	 * Check if the other time matches this one, or is just after within a threshold.
	 * 
	 * @param other
	 *        The other time.
	 * @return true if satisfied, false if not.
	 */
	public boolean isSatisfiedBy(RunTime other)
	{
		// same hour
		if (other.getHour() == this.hour)
		{
			// if other is up to threshold minutes past this time
			int delta = other.getMinute() - this.minute;
			if ((delta >= 0) && (delta <= MINUTE_THRESHOLD))
			{
				return true;
			}
		}

		// if it is the next hour, it might still be within the threshold
		else if (other.getHour() == ((this.hour + 1) % 24))
		{
			int delta = (other.getMinute() + 60) - this.minute;
			if ((delta >= 0) && (delta <= MINUTE_THRESHOLD))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return (this.hour < 10 ? "0" : "") + Integer.toString(this.hour) + ":" + (this.minute < 10 ? "0" : "") + Integer.toString(this.minute);
	}
}
