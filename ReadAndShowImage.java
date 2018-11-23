package ro.imageprocess;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

public class ReadAndShowImage extends Application {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		launch(args);

	}

	public WritableImage loadImage() throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// lenna = C:\\Stuff\\workspace\\opencvTest\\lenna.jpg
		// roi = C:\\Stuff\\workspace\\opencvTest\\roi.jpg
		// hsv = C:\\Stuff\\workspace\\opencvTest\\hsv.jpg
		
		String file = "C:\\Stuff\\workspace\\opencvTest\\hsv.jpg";
		Mat image = Imgcodecs.imread(file, Imgcodecs.IMREAD_COLOR);

		// proprietati

		System.out.println(image.rows());
		System.out.println(image.cols());
		System.out.println(image.channels());
		// System.out.println(image.dump());

		// transf unei img color in una mata
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);

		// contrast
		Mat highContrast = new Mat();
		gray.convertTo(highContrast, -1, 1.2, 0);

		// luminozitate

		Mat highBrightnes = new Mat();
		gray.convertTo(highBrightnes, -1, 0.7, 75);

		// histograma
		Mat gray_hist = new Mat();
		Imgproc.cvtColor(image, gray_hist, Imgproc.COLOR_RGB2GRAY);
		Mat histograma = new Mat();

		List<Mat> images = new ArrayList<>();
		images.add(gray_hist);

		// set the number of bins at 256
		MatOfInt histSize = new MatOfInt(256);

		// only one channel
		MatOfInt channels = new MatOfInt(0);

		// set the ranges
		MatOfFloat histRange = new MatOfFloat(0, 256);

		Imgproc.calcHist(images.subList(0, 1), channels, new Mat(), histograma, histSize, histRange, false);

		int hist_w = 512; // width of the histo
		int hist_h = 512; // height of the histo
		// nr of bins in pixel
		int bin_w = (int) Math.round(hist_w / histSize.get(0, 0)[0]);

		Mat histImage = new Mat(hist_h, hist_w, CvType.CV_8UC3, new Scalar(0, 0, 0));

		// normalize the result to [0 , histImage.rows()]
		System.out.println("hist_image rows" + histImage.rows());
		Core.normalize(histograma, histograma, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
		System.out.println(bin_w);

		// effectively draw the histo
		for (int i = 1; i < histSize.get(0, 0)[0]; i++) {
			Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(histograma.get(i - 1, 0)[0])),
					new Point(bin_w * (i), hist_h - Math.round(histograma.get(i, 0)[0])), new Scalar(255, 255, 255), 2,
					8, 0);
		}

		// Binarizare

		Mat binary = new Mat();
		Imgproc.threshold(gray_hist, binary, 110, 255, Imgproc.THRESH_BINARY);

		// median
		Mat median = new Mat();
		Imgproc.medianBlur(gray_hist, median, 25);

		// filter
		Mat filter = new Mat();
		int kernelSize = 25;
		Mat kernel = new Mat(kernelSize, kernelSize, CvType.CV_32F) {
			{
				put(0, 0, 1.0 / 9);
				put(0, 1, 1.0 / 9);
				put(0, 2, 1.0 / 9);

				put(1, 0, 1.0 / 9);
				put(1, 1, 1.0 / 9);
				put(1, 2, 1.0 / 9);

				put(2, 0, 1.0 / 9);
				put(2, 1, 1.0 / 9);
				put(2, 2, 1.0 / 9);

			}
			
		};
		for (int i = 0; i < kernelSize; i++) {
			for (int j = 0; j < kernelSize; j++) {
				kernel.put(i, j, 1.0 / (kernelSize * kernelSize));
			}

		}

		Imgproc.filter2D(gray_hist, filter, -1, kernel);

		// edgefilter
		// sobel
		Mat sobel = new Mat();
		int kernelSizeSobel = 3;
		Mat kernelSobel = new Mat(kernelSizeSobel, kernelSizeSobel, CvType.CV_32F) {
			{
				put(0, 0, -1);
				put(0, 1, -2);
				put(0, 2, -1);

				put(1, 0, 0);
				put(1, 1, 0);
				put(1, 2, 0);

				put(2, 0, 1);
				put(2, 1, 2);
				put(2, 2, 1);

			}
		};
		// for(int i=0;i<kernelSize;i++) {
		// for(int j=0;j<kernelSize;j++) {
		// kernel.put(i, j, 1.0/(kernelSize*kernelSize));
		// }
		//
		// }

		// canny
		Mat canny = new Mat();
		Imgproc.Canny(gray_hist, canny, 200, 400);

		// Hough
		// change color of canny
		Mat houghColor = new Mat();
		Imgproc.cvtColor(canny, houghColor, Imgproc.COLOR_GRAY2BGR);

		// detect the hough lines (from canny)
		Mat lines = new Mat();
		
		// canny - source image
		// lines - array for saving the lines
		// 1 - pixel precision
		// Math.PI/180 = 1 degree - angular precision
		// 50 - min nr of point found in hough space (votes)
		// 200 - min length of a line (in pixels)
		// 20 - min lenght of a gap between lines (in pixels)
		
		
		Imgproc.HoughLinesP(canny, lines, 1, Math.PI/180, 20, 25, 10);

		// draw lines
		Point pt1 = new Point();
		Point pt2 = new Point();

		for (int i = 0; i < lines.rows(); i++) {

			double datavals[] = lines.get(i, 0);
			pt1 = new Point(datavals[0], datavals[1]); // x1 and y1
			pt2 = new Point(datavals[2], datavals[3]); // x2 and y2

			// draw each line

			Imgproc.line(houghColor, pt1, pt2, new Scalar(0, 0, 255), 3);
			

		}

		//Erode
		Mat erode = new Mat();
		int erosionsize = 5;
		Size ksize = new Size(erosionsize, erosionsize);
		
		//kernel for erosion and dilatation
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, ksize);
		Imgproc.erode(binary, erode, element);

		//Dilate
		Mat dilate = new Mat();
		Imgproc.dilate(binary, dilate, element);
		
		//Opening
		Mat opening = new Mat();
		Imgproc.dilate(erode, opening, element);
		
		//Closing
		Mat closing = new Mat();
		Imgproc.erode(dilate, closing, element);
		
		//find contours
		List <MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(canny,contours, new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
		
		Iterator<MatOfPoint> iterator = contours.iterator();
		
		Mat drawContour = new Mat();
		Imgproc.cvtColor(canny, drawContour, Imgproc.COLOR_GRAY2BGR);
		
		while(iterator.hasNext()) {
			
			MatOfPoint contour = iterator.next();
			
			//calculate area of contour
			double area = Imgproc.contourArea(contour);
			System.out.println("Area: " + area );
			
			Rect rect = Imgproc.boundingRect(contour);
			Imgproc.rectangle(drawContour, new Point(rect.x,rect.y),
					new Point(rect.x+rect.width, rect.y + rect.height),
					new Scalar(255,0,0),3);
			
			System.out.println(rect.x + ":" + rect.y + ":" + rect.width + ":" + rect.height );
			
		}
		
		//HSV Hue Saturation Value
		
		Mat hsv = new Mat();
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_RGB2HSV);
		
		Scalar lowerThreshold = new Scalar(230,0,0);
		Scalar upperThreshold = new Scalar(255,0,0);
		
		Core.inRange(image,lowerThreshold, upperThreshold, hsv); //img
		
		
				
		
		Imgproc.filter2D(gray_hist, sobel, -1, kernelSobel);

		Mat sobelIntern = new Mat();
		Imgproc.Sobel(gray_hist, sobelIntern, -1, 1, 1);

		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".jpg", hsv, matOfByte);
		

		byte[] byteArray = matOfByte.toArray();
		InputStream in = new ByteArrayInputStream(byteArray);

		BufferedImage buffImage = ImageIO.read(in);
		WritableImage writableImage = SwingFXUtils.toFXImage(buffImage, null);

		return writableImage;

	}

	@Override
	public void start(Stage stage) throws Exception {
		// TODO Auto-generated method stub

		WritableImage writableImage = loadImage();

		ImageView imageView = new ImageView(writableImage);

		imageView.setX(50);
		imageView.setY(25);

		imageView.setFitWidth(500);
		imageView.setFitHeight(400);

		imageView.setPreserveRatio(true);

		Group root = new Group(imageView);

		Scene scene = new Scene(root, 500, 450);

		// setting title to the stage
		stage.setTitle("Reading image");

		stage.setScene(scene);

		stage.show();

	}

}