package de.cesr.crafty.core.dataLoader;

import java.util.Map;

public enum CsvKind {

	CAPITALS {
		@Override
		void apply(String line, Map<String, Integer> idx) {
			CsvProcessors.associateCapitalsToCells(idx, line);
		}
	},

	SERVICES {
		@Override
		void apply(String line, Map<String, Integer> idx) {
			CsvProcessors.associateOutPutServicesToCells(idx, line);
		}
	},

	BASELINE {
		@Override
		void apply(String line, Map<String, Integer> idx) {
			CsvProcessors.createCells(idx, line);
		}
	};

	/** One behaviour per constant */
	abstract void apply(String line, Map<String, Integer> index);
}