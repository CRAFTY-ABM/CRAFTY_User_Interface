package de.cesr.crafty.core.crafty;

import de.cesr.crafty.core.modelRunner.AbstractModelRunner;

public interface ModelState {
	 /** give the component a back-pointer to the runner */
    void setup(AbstractModelRunner modelRunner);

    /** register myself with the scheduler */
    void toSchedule();

    /** called every tick */
    void step();
}
