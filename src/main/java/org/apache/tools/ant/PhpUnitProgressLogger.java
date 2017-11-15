package org.apache.tools.ant;

import java.io.PrintStream;

import org.apache.tools.ant.util.StringUtils;
import org.opfx.ant.php.task.Unit;

public class PhpUnitProgressLogger extends DefaultLogger {

	private int messageCount;
	private int columnCount;
	private DefaultLogger defaultLogger;

	public PhpUnitProgressLogger(DefaultLogger logger) {
		super();
		defaultLogger = logger;
		out = new PrintStream(logger.out);
		
		// err = logger.err;
		emacsMode = logger.emacsMode;
		msgOutputLevel = logger.msgOutputLevel;
		messageCount = 0;
		columnCount = 80;
	}

	public int getMessageOutputLevel() {
		return this.msgOutputLevel;
	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */
	public void buildStarted(BuildEvent event) {
	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */

	public void buildFinished(BuildEvent event) {
	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */

	public void targetStarted(BuildEvent event) {

	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */
	public void targetFinished(BuildEvent event) {
	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */
	public void taskStarted(BuildEvent event) {
	}

	/**
	 * No-op implementation.
	 *
	 * @param event
	 *            Ignored.
	 */
	public void taskFinished(BuildEvent event) {
	}

	/**
	 * Logs a message, if the priority is suitable. In non-emacs mode, task
	 * level messages are prefixed by the task name which is right-justified.
	 *
	 * @param event
	 *            A BuildEvent containing message information. Must not be
	 *            <code>null</code>.
	 */
	public void messageLogged(BuildEvent event) { 

		int priority = event.getPriority();
		if(event.getMessage().isEmpty()) {
			return;
		}
		if (priority > msgOutputLevel) {
			defaultLogger.messageLogged(event);
			return;
		}
		Task task = event.getTask();
		if (!(task instanceof Unit)) {
			defaultLogger.messageLogged(event);
			return;
		}

		String progress = event.getMessage();
		switch (progress) {
		case "#":
			progress = StringUtils.LINE_SEP;
		case ".":
			priority = Project.MSG_INFO;
			break;
		case "E":
		case "F":
		case "I":
		case "S":
			priority = Project.MSG_ERR;
			// stream = err;
			break;

		default:
			defaultLogger.messageLogged(event);
			return;
		}

		if (messageCount > columnCount) {
			messageCount = 0;
			progress = progress + StringUtils.LINE_SEP;
		}

		String label = "";
		if (messageCount == 0 && !emacsMode) {
			label = String.format("[%s] ", task.getTaskName());
			int size = LEFT_COLUMN_SIZE - label.length();
			StringBuffer tmp = new StringBuffer();
			for (int i = 0; i < size; i++) {
				tmp.append(" ");
			}
			tmp.append(label);
			label = tmp.toString();
		}
		progress = label + progress;

		printMessage(progress, out, priority);
	}

	/**
	 * Empty implementation which allows subclasses to receive the same output
	 * that is generated here.
	 *
	 * @param message
	 *            Message being logged. Should not be <code>null</code>.
	 */
	protected void log(String message) {
	}

	protected void printMessage(final String message, final PrintStream stream, final int priority) {
		messageCount++;
		stream.print(message);
		stream.flush();
	}

}
