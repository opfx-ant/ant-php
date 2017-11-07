package org.opfx.ant.php.type;

import java.util.Enumeration;

import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.FileSelector;

public class PreserveInTarget extends AbstractFileSet {
	private boolean preserveEmptyDirs;

	public PreserveInTarget() {
		super();
		this.appendExcludes(new String[] { "**/*.php" });
	}

	// /////////////////////////////////////
	// ATTRIBUTES

	public void setPreserveEmptyDirs(final boolean flag) {
		preserveEmptyDirs = flag;
	}

	// /////////////////////////////////////
	// PROPERTIES

	public boolean getPreserveEmptyDirs() {
		return preserveEmptyDirs;
	}

	public FileSet toFileSet(boolean withPatterns) {
		FileSet fs = new FileSet();
		fs.setCaseSensitive(isCaseSensitive());
		fs.setFollowSymlinks(isFollowSymlinks());
		fs.setMaxLevelsOfSymlinks(getMaxLevelsOfSymlinks());
		fs.setProject(getProject());

		if (withPatterns) {
			PatternSet ps = mergePatterns(getProject());
			fs.appendIncludes(ps.getIncludePatterns(getProject()));
			fs.appendExcludes(ps.getExcludePatterns(getProject()));
			for (Enumeration<FileSelector> e = selectorElements(); e.hasMoreElements();) {
				fs.appendSelector((FileSelector) e.nextElement());
			}
			fs.setDefaultexcludes(getDefaultexcludes());
		}
		return fs;
	}

}
