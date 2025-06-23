package de.cesr.crafty.core.utils.general;

//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.GridCoverageFactory;
//import org.geotools.coverage.grid.io.AbstractGridFormat;
//import org.geotools.geometry.Envelope2D;
//import org.geotools.gce.geotiff.GeoTiffFormat;
//import org.geotools.gce.geotiff.GeoTiffWriteParams;
//import org.geotools.gce.geotiff.GeoTiffWriter;
//import org.geotools.referencing.crs.DefaultGeographicCRS;
//import org.opengis.parameter.GeneralParameterValue;
//import org.opengis.parameter.ParameterValueGroup;
//
//import de.cesr.crafty.core.cli.ConfigLoader;
//import de.cesr.crafty.core.dataLoader.CellsLoader;
//import de.cesr.crafty.core.dataLoader.ProjectLoader;
//import de.cesr.crafty.core.output.Listener;
//import de.cesr.crafty.core.utils.file.PathTools;
//
//import javax.imageio.spi.IIORegistry;
//import javax.imageio.spi.ImageWriterSpi;
//import java.awt.Color;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.Iterator;

public class GeoTiffExample {

//	public static void geoTiffWriter() {
//		// Ensure headless mode for HPC
//		System.setProperty("java.awt.headless", "true");
//		deregisterBrokenImageWriters();
//
//		if (ConfigLoader.config.generate_map_plots_tif) {
//			if (Listener.yearsMapExporting.contains(ProjectLoader.getCurrentYear())) {
//				// Create output directory path
//				String path = PathTools
//						.makeDirectory(ConfigLoader.config.output_folder_name + File.separator + "MapsPlots");
//
//				// Create the ARGB image
//				BufferedImage image = new BufferedImage(CellsLoader.maxX, CellsLoader.maxY,
//						BufferedImage.TYPE_INT_ARGB);
//
//				// Fill the image from cells
//				CellsLoader.hashCell.values().forEach(c -> {
//					if (c.getOwner() != null)
//						image.setRGB(c.getX(), c.getY(), Color.decode(c.getOwner().getColor()).getRGB());
//				});
//
//				// Define geospatial envelope
//				Envelope2D envelope = new Envelope2D(DefaultGeographicCRS.WGS84, 10.0, 20.0, 5.0, 5.0);
//
//				// Create coverage
//				GridCoverageFactory factory = new GridCoverageFactory();
//				GridCoverage2D coverage = factory.create("CRAFTY_Output", image, envelope);
//
//				// GeoTIFF output file
//				File outputTiff = new File(path + File.separator + "map_" + ProjectLoader.getCurrentYear() + ".tif");
//
//				// Ensure output folder exists
//				File parentDir = outputTiff.getParentFile();
//				if (!parentDir.exists()) {
//					parentDir.mkdirs();
//				}
//
//				try {
//					// Configure write params
//					GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
//					writeParams.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
//					writeParams.setCompressionType("LZW"); // Use safe, non-native compression
//
//					// Create the parameter group
//					GeoTiffFormat format = new GeoTiffFormat();
//					ParameterValueGroup params = format.getWriteParameters();
//					params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
//							.setValue(writeParams);
//
//					// Write the GeoTIFF
//					GeoTiffWriter writer = new GeoTiffWriter(outputTiff);
//					writer.write(coverage, params.values().toArray(new GeneralParameterValue[0]));
//					writer.dispose();
//
//					System.out.println("GeoTIFF written to: " + outputTiff.getAbsolutePath());
//
//				} catch (IOException e) {
//					System.err.println("Failed to write GeoTIFF: " + e.getMessage());
//					e.printStackTrace();
//				}
//			}
//		}
//	}
//
//	/**
//	 * Deregisters problematic native image writers (e.g., CLibJPEGImageWriterSpi).
//	 * Prevents crashes in headless environments like HPC clusters.
//	 */
//	private static void deregisterBrokenImageWriters() {
//		IIORegistry registry = IIORegistry.getDefaultInstance();
//		Iterator<ImageWriterSpi> it = registry.getServiceProviders(ImageWriterSpi.class, true);
//		while (it.hasNext()) {
//			ImageWriterSpi writer = it.next();
//			String name = writer.getClass().getName();
//			if (name.contains("CLibJPEGImageWriterSpi")) {
//				registry.deregisterServiceProvider(writer);
//				System.out.println("Deregistered native ImageWriter: " + name);
//			}
//		}
//	}

}
