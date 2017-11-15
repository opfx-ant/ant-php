package org.opfx.ant.php.task;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.opfx.ant.php.AbstractTask;
import org.opfx.ant.php.Constants;

//TODO : only overwrite the destfile if the sources are newer then the phar
//TODO : add excludes/includes
//TODO : deal with empty basedir or an empty fileset (due to excludes/includes usage) but maybe do it in the task
//TODO : add ability to specify the hash algorithm
//TODO : add attribute to control whether all files should be included in phar or not
public class Phar extends AbstractTask {

	/**
	 * The alias for the phar.
	 */
	private String alias;

	/**
	 * The file that will be used as phar's stub
	 */
	private File stub;

	private Vector<Variable> vars;

	public Phar() {
		super(Constants.TOOL_PHPAB);
		vars = new Vector<Variable>();
	}

	public Variable createVar() {
		Variable var = new Variable();
		vars.add(var);
		return var;
	}

	public void setAlias(final String alias) {
		this.alias = alias;
	}

	public void setBaseDir(final File dir) {
		setSrcDir(dir);
	}

	public void setStub(final File file) {
		stub = file;
	}

	@Override
	public void setDestFile(final File file) {
		super.setDestFile(file);
	}

	@Override
	public void init() throws BuildException {
		super.init();
		useExtension("fileinfo");
		define("phar.readonly", "0");
		setIncludes("*.*");
	}

	@Override
	protected void validateParameters() throws BuildException {
		if (srcDir == null) {
			throw new BuildException("'basedir' attribute must be set.");
		}

		if (!srcDir.exists()) {
			throw new BuildException("The directory '" + srcDir + "' specified by 'basedir' attribute does not exist.");
		}

		if (!srcDir.isDirectory()) {
			throw new BuildException("The '" + srcDir + "' specified by the 'basedir' attribute is not a directory.");
		}

		if (destFile == null) {
			throw new BuildException("'destfile' attribute must be set.");
		}

		for (Variable var : vars) {
			var.validateParameters();
		}
		super.validateParameters();
	}

	@Override
	protected void configureArguments(Commandline args) {

		if (stub == null) {
			File autoload = new File(srcDir, "autoload.php");
			if (autoload.exists()) {
				stub = autoload;
				setExcludes("**/autoload.php");
			}
		}
		if (stub == null) {
			File autoload = new File(srcDir, "stub.php");
			if (autoload.exists()) {
				stub = autoload;
				setExcludes("**/stub.php");
			}

		}
		log(String.format("Using '%s' as stub.", stub.toString()));
		args.createArgument().setValue("--phar");

		if (includes != null) {
			args.createArgument().setValue("--include");
			includes = includes.replace("/", File.separator);
			args.createArgument().setValue(includes);
		}

		if (excludes != null) {
			args.createArgument().setValue("--exclude");
			excludes = excludes.replace("/", File.separator);
			args.createArgument().setValue(excludes);
		}

		args.createArgument().setValue("--hash");
		args.createArgument().setValue("SHA-1");
		args.createArgument().setValue("--output");
		args.createArgument().setFile(destFile);
		if (alias == null) {
			alias = destFile.getName().toLowerCase();
			alias = alias.replace(".phar", "");
		}

		args.createArgument().setValue("--alias");
		args.createArgument().setValue(alias);

		if (stub != null) {
			args.createArgument().setValue("--template");
			args.createArgument().setFile(stub);
		}

		for (Variable var : vars) {
			args.createArgument().setValue("--var");
			args.createArgument().setValue(var.getName() + "=" + var.getValue());
		}

	}

	public static class Variable {
		private String name;
		private String value;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(final String value) {
			this.value = value;
		}

		public void validateParameters() throws BuildException {
			if (name == null) {
				throw new BuildException("'name' attribute must be set on each var element.");
			}
			if (name.isEmpty()) {
				throw new BuildException("'name' attribute of a var element cannot be an empty string.");
			}
			if (value == null) {
				throw new BuildException("'value' attribute must be set on each var element.");
			}
		}
	}

}
