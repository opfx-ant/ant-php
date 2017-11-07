package org.opfx.ant.php.task;

import java.io.File;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.PhpUnitProgressLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;
import org.opfx.ant.php.AbstractTask;
//TODO use 5.7.22
import org.opfx.ant.php.Constants;

public class Unit extends AbstractTask {

	private File bootstrap;
	private File whitelist;
	protected DefaultLogger defaultLogger;
	protected PhpUnitProgressLogger progressLogger;
	protected ConcurrentHashMap<String, Vector<String>> results;

	private int millies;
	private double mbytes;

	private int cntTests;
	private int cntErrors;
	private int cntAssertions;
	private int cntFailures;
	private int cntWarnings;
	private int cntIncomplete;
	private int cntSkipped;
	private int cntRisky;

	public Unit() {
		super(Constants.TOOL_PHPUNIT);
		FilterChain chain = new FilterChain();
		chain.add(new OutputFilterReader(this));
		outputFilterChains.add(chain);
		results = new ConcurrentHashMap<String, Vector<String>>();
		results.put("counters", new Vector<String>());
		results.put("resources", new Vector<String>());
		millies = 0;
		mbytes = 0;
		cntTests = 0;
		cntErrors = 0;
		cntAssertions = 0;
		cntFailures = 0;
		cntWarnings = 0;
		cntIncomplete = 0;
		cntSkipped = 0;
		cntRisky = 0;
	}

	public void setSrcDir(final File dir) {
		super.setSrcDir(dir);
	}

	public void setLogDir(final File dir) {
		super.setDestDir(dir);
	}

	public void setBootstrap(final File file) {
		bootstrap = file;
	}

	public void setParallel(final boolean flag) {
		super.setParallel(flag);
	}

	public void setWhitelist(final File file) {
		whitelist = file;
	}

	public void setDir(final File dir) {
		super.setWorkDir(dir);
	}

	public void init() throws BuildException {
		super.init();
		useExtension("xdebug", true);
	}

	protected void process() throws BuildException {
		log(StringUtils.LINE_SEP);
		Project project = getProject();
		Vector<BuildListener> listeners = project.getBuildListeners();
		for (BuildListener listener : listeners) {
			if (listener instanceof DefaultLogger) {
				defaultLogger = (DefaultLogger) listener;
			}
		}
		progressLogger = new PhpUnitProgressLogger(defaultLogger);

		project.addBuildListener(progressLogger);
		project.removeBuildListener(defaultLogger);
		try {
			super.process();
		} finally {

			BuildEvent event = new BuildEvent(this);
			event.setMessage("#", Project.MSG_INFO);
			progressLogger.messageLogged(event);
			project.removeBuildListener(progressLogger);
			project.addBuildListener(defaultLogger);

		}
		processFailures(getFailures());
		processResources(results.get("resources"));
		processCounters(results.get("counters"));

	}

	protected void configureArguments(Commandline args) {

		// the following line does not see necessary
		// args.createArgument().setValue("--no-configuration");

		if (bootstrap != null) {
			args.createArgument().setValue("--bootstrap");
			args.createArgument().setFile(bootstrap);
		}
		
		File resultPrinter = resolveScript(Constants.SCRIPT_PHPUNITRESULTPRINTER);
		
		// for development purposes only
		Map<String, String> system =Execute.getEnvironmentVariables();		
		if(system.get("SCRIPT_PHPUNITRESULTPRINTER")!=null) {
			resultPrinter = new File(system.get("SCRIPT_PHPUNITRESULTPRINTER"));
		}
		args.createArgument().setValue("--include-path="+resultPrinter.getParent());
		args.createArgument().setValue("--printer="+resultPrinter.getName().replace(".php", "")); 


		/*
		 * File log; File logJunit; if (parallel) { File tmpdir = new
		 * File(System.getProperty("java.io.tmpdir")); // log = new File(tmpdir,
		 * "CV_" + executor.getGuid()); log = new File(destDir, "clover" +
		 * executor.getGuid() + ".xml"); logJunit = new File(destDir, "junit" +
		 * executor.getGuid() + ".xml"); } else { log = new File(destDir,
		 * "clover" + executor.getGuid() + ".xml"); // log = new File(destDir,
		 * "clover.xml"); logJunit = new File(destDir, "junit" +
		 * executor.getGuid() + ".xml");
		 * 
		 * }
		 * 
		 * args.createArgument().setValue("--coverage-clover");
		 * args.createArgument().setFile(log);
		 * args.createArgument().setValue("--log-junit");
		 * args.createArgument().setFile(logJunit);
		 * 
		 * args.createArgument().setValue("--whitelist");
		 * args.createArgument().setFile(whitelist);
		 */
	}

	protected void configureEnvironment(Environment env, Executor executor) {
		super.configureEnvironment(env, executor);
//		Variable testToken = new Variable();
		Variable xdebugConfig = new Variable();

		//testToken.setKey("TEST_TOKEN");
		// testToken.setValue(String.valueOf(executor.getGuid()));
		// env.addVariable(testToken);

		xdebugConfig.setKey("XDEBUG_CONFIG");
		xdebugConfig.setValue("remote_enable=1 idekey=ECLIPSE_DBGP");
		env.addVariable(xdebugConfig);

	}

	protected void validateParameters() throws BuildException {

	}

	private void processFailures(Vector<String> failures) {
		if (failures.isEmpty()) {
			return;
		}
		Map<String, Vector<String>> failuresMap = new TreeMap<String, Vector<String>>();
		failuresMap.put("error", new Vector<String>());
		failuresMap.put("failure", new Vector<String>());

		for (String failure : failures) {
			String[] entries = failure.split(StringUtils.LINE_SEP);
			for (String entry : entries) {
				String[] parts = entry.split("\\|");
				String failureType = parts[0];
				String description = parts[1];
				if (!failuresMap.containsKey(failureType)) {
					String t = "";
					String x = t;
				}
				int count = failuresMap.get(failureType).size();
				description = String.format("%d ) %s#", count + 1, description);
				failuresMap.get(failureType).add(description);
			}
		}
		failures.clear();
		for (String failureType : failuresMap.keySet()) {
			int count = failuresMap.get(failureType).size();
			if (count > 0) {
				String verb = count == 1 ? "was" : "were";
				String suffix = count == 1 ? "" : "s";
				String header = String.format("There %s %d %s%s:", verb, count, failureType, suffix);
				failures.add(StringUtils.LINE_SEP);
				failures.add("------");
				failures.add(header);
				for (String failure : failuresMap.get(failureType)) {
					failures.add(StringUtils.LINE_SEP);
					failure = failure.replace("#", StringUtils.LINE_SEP);
					failures.add(failure);
				}
			}

		}

	}

	/**
	 * Processes the resource usage for each executed test.
	 * 
	 * Each individual executor report the phpunit resource usage as one entry
	 * in the resources vector. This method iterates of these entries and add up
	 * the values making them available for further processing.
	 * 
	 * @param resources
	 */
	private void processResources(Vector<String> resources) {
		for (String entry : resources) {
			String[] pairs = entry.split(";");
			for (String pair : pairs) {
				String[] parts = pair.split(":");

				switch (parts[0].toLowerCase()) {
				case "t":
					int count = Integer.valueOf(parts[1]);
					millies = millies + count;
					break;
				case "m":
					double value = Double.valueOf(parts[1]);
					mbytes = Math.max(mbytes, value);
					break;
				default:
				}
			}
		}
		displayResources();
	}

	private void displayResources() {
		String usage = StringUtils.LINE_SEP;
		try {
			usage += String.format("Time: %d ms, Memory: %sMB", millies, mbytes);
		} catch (IllegalFormatException e) {
			usage += "Failed to format usage, due to " + e.getMessage();
		}
		this.log(usage);
	}

	/**
	 * Processes the counters for each test
	 * 
	 * Each individual executor reports the phpunit counters as one entry in the
	 * counters vector. This method iterates over all entries and add up the
	 * values of each individual counter making them available for further
	 * processing.
	 * 
	 * @param counters
	 */
	private void processCounters(Vector<String> counters) {
		for (String entry : counters) {
			String[] pairs = entry.split(";");
			for (String pair : pairs) {
				String[] parts = pair.split(":");
				int count = Integer.valueOf(parts[1]);
				switch (parts[0].toLowerCase()) {
				case "a":
					cntAssertions = cntAssertions + count;
					break;
				case "e":
					cntErrors = cntErrors + count;
					break;
				case "f":
					cntFailures = cntFailures + count;
					break;
				case "i":
					cntIncomplete = cntIncomplete + count;
					break;
				case "r":
					cntRisky = cntRisky + count;
				case "s":
					cntSkipped = cntSkipped + count;
					break;
				case "t":
					cntTests = cntTests + count;
					break;
				case "w":
					cntWarnings = cntWarnings + count;
					break;
				default:
				}
			}
		}
		displayCounters();
	}

	private void displayCounters() {
		String counters = StringUtils.LINE_SEP;
		counters += String.format("Tests: %d, Assertions: %d", cntTests, cntAssertions);
		if (cntErrors > 0) {
			counters += String.format(", Errors: %d", cntErrors);
		}
		if (cntFailures > 0) {
			counters += String.format(", Failures: %d", cntFailures);
		}
		if (cntWarnings > 0) {
			counters += String.format(", Warnings: %d", cntWarnings);
		}
		if (cntSkipped > 0) {
			counters += String.format(", Skipped: %d", cntSkipped);
		}
		if (cntIncomplete > 0) {
			counters += String.format(", Incomplete: %d", cntIncomplete);
		}
		if (cntRisky > 0) {
			counters += String.format(", Risky: %d", cntRisky);
		}
		this.log(counters);
	}

	static class OutputFilterReader extends FilterReader implements ChainableReader {

		private Unit owner;

		protected OutputFilterReader(Unit task) {
			super(new StringReader(""));
			owner = task;
			FileUtils.close(this);
		}

		public Reader chain(Reader reader) {
			return new ExecutorOutputReader(reader, owner);
		}
	}

	static class ExecutorOutputReader extends FilterReader {

		static private int PHPUNIT_LOGO_LENGTH = 50;
		private Unit owner;

		int startPosition;
		boolean progressStarted;
		boolean progressStopped;

		String resources;
		String results;
		boolean readResources;
		boolean readCounters;

		int iterations = 0;

		protected ExecutorOutputReader(Reader reader, Unit task) {
			super(reader);
			owner = task;

			startPosition = PHPUNIT_LOGO_LENGTH;
			progressStarted = false;
			progressStopped = false;

			resources = "";
			results = "";
			readResources = false;
			readCounters = false;

		}

		public int read(char cbuf[], int off, int len) throws IOException {
			int cc = 0;
			while (cc == 0) {
				cc = in.read(cbuf, off, len);
				if (cc == -1) {
					owner.results.get("resources").add(resources);
					owner.results.get("counters").add(results);
					return -1;
				}

				// skip as much of the php version information as posible
				// setting the start position to PHP_LOGO_LENGTH as much
				// of the processed output in terms of chars is the logo;
				// after the initial iteration reset the startPosition to
				// zero so any char batches are fully processed

				startPosition = Math.min(startPosition, cc);

				for (int i = startPosition; i < cc; i++) {
					iterations++;
					// wait for the first newline char
					if (!progressStarted) {
						if (cbuf[i] == 10) {
							progressStarted = true;
						}
						continue;
					}

					if (cbuf[i] == 10) {
						continue; // ignore new lines
					}

					if (cbuf[i] == 35) {
						progressStopped = true;
						readResources = !readResources;
						readCounters = readResources ? false : true;

						continue;
					}
					if (progressStarted && !progressStopped) {
						synchronized (owner) {
							owner.log(String.format("%s", cbuf[i]));
							continue;
						}
					}
					// read the test resource usage
					if (readResources) {
						resources = resources + cbuf[i];
					}
					// read the test summary (tests,errors,warnings ...)
					if (readCounters) {
						results = results + cbuf[i];
					}

				}

				startPosition = 0;
				cc = 0;

			}
			return -1;
		}

	}
}
