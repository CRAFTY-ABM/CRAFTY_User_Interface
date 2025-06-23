package de.cesr.crafty.core.updaters;

import de.cesr.crafty.core.crafty.ModelState;
import de.cesr.crafty.core.modelRunner.AbstractModelRunner;

/**
 * @author Mohamed Byari
 *
 */
public abstract class AbstractUpdater implements ModelState{
    /** visible to subclasses, hidden from the outside  */
    protected AbstractModelRunner modelRunner;

    @Override
    public void setup(AbstractModelRunner modelRunner) {
        this.modelRunner = modelRunner;          
    }
}
