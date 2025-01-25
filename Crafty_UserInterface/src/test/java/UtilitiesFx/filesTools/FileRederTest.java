package UtilitiesFx.filesTools;

import java.io.File;

import org.junit.jupiter.api.Test;

import de.cesr.crafty.main.FxMain;
import javafx.stage.DirectoryChooser;

class FileRederTest {

//	 @BeforeEach
	void setUp() throws Exception {
		System.out.println("s");
	}

	@Test
	void getFile() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select Project");
		chooser.setInitialDirectory(new File("C:\\Users\\byari-m\\Downloads"));
	}

	// @Test
//	void test() {
//		for (int i = 0; i < 10; i++) {
//			ReaderFile.processCSV(//"C:\\Users\\byari-m\\Downloads\\Baseline_map.csv");
//					null,"C:\\Users\\byari-m\\Documents\\Data\\data-DE\\worlds\\capitals\\Baseline\\DE_capitals_baseline_2020.csv","Capitals");
//		}
//		System.out.println("----------");
//	}

}
