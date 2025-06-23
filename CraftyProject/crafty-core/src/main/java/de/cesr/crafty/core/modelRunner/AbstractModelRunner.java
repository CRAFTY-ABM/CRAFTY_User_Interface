package de.cesr.crafty.core.modelRunner;

import java.util.ArrayList;
import java.util.List;

import de.cesr.crafty.core.crafty.ModelState;

public abstract class AbstractModelRunner {
	private final List<ModelState> scheduled = new ArrayList<>();

	public List<ModelState> getScheduled() {
		return scheduled;
	}

	public void scheduleRepeating(ModelState s) {
		scheduled.add(s);
	}

	public void step() {
		for (ModelState s : scheduled)
			s.step();
	}

	public void setup(AbstractModelRunner abstractModelRunner) {
		for (ModelState modelState : scheduled) {
			modelState.setup(this);
		}
	}
}
