package dataLoader;

import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.cesr.crafty.dataLoader.CellsLoader;
import de.cesr.crafty.model.Cell;
import de.cesr.crafty.model.CellsSet;
import de.cesr.crafty.utils.file.CsvTools;
import de.cesr.crafty.utils.graphical.Tools;

public class CellsLoaderTest {

	AFTsLoaderTest afTets = new AFTsLoaderTest();
	public CellsLoader M = new CellsLoader();
	Cell c = mock(Cell.class);

//	@BeforeEach
	public void setUp() throws Exception {
//		PathsLoader.initialisation("C:\\Users\\byari-m\\Documents\\Data\\data_EUpaper_nocsv");
//		M.loadCapitalsAndServiceList();
//		afTets.setUp();
//		M.loadMap();
//		CellsSet.setCellsSet(M);

	}

//	@Test
//	void testFindBookById() {
//
//		Manager ow = mock(Manager.class);
//		when(c.getOwner()).thenReturn(ow);// Mock the behavior of c
//	//	verify(c).getOwner(); 
//		System.out.println(c + "-->" + c.getOwner());
//		verify(c).getOwner(); 
//		when(c.getOwner().getColor()).thenReturn(Color.RED);
////		verify(c).getOwner(); 
//		System.out.println("-->" + c.getOwner().getColor());
//	//	verify(c).getOwner(); 
//////		// Exercise
//		Color result = c.getOwner().getColor();
////		verify(c).getOwner(); 
//////		// Verify
//		assertEquals(Color.RED, result); // Assert that the result is what we mocked
////		// Optionally verify that c.getOwner was called
//		
//		
//		
//	}

	double[] sumCSVboxs(String path, int firstYear, int lastYear) {
		double[] SUM = new double[lastYear - firstYear];
		for (int k = 0; k < SUM.length; k++) {
			Path p = Paths.get(path + (firstYear + k) + ".csv");
			String[][] svct = CsvTools.csvReader(p);
			double sum = 0;
			for (int i = 0; i < svct.length; i++) {
				for (int j = 0; j < svct[0].length; j++) {
					sum = sum + Tools.sToD(svct[i][j]);
				}
			}
			SUM[k] = sum;
		}
		return SUM;
	}

//	@Test
	void test() {
		// afTets.test();
		// fail("Not yet implemented");
		double[] sum = sumCSVboxs(
				"C:\\Users\\byari-m\\Desktop\\CRAFTY-Cobra-Code-compareson\\data_for_UI\\worlds\\capitals\\RCP4_5-SSP5\\",
				2020, 2030);
		System.out.println(Arrays.toString(sum));

	}

}
