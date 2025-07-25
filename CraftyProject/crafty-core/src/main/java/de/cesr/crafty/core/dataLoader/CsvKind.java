package de.cesr.crafty.core.dataLoader;

import java.util.Map;

public enum CsvKind {

	CAPITALS {
		@Override
		void apply(String line, Map<String, Integer> index) {
			CsvProcessors.associateCapitalsToCells(index, line);
		}
	},
	SHOCKS {
		@Override
		void apply(String line, Map<String, Integer> index) {
			CsvProcessors.associateShockesToCells(index, line);
		}
	},

	SERVICES {
		@Override
		void apply(String line, Map<String, Integer> index) {
			CsvProcessors.associateOutPutServicesToCells(index, line);
		}
	},

	BASELINE {
		@Override
		void apply(String line, Map<String, Integer> index) {
			CsvProcessors.createCells(index, line);
		}
	};

	/** One behaviour per constant */
	abstract void apply(String line, Map<String, Integer> index);
}