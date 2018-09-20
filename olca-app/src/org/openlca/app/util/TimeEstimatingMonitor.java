package org.openlca.app.util;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;

public class TimeEstimatingMonitor {

	private final IProgressMonitor monitor;
	private final boolean showSeconds;
	private final Queue<Long> lastTimes = new LinkedList<>();
	private long time;
	private int worked;
	private int total;
	private long lastTimeSet;
	private int considerationValue;
	private boolean showedTime;

	public TimeEstimatingMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
		this.showSeconds = false;
	}

	public void beginTask(String task, int total) {
		this.total = total > 0 ? total : 0;
		worked = 0;
		time = Calendar.getInstance().getTimeInMillis();
		lastTimeSet = time;
		lastTimes.clear();
		considerationValue = total < 100 ? -1 : total / 25;
		showedTime = false;
		monitor.beginTask(task, total);
	}

	public void endTask() {
		monitor.subTask("");
	}

	public void done() {
		monitor.done();
	}

	public void worked() {
		worked++;
		monitor.worked(1);
		long remainingTime = getRemainingTime();
		long current = Calendar.getInstance().getTimeInMillis();
		long took = current - lastTimeSet;
		if (took < 1000)
			return;
		if (lastTimes.size() < considerationValue && took < 10000)
			return;
		if (!showedTime && (remainingTime < 150 || worked > (total / 10)))
			return;
		showedTime = true;
		String remaining = formatTime((long) remainingTime);
		if (remaining.isEmpty()) {
			monitor.subTask("");
		} else {
			monitor.subTask(M.EstimatedTimeRemaining + " " + remaining);
		}
		lastTimeSet = current;
	}

	private long getRemainingTime() {
		long current = Calendar.getInstance().getTimeInMillis();
		long took = current - time;
		time = current;
		if (lastTimes.size() == considerationValue)
			lastTimes.poll();
		lastTimes.add(took);
		int count = 0;
		long lastTook = 0;
		for (Long t : lastTimes) {
			lastTook += t;
			count++;
		}
		double per = lastTook / (double) count;
		int remaining = total - worked;
		return (long) Math.ceil((per * remaining) / 1000d);
	}

	private String formatTime(long t) {
		if (t < 0)
			return "";
		String s = "";
		long h = t / 3600;
		if (h > 0) {
			t = t % 3600;
			s = h + "h ";
		}
		if (h > 2)
			return s;
		long m = t / 60;
		if (m > 0) {
			t = t % 60;
			if (!showSeconds && t > 30) {
				m++;
			}
			s += m + "m ";
		}
		if (!showSeconds || h > 0 || m > 9) {
			if (s.isEmpty())
				return "< 1m";
			return s;
		}
		if (t > 0)
			s += t + "s";
		return s;
	}
}