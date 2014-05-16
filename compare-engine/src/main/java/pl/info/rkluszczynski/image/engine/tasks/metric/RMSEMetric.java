package pl.info.rkluszczynski.image.engine.tasks.metric;

import java.awt.*;

public class RMSEMetric implements Metric {

    private double metricValue = 0.;
    private double pixelsNumber = 0.;

    @Override
    public double calculateValue() {
        return Math.sqrt(metricValue / pixelsNumber);
    }

    @Override
    public void addPixelsDifference(Color inputPixel, Color templatePixel) {
        metricValue += ((inputPixel.getRed() - templatePixel.getRed()) * (inputPixel.getRed() - templatePixel.getRed()));
        metricValue += ((inputPixel.getGreen() - templatePixel.getGreen()) * (inputPixel.getGreen() - templatePixel.getGreen()));
        metricValue += ((inputPixel.getBlue() - templatePixel.getBlue()) * (inputPixel.getBlue() - templatePixel.getBlue()));
        ++pixelsNumber;
    }
}
