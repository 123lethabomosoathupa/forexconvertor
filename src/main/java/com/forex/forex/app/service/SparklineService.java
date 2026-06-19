package com.forex.forexapp.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class SparklineService {

    /**
     * Generates a sparkline PNG from a list of rate values. Returns the image
     * as a byte array. Width: 300px, Height: 80px — compact inline sparkline.
     */
    public byte[] generateSparkline(String from, String to, List<Double> rates)
            throws IOException {
// Build dataset
        XYSeries series = new XYSeries("Rate");
        for (int i = 0; i < rates.size(); i++) {
            series.add(i, rates.get(i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
// Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                null, // no title
                null, // no x-axis label
                null, // no y-axis label
                dataset,
                PlotOrientation.VERTICAL,
                false, // no legend
                false, // no tooltips
                false // no URLs
        );
// Style — clean, minimal, dark theme matching the UI
        chart.setBackgroundPaint(new Color(15, 23, 42)); // #0f172a
        chart.setBorderVisible(false);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(15, 23, 42));
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
// Line colour: sky blue #38bdf8
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(56, 189, 248));
        renderer.setSeriesStroke(0, new BasicStroke(1.8f));
        plot.setRenderer(renderer);
// Hide axes
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false);
// Render to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 300, 80);
        return baos.toByteArray();
    }

    /**
     * Returns a 1x1 transparent PNG if there is no data yet for this pair.
     * Prevents broken image icons in the UI.
     */
    public byte[] emptyImage() throws IOException {
// Minimal 1x1 transparent PNG bytes
        return new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x00, 0x00, 0x02,
            0x00, 0x01, (byte) 0xE5, 0x27, (byte) 0xDE, (byte) 0xFC,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
