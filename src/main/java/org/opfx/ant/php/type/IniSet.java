package org.opfx.ant.php.type;

import java.util.Iterator;
import java.util.Vector;

import org.apache.tools.ant.types.DataType;

public class IniSet extends DataType implements Iterable<IniSet.Entry> {
	// TODO > P3 : IniSet : accept references to other inisets
	// TODO > P3 : IniSet : accept propertysetrefs

	private String name;
	private Vector<Entry> entries;

	// /////////////////////////////////////
	// CONSTRUCTION
	public IniSet() {
		super();
		this.entries = new Vector<Entry>();
	}

	// /////////////////////////////////////
	// ATTRIBUTES
	public void setName(final String name) {
		this.name = name;
	}

	// /////////////////////////////////////
	// ELEMENTS
	public Entry createEntry() {
		Entry entry = new Entry();
		entry.setPrefix(this.name);
		entries.add(entry);
		return entry;
	}

	// /////////////////////////////////////
	// ITERATOR INTERFACE IMPLEMENTATION
	public synchronized Iterator<Entry> iterator() {
		return entries.iterator();
	}
	// /////////////////////////////////////
	// IniSet.Entry CLASS///////////////////
	// /////////////////////////////////////

	public static class Entry {
		private String prefix;
		private String name;
		private String value;

		// /////////////////////////////////////
		// CONSTRUCTION
		public Entry() {
			super();
			prefix = "";
		}

		// /////////////////////////////////////
		// PROPERTIES
		public Entry setPrefix(final String prefix) {
			if (prefix == null || prefix.length() == 0) {
				return this;
			}
			this.prefix = prefix + ".";
			return this;
		}

		public Entry setName(final String name) {
			this.name = name;
			return this;

		}

		public String getName() {
			return prefix + name;
		}

		public Entry setValue(final String value) {
			this.value = value;
			return this;
		}

		public String getValue() {
			return value;
		}
	}
}
