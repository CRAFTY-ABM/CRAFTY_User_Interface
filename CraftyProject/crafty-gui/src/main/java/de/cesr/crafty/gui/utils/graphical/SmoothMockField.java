package de.cesr.crafty.gui.utils.graphical;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import de.cesr.crafty.core.dataLoader.land.CellsLoader;
import de.cesr.crafty.core.utils.file.CsvTools;
import de.cesr.crafty.gui.canvasFx.CellsCanvas;

/**
 * MockField – produces smooth, multi-centred values in [0,1].
 *
 * Call {@link #value(int, int, int)} for any grid-cell (x,y) and time-index t.
 * The pattern is entirely deterministic for a given constructor seed, so you
 * always get the same map back.
 *
 * How it works ------------ value(x,y,t) = Σ_k A_k(t) · exp(-|p-c_k|² /
 * (2σ_k²))
 *
 * • k = Gaussian “blob” index (there are <centres> of them) • c_k = centre of
 * blob k (fixed at construction) • σ_k = spatial width (randomised per blob) •
 * A_k = amplitude that wiggles with time (smooth ∈ [0.2,1.0])
 *
 * Tune <centres>, <spaceScale> & <timeScale> until the field looks right.
 */
public final class SmoothMockField {

	private final int width, height, centres;
	private final double[] cx, cy, sigma, phase; // blob parameters
	private final double spaceScale, timeScale; // global tuning knobs

	/**
	 * @param width      lattice width (pixels)
	 * @param height     lattice height (pixels)
	 * @param centres    how many blobs to sprinkle (≥ 1)
	 * @param spaceScale typical blob radius (pixels, e.g. 150)
	 * @param timeScale  speed of temporal change (radians per t-step, e.g. 0.05)
	 */
	public SmoothMockField(int width, int height, int centres, double spaceScale, double timeScale) {

		if (centres < 1)
			throw new IllegalArgumentException("need at least 1 centre");
		this.width = width;
		this.height = height;
		this.centres = centres;
		this.spaceScale = spaceScale;
		this.timeScale = timeScale;

		cx = new double[centres];
		cy = new double[centres];
		sigma = new double[centres];
		phase = new double[centres];

		Random rnd = new Random();

		for (int i = 0; i < centres; i++) {
			int base = 20;
			if (i < base) {
				cx[i] = rnd.nextDouble() * width;
				cy[i] = rnd.nextDouble() * height;
				sigma[i] = (0.30 + 0.70 * rnd.nextDouble()) * spaceScale; // 0.3–1.0× scale
				phase[i] = rnd.nextDouble() * 2 * Math.PI; // start phase
			} else {
				findNextPoint(i, i);
			}
		}

	}

	private void findNextPoint(int start, int j) {
		double x = new Random().nextDouble() * width;
		double y = new Random().nextDouble() * height;
		cx[j] = cx[0];
		cy[j] = cy[0];
		for (int i = 0; i < start; i++) {
			double d = Math.sqrt((cx[i] - x) * (cx[i] - x) + (cy[i] - y) * (cy[i] - y));
			if (d < 70) {
				cx[j] = x;
				cy[j] = y;
				break;
			}
		}
		sigma[j] = (0.30 + 0.70 * new Random().nextDouble()) * spaceScale; // 0.3–1.0× scale
		phase[j] = new Random().nextDouble() * 2 * Math.PI; // start phase

	}

	/** Smooth deterministic value in **[0,1]** for a grid-cell and time-step. */
	public double value(int x, int y, int t) {
		double sum = 0.0;

		for (int i = 0; i < centres; i++) {
			double dx = x - cx[i];
			double dy = y - cy[i];
			double r2 = dx * dx + dy * dy;

			// blob amplitude wiggles gently with time
			double a = Math.sin(phase[i] + t * timeScale); // 0.2 … 1.0
			sum += a * Math.exp(-r2 / (2 * sigma[i] * sigma[i]));
		}
		// squash the open-ended Gaussian sum neatly into [0,1]
		double r = 0.5 * (1 + Math.tanh(sum));// 0.5 * centres
		r = (r > 0.53 ? r - r * Math.random() * 0.1 : 0);

		return r;
	}

	static String[][] csv;

	public void color(int step) {
		List<Double> set = new ArrayList<>();
		csv = new String[CellsLoader.hashCell.size() + 1][2];
		AtomicInteger i = new AtomicInteger(1);
		csv[0][0] = "ID,X,Y";
		csv[0][1] = "ExtConifer";
		CellsLoader.hashCell.values().forEach(c -> {
			double v = value(c.getX(), c.getY(), step);
			set.add(v);
			CellsCanvas.ColorP(c, ColorsTools.getColorForValue(v));
			csv[i.get()][0] = c.getID() + "," + c.getX() + "," + c.getY();
			csv[i.getAndIncrement()][1] = String.valueOf(v);
		});

	}

	public static void writeMockData() {
		for (int i = 2020; i <= 2100; i++) {
			CsvTools.writeCSVfile(csv, Paths.get(
					"C:\\Users\\byari-m\\Desktop\\CRAFTY_DATA\\CRAFTY-EU-1km_upscaled_16\\worlds\\shocksMap\\default_cellsShocks\\"
							+ i + ".csv"));
		}
		;
	}
}
