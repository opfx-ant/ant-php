package org.opfx.ant.php.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.opfx.ant.php.AbstractTask;
import org.opfx.ant.php.type.PreserveInTarget;

public class Lint extends AbstractTask {

	private Sync sync;

	private PreserveInTarget preserveInTarget;

	/**
	 * Add a filterset.
	 * 
	 * @return a filter set object.
	 */
	public FilterSet createFilterSet() {
		return sync.createFilterSet();
	}

	/**
	 * Sets the directory containing the files that need to be linted.
	 */
	@Override
	public void setSrcDir(final File dir) {
		super.setSrcDir(dir);
	}

	/**
	 * Sets the directory where the linted files will be stored.
	 * 
	 * @param dir
	 */
	@Override
	public void setDestDir(final File dir) {
		super.setDestDir(dir);
	}

	/**
	 * Comma- or space-separated list of files that must be excluded; no files
	 * are excluded when omitted. Wildcard patterns may be used to specify the
	 * list of files.
	 */
	public void setExcludes(final String pattern) {
		super.setExcludes(pattern);
	}

	/**
	 * Comma- or space-separated list of files that must be included; all .php
	 * files are included when omitted. Wildcard patterns may be used to specify
	 * the list of files.
	 */
	public void setIncludes(final String pattern) {
		super.setIncludes(pattern);
	}

	/**
	 * Adds the given fileset to set of sources that will be linted.
	 * 
	 * @param fs
	 */
	public void addFileSet(final FileSet fs) {
		super.addFileSet(fs);
	}

	/**
	 * Set whether to copy empty directories in the destDir.
	 * 
	 * @param includeEmpty
	 *            if true copy empty directories. Default is true.
	 */
	public void setIncludeEmptyDirs(final boolean flag) {
		sync.setIncludeEmptyDirs(flag);
	}

	public void addPreserveInTarget(PreserveInTarget fs) {
		if (preserveInTarget != null) {
			throw new BuildException("Lint only accepts one preserveintarget element.");
		}
		preserveInTarget = fs;
	}

	protected PreserveInTarget getPreserveInTarget() {
		if (preserveInTarget == null) {
			preserveInTarget = new PreserveInTarget();
		}
		return preserveInTarget;
	}

	@Override
	public void init() throws BuildException {
		super.setParallel(true);
		super.init();
		sync = new Sync(this);
		sync.init();
		setIncludes("**/*.php");

	}

	@Override
	protected void process() throws BuildException {
		if (destDir != null) {
			sync.execute();
		} else {
			super.process();
		}
	}

	@Override
	protected void validateParameters() throws BuildException {
		if (srcDir == null && sources == null) {
			throw new BuildException("'srcdir' attribute must be set, or at least one fileset must be given.");
		}
		super.validateParameters();
	}

	@Override
	protected void configureOptions(Commandline opts) {
		opts.createArgument().setValue("-l");
	}

	protected void complete(Executor executor) {
		if (executor.getResult() != 0) {
			sync.ignoreFile(executor.getFile());
		}
	}

	static class Sync extends Copy {
		private Lint owner;
		private Set<String> nonOrphans;
		private int outofdateFilesCount;

		public Sync(Lint lint) {
			super();
			owner = lint;
			nonOrphans = new HashSet<String>();
			outofdateFilesCount = 0;
		}

		/**
		 * 
		 * @param file
		 */
		protected void ignoreFile(File file) {
			if (fileCopyMap != null) {
				fileCopyMap.remove(file.getAbsolutePath());
			}
		}

		@Override
		public void init() throws BuildException {
			super.init();
			bindToOwner(owner);
		}

		@Override
		public void execute() throws BuildException {
			setVerbose(false);
			setIncludeEmptyDirs(false);
			setTodir(owner.destDir);

			if (owner.sources != null) {
				for (ResourceCollection rc : owner.sources) {
					add(rc);
				}
			}
			try {
				super.execute();
			} catch (BuildException e) {
				// log(e.getMessage(), Project.MSG_ERR);
				if (owner.failOnError) {
					throw e;
				}
			} finally {
				doRemoveOperations();
			}
		}

		@Override
		protected void doFileOperations() {
			try {
				if (outofdateFilesCount > 0) {
					log(String.format("Linting %d %s...", outofdateFilesCount,
							outofdateFilesCount > 1 ? "files" : "file"));
					owner.process(owner.getFiles());
				} else {
					log("Nothing to lint.");
				}
			} finally {
				super.doFileOperations();
			}
		}

		private void doRemoveOperations() {
			Set<File> preservedDirs = new LinkedHashSet<File>();
			removeOrphanFiles(preservedDirs);
			removeEmptyDirs(preservedDirs);
		}

		@Override
		protected void scan(File fromDir, File toDir, String[] files, String[] dirs) {
			super.scan(fromDir, toDir, files, dirs);
			for (int i = 0; i < files.length; i++) {
				nonOrphans.add(files[i]);
			}
			for (int i = 0; i < dirs.length; i++) {
				nonOrphans.add(dirs[i]);
			}
		}

		@Override
		protected void buildMap(File fromDir, File toDir, String[] names, FileNameMapper mapper,
				Hashtable<String, String[]> map) {
			String[] toCopy = null;
			if (forceOverwrite) {
				Vector<String> v = new Vector<String>();
				for (int i = 0; i < names.length; i++) {
					if (mapper.mapFileName(names[i]) != null) {
						v.addElement(names[i]);
					}
				}
				toCopy = new String[v.size()];
				v.copyInto(toCopy);
			} else {
				SourceFileScanner sfs = new SourceFileScanner(this);
				toCopy = sfs.restrict(names, fromDir, toDir, mapper, 0);

				// remove from the Lint's sources the files that are uptodate
				// only needed when overwrite is not enforced
				for (ResourceCollection rc : owner.sources) {
					DirectoryScanner ds = ((FileSet) rc).getDirectoryScanner(getProject());
					if (ds.getBasedir().equals(fromDir)) {
						String[] sources = ds.getIncludedFiles();
						List<String> toLint = Arrays.asList(toCopy);
						outofdateFilesCount = outofdateFilesCount + toLint.size();
						ArrayList<String> excludes = new ArrayList<String>();
						for (String source : sources) {
							if (!toLint.contains(source)) {
								excludes.add(source);
							}
						}
						((FileSet) rc).appendExcludes(excludes.toArray(new String[excludes.size()]));
					}
				}
			}

			for (int i = 0; i < toCopy.length; i++) {
				File src = new File(fromDir, toCopy[i]);
				String[] mappedFiles = mapper.mapFileName(toCopy[i]);

				map.put(src.getAbsolutePath(), new String[] { new File(toDir, mappedFiles[0]).getAbsolutePath() });

			}
		}

		private void removeOrphanFiles(Set<File> preservedDirs) {
			DirectoryScanner ds = null;

			String[] excludes = nonOrphans.toArray(new String[nonOrphans.size() + 1]);
			excludes[nonOrphans.size()] = "";

			FileSet fs = owner.getPreserveInTarget().toFileSet(false);
			fs.setDir(destDir);
			PatternSet ps = owner.getPreserveInTarget().mergePatterns(getProject());
			fs.appendExcludes(ps.getIncludePatterns(getProject()));
			fs.appendIncludes(ps.getExcludePatterns(getProject()));
			fs.setDefaultexcludes(!owner.getPreserveInTarget().getDefaultexcludes());

			FileSelector[] selectors = owner.getPreserveInTarget().getSelectors(getProject());
			if (selectors.length > 0) {
				NoneSelector ns = new NoneSelector();
				for (int i = 0; i < selectors.length; i++) {
					ns.appendSelector(selectors[i]);
				}
				fs.appendSelector(ns);
			}

			ds = fs.getDirectoryScanner(getProject());
			ds.addExcludes(excludes);
			ds.scan();
			String[] files = ds.getIncludedFiles();
			for (int i = 0; i < files.length; i++) {
				File file = new File(destDir, files[i]);
				log(String.format("Removing dangling file %s", file), Project.MSG_VERBOSE);
				file.delete();
			}

			String[] dirs = ds.getIncludedDirectories();
			for (int i = dirs.length - 1; i >= 0; i--) {
				File dir = new File(destDir, dirs[i]);
				String[] children = dir.list();
				if (children == null || children.length < 1) {
					log(String.format("Removing dangling dir %s", dir), Project.MSG_VERBOSE);
					dir.delete();
				}
			}

			if (owner.getPreserveInTarget().getPreserveEmptyDirs() != includeEmpty) {
				fs = owner.getPreserveInTarget().toFileSet(true);
				fs.setDir(destDir);
				String[] preserved = fs.getDirectoryScanner(getProject()).getIncludedDirectories();
				for (int i = preserved.length - 1; i >= 0; i--) {
					preservedDirs.add(new File(destDir, preserved[i]));
				}

			}

		}

		private void removeEmptyDirs(Set<File> preservedDirs) {
			if (!includeEmpty) {
				removeEmptyDirs(destDir, false, preservedDirs);
			} else {
				for (File dir : preservedDirs) {
					String[] children = dir.list();
					if (children == null || children.length == 0) {
						// TODO log only if verbose
						log(String.format("Removing empty directory %s", file), Project.MSG_VERBOSE);
						file.delete();

					}
				}
			}
		}

		/**
		 * Removes the empty directories from the directory specified by dir.
		 * 
		 * If removeDirIfEmpty is true and the specified directory is empty at
		 * the end of the operation, the directory will be also removed.
		 * 
		 * @param dir
		 * @param removeDirIfEmpty
		 * @param preservedDirs
		 * @return
		 */
		private int removeEmptyDirs(File dir, boolean removeDirIfEmpty, Set<File> preservedDirs) {
			int removedCount = 0;
			if (!dir.isDirectory()) {
				return removedCount;
			}
			File[] children = dir.listFiles();
			for (int i = 0; i < children.length; i++) {
				if (children[i].isDirectory()) {
					removedCount += removeEmptyDirs(children[i], true, preservedDirs);
				}
			}
			if (children.length > 0) {
				children = dir.listFiles();
			}
			if (children.length == 0 && removeDirIfEmpty && !preservedDirs.contains(dir)) {
				dir.delete();
				removedCount++;
			}
			return removedCount;
		}
	}
}
