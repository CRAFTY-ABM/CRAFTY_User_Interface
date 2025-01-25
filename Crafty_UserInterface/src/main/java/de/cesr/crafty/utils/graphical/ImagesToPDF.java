package de.cesr.crafty.utils.graphical;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;

public class ImagesToPDF {
	public static void createPDFWithImages(String folderPath, String outputPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

        try (PDDocument document = new PDDocument()) {
            for (File file : files) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromFile(file.getAbsolutePath(), document);
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // Adjust image size and position here
                float scale = 1f; // Adjust scaling factor to fit the image on the page as needed
                float imageWidth = pdImage.getWidth() * scale;
                float imageHeight = pdImage.getHeight() * scale;
                float startX = (PDRectangle.A4.getWidth() - imageWidth) / 2;
                float startY = (PDRectangle.A4.getHeight() - imageHeight) / 2;
                contentStream.drawImage(pdImage, startX, startY, imageWidth, imageHeight);
                
             // Add text below the image
                String imageTitle = "Title for " + file.getName(); // Modify or dynamically set the title as needed
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14); // Set font and size
                float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(imageTitle) / 1000 * 14;
                float textX = (PDRectangle.A4.getWidth() - textWidth) / 2; // Center the text
                float textY = startY - 20; // Position text below the image
                contentStream.newLineAtOffset(textX, textY);
                contentStream.showText(imageTitle);
                contentStream.endText();

                
                contentStream.close();
            }

            document.save(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	
	 public static void createPDFWithImages(String folderPath, String PDFname, int columns, int rows) {
	        File folder = new File(folderPath);
	        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

	        try (PDDocument document = new PDDocument()) {
	            PDPage page = new PDPage(PDRectangle.A4);
	            document.addPage(page);
	            PDPageContentStream contentStream = new PDPageContentStream(document, page);

	            float margin = 40; // Margin on each side
	            float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
	            float pageHeight = PDRectangle.A4.getHeight() - 2 * margin;
	            float imageWidth = pageWidth / columns;
	            float imageHeight = (pageHeight / rows) * 0.85f; // Reduce image height to make space for titles
	            
	            int imageCount = 0;
	         // Add the title below the image
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                String title0 = new File(folderPath).getName(); // Using the file name as the title
                float titleWidth0 = PDType1Font.HELVETICA_BOLD.getStringWidth(title0) / 1000 * 12;
                float titleX0 =  (imageWidth - titleWidth0) / 2; // Center the title under the image
                float titleY0 = PDRectangle.A4.getHeight() - margin; // Position the title 15 points below the image
                contentStream.newLineAtOffset(titleX0, titleY0);
                contentStream.showText(title0);
                contentStream.endText();

	            for (File file : files) {
	                if (imageCount >= columns * rows) {
	                    contentStream.close();
	                    page = new PDPage(PDRectangle.A4);
	                    document.addPage(page);
	                    contentStream = new PDPageContentStream(document, page);
	                    imageCount = 0;
	                }

	                int currentColumn = imageCount % columns;
	                int currentRow = imageCount / columns;

	                PDImageXObject pdImage = PDImageXObject.createFromFile(file.getAbsolutePath(), document);

	                float startX = margin + currentColumn * imageWidth;
	                float startY = PDRectangle.A4.getHeight() - margin - imageHeight - (currentRow * imageHeight);

	                // Scale image to fit
	                float scale = Math.min(imageWidth / pdImage.getWidth(), imageHeight / pdImage.getHeight());
	                float scaledWidth = pdImage.getWidth() * scale;
	                float scaledHeight = pdImage.getHeight() * scale;

	                // Draw the image centered in the grid cell
	                contentStream.drawImage(pdImage, startX + (imageWidth - scaledWidth) / 2, startY + (imageHeight - scaledHeight) / 2, scaledWidth, scaledHeight);

	                // Add the title below the image
	                contentStream.beginText();
	                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
	                String title = file.getName().substring(0, file.getName().lastIndexOf('.')); // Using the file name as the title
	                float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 12;
	                float titleX = startX + (imageWidth - titleWidth) / 2; // Center the title under the image
	                float titleY = startY ; // Position the title 15 points below the image
	                contentStream.newLineAtOffset(titleX, titleY);
	                contentStream.showText(title);
	                contentStream.endText();

	                imageCount++;
	            }

	            contentStream.close();
	            document.save(folderPath+File.separator + PDFname);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
}
