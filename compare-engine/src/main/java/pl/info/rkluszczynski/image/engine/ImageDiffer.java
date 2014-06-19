package pl.info.rkluszczynski.image.engine;

import JavaMI.MutualInformation;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.info.rkluszczynski.image.core.compare.PersonCorrelation;
import pl.info.rkluszczynski.image.core.compare.SampleCorrelation;
import pl.info.rkluszczynski.image.core.compare.metric.CompareMetric;
import pl.info.rkluszczynski.image.core.compare.phash.HammingDistance;
import pl.info.rkluszczynski.image.core.compare.phash.ImagePHash05;
import pl.info.rkluszczynski.image.engine.model.metrics.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Rafal on 2014-06-08.
 */
public class ImageDiffer {
    private static final Logger logger = LoggerFactory.getLogger(ImageDiffer.class);

    public static void calculateDifferStatistics(BufferedImage image1, BufferedImage image2) {
        calculateDifferStatistics(image1, image2, null);
    }

    public static void calculateDifferStatistics(BufferedImage image1, BufferedImage image2, List<Double> stats) {
        Locale.setDefault(Locale.ENGLISH);

        Color[][] imageColorArray1 = convertImageToColorArray(image1);
        Color[][] imageColorArray2 = convertImageToColorArray(image2);

        Color[] imageColorOneDimensionalArray1 = convertArrayToOneDimension(imageColorArray1);
        Color[] imageColorOneDimensionalArray2 = convertArrayToOneDimension(imageColorArray2);

        double[] resultArray;
        double resultValue;

        CompareMetric[] compareMetrics = {
                new AbsAveGrayScaleMetric(), new PSNRAveGrayScaleMetric(), new RMSEAveGrayScaleMetric(),
                new AbsColorMetric(), new ExpColorMetric(), new PSNRColorMetric(), new RMSEColorMetric(),
                new NRMSEColorMetric()
        };
        for (CompareMetric metric : compareMetrics) {
            logger.info(calculateMetricValue(imageColorArray1, imageColorArray2, metric, stats));
        }

        resultArray = SampleCorrelation.calculateForRGB(imageColorOneDimensionalArray1, imageColorOneDimensionalArray2);
        logger.info("  Sample Correlation Coefficients for RGB: {}", getResultArrayString(resultArray, stats));

        resultArray = PersonCorrelation.calculateForRGB(imageColorOneDimensionalArray1, imageColorOneDimensionalArray2);
        logger.info("  Person Correlation Coefficients for RGB: {}", getResultArrayString(resultArray, stats));

        resultValue = calculateGrayScalePHash(image1, image2);
        if (stats != null) {
            stats.add(resultValue);
        }
        logger.info(String.format("                 GrayScale PHash distance: %.3f", resultValue));

        resultArray = calculateColorPHashes(image1, image2);
        logger.info("          Color PHashes distances for RGB: {}", getResultArrayString(resultArray, stats));

        resultArray = calculateMI4RGB(imageColorOneDimensionalArray1, imageColorOneDimensionalArray2);
        logger.info("        Mutual Information values for RGB: {}", getResultArrayString(resultArray, stats));
    }

    private static String calculateMetricValue(Color[][] imageArray1, Color[][] imageArray2, CompareMetric metric, List<Double> stats) {
        metric.resetValue();
        for (int iw = 0; iw < imageArray1.length; ++iw) {
            int columnHeight = imageArray1[0].length;
            for (int ih = 0; ih < columnHeight; ++ih) {
                metric.addPixelsDifference(imageArray1[iw][ih], imageArray2[iw][ih]);
            }
        }
        double value = metric.calculateValue();
        if (stats != null) {
            stats.add(value);
        }
        return
                String.format("                  Metric %10s value: %.3f", metric.getName(), value);
    }

    private static String getResultArrayString(double[] resultArray, List<Double> stats) {
        List<Double> resultsList = Lists.newArrayList();
        double resultSum = 0;
        for (int i = 0; i < resultArray.length; ++i) {
            resultsList.add(new Double(resultArray[i]));
            resultSum += resultArray[i];
        }
        double resultAvg = resultSum / resultArray.length;

        Collections.sort(resultsList);
        double median = resultsList.get(resultsList.size() / 2);
        if (resultsList.size() % 2 == 0) {
            median += resultsList.get(resultsList.size() / 2 - 1);
            median /= 2;
        }

        if (stats != null) {
            for (int i = 0; i < resultArray.length; ++i) {
                stats.add(new Double(resultArray[i]));
            }
            stats.add(resultAvg);
            stats.add(median);
        }
        return String.format("%s, mean=%.3f, median=%.3f", covertArrayToString(resultArray), resultAvg, median);
    }

    private static String covertArrayToString(double[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.format("%.3f", a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    private static double[] calculateMI4RGB(Color[] imageArray1, Color[] imageArray2) {
        if (imageArray1.length != imageArray2.length) {
            throw new IllegalArgumentException("Image arrays length differs!");
        }
        double result[] = new double[3];
        int pixelsCount = imageArray1.length;

        for (int rgb = 0; rgb < 3; ++rgb) {
            double scores1[] = new double[pixelsCount];
            double scores2[] = new double[pixelsCount];

            for (int i = 0; i < pixelsCount; ++i) {
                switch (rgb) {
                    case 0:
                        scores1[i] = imageArray1[i].getRed();
                        scores2[i] = imageArray2[i].getRed();
                        break;
                    case 1:
                        scores1[i] = imageArray1[i].getGreen();
                        scores2[i] = imageArray2[i].getGreen();
                        break;
                    case 2:
                        scores1[i] = imageArray1[i].getBlue();
                        scores2[i] = imageArray2[i].getBlue();
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
            result[rgb] = MutualInformation.calculateMutualInformation(scores1, scores2);
        }
        logger.debug("Mutual Information values for RGB: {}", Arrays.toString(result));
        return result;
    }

    private static double[] calculateColorPHashes(BufferedImage image1, BufferedImage image2) {
        ImagePHash05 imagePHash = new ImagePHash05();
        String[] pHashes1 = imagePHash.getColorHashes(image1);
        String[] pHashes2 = imagePHash.getColorHashes(image2);
        double[] result = new double[3];
        for (int i = 0; i < 3; ++i)
            result[i] = HammingDistance.calculate(pHashes1[i], pHashes2[i]);
        return result;
    }

    private static double calculateGrayScalePHash(BufferedImage image1, BufferedImage image2) {
        ImagePHash05 imagePHash = new ImagePHash05();
        String pHash1 = imagePHash.getGrayScaleHash(image1);
        String pHash2 = imagePHash.getGrayScaleHash(image2);
        return HammingDistance.calculate(pHash1, pHash2);
    }

    private static Color[] convertArrayToOneDimension(Color[][] imageColorArray) {
        int N = imageColorArray.length * imageColorArray[0].length;
        Color[] oneDimensionalColorArray = new Color[N];
        for (int iw = 0; iw < imageColorArray.length; ++iw) {
            int columnHeight = imageColorArray[0].length;
            for (int ih = 0; ih < columnHeight; ++ih) {
                oneDimensionalColorArray[iw * columnHeight + ih] =
                        new Color(imageColorArray[iw][ih].getRGB());
            }
        }
        return oneDimensionalColorArray;
    }

    private static Color[][] convertImageToColorArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Color[][] imageColorArray = new Color[width][height];
        for (int iw = 0; iw < width; ++iw) {
            for (int ih = 0; ih < height; ++ih) {
                imageColorArray[iw][ih] = new Color(image.getRGB(iw, ih));
            }
        }
        return imageColorArray;
    }
}
