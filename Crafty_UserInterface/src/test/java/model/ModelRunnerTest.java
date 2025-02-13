package model;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dataLoader.CellsLoaderTest;
import de.cesr.crafty.model.Cell;
import de.cesr.crafty.model.Aft;
import de.cesr.crafty.model.ModelRunner;

class ModelRunnerTest {

	CellsLoaderTest loader = new CellsLoaderTest();
	ModelRunner R;

	// @BeforeEach
	void setUp() throws Exception {
		loader.setUp();
		R = new ModelRunner();
//		R.calculeSystemSupply();
	}

//	@Test
	void justatest() {
		Map<String, Double> capitals = new HashMap<>();
		capitals.put("Capital1", 10.0);
		capitals.put("Capital2", 5.0);

		Map<String, Double> manager = new HashMap<>();
		manager.put("Capital1_serviceA", 0.5);
		manager.put("Capital2_serviceA", 1.);

		calculateProductivity(capitals, manager, "serviceA");
	}

	void calculateProductivity(Map<String, Double> capitals, Map<String, Double> a, String serviceName) {

		DoubleStream sam = capitals.entrySet().stream().mapToDouble(e -> (e.getValue() + a.get(e.getKey() + "_" + serviceName)));
		
		double s = sam.reduce(0, (x, y) -> x + y);
		System.out.println("--"+s);
		sam.forEach(e->{System.out.println(e);});
	}

//	 @Test
	void testCalculeSystemSupply() {
		// Setup
		// Assuming Cell is a class with methods getOwner() and prodactivity(String
		// owner, String service)
		Cell cell1 = mock(Cell.class);
		Cell cell2 = mock(Cell.class);
		Aft Owner1 = mock(Aft.class);
		Aft Owner2 = mock(Aft.class);

		Mockito.when(cell1.getOwner()).thenReturn(Owner1);
		Mockito.when(cell2.getOwner()).thenReturn(Owner2);

		String service1 = "Service1";
		String service2 = "Service2";
//		Mockito.when(cell1.productivity(Owner1, service1)).thenReturn(10.0);
//		Mockito.when(cell1.productivity(Owner1, service2)).thenReturn(20.0);
//		Mockito.when(cell2.productivity(Owner2, service1)).thenReturn(15.0);
//		Mockito.when(cell2.productivity(Owner2, service2)).thenReturn(25.0);

		// Mockito.when(CellsSet.getCellsSet()).thenReturn(new
		// HashSet<>(Arrays.asList(cell1, cell2)));
		// Mockito.when(CellsSet.getServicesNames()).thenReturn(Arrays.asList(service1,
		// service2));

		// Act
	//	R.calculeSystemSupply();

		// Assert
		Map<String, Double> expectedSupply = new HashMap<>();
		expectedSupply.put(service1, 25.0); // 10 from cell1 + 15 from cell2
		expectedSupply.put(service2, 45.0); // 20 from cell1 + 25 from cell2

//	        Mockito.assertThat(R.supply).containsExactlyInAnyOrderEntriesOf(expectedSupply);
	}

}
