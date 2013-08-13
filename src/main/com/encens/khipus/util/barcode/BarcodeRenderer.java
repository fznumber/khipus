package com.encens.khipus.util.barcode;

import com.encens.khipus.util.ValidatorUtil;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.output.java2d.Java2DCanvasProvider;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author
 * @version 3.1
 */
public class BarcodeRenderer {

    private Java2DCanvasProvider canvas;

    private BarcodeDimension barcodeDimension;

    private BarcodeGenerator barcodeGenerator;

    public BarcodeRenderer(String type) {
        barcodeGenerator = BarcodeBeanFactory.i.getBarcodeBean(type);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static InputStream paintInByteOutputStream(String barcode, Integer dpi, String type) {
        ByteArrayOutputStream outputStream = null;
        BarcodeData barcodeData = new BarcodeData();
        barcodeData.setText(barcode);
        BarcodeGenerator barcodeGenerator = BarcodeBeanFactory.i.getBarcodeBean(type);
        try {
            outputStream = new ByteArrayOutputStream();
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(outputStream,
                    "image/x-png",
                    dpi,
                    BufferedImage.TYPE_BYTE_BINARY,
                    false,
                    0);

            barcodeGenerator.generateBarcode(canvas, barcodeData.getText());

            canvas.finish();

        } catch (IOException e) {
            //
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Generate barcode in "code39" encoding
     * @param barcode code
     * @param dpi
     * @param displayStartStop true if start stop character '*' should be display
     * @return InputStream
     */
    public static InputStream paintCode39InByteOutputStream(String barcode, Integer dpi, boolean displayStartStop) {
        ByteArrayOutputStream outputStream = null;
        BarcodeData barcodeData = new BarcodeData();
        barcodeData.setText(barcode);

        Code39Bean code39Bean = (Code39Bean) BarcodeBeanFactory.i.getBarcodeBean("code39");
        code39Bean.setDisplayStartStop(displayStartStop);

        try {
            outputStream = new ByteArrayOutputStream();
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(outputStream,
                    "image/x-png",
                    dpi,
                    BufferedImage.TYPE_BYTE_BINARY,
                    false,
                    0);

            code39Bean.generateBarcode(canvas, barcodeData.getText());

            canvas.finish();

        } catch (IOException e) {
            //
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }


    public void paint(Graphics2D g2d, Object obj) {
        BarcodeData barcodeData = (BarcodeData) obj;

        if (ValidatorUtil.isBlankOrNull(barcodeData.getText())) {
            return;
        }

        initializeCanvas(g2d);

        initializeBarcodeDimension(barcodeData);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        try {
            AffineTransform affineTransform = g2d.getTransform();
            try {
                transformAsNecessary(g2d, barcodeData);
                g2d.setColor(Color.black);

                barcodeGenerator.generateBarcode(canvas, barcodeData.getText());

            } finally {
                g2d.setTransform(affineTransform);
            }
        } catch (Exception e) {
            g2d.setColor(Color.red);
            g2d.drawLine(0, 0, barcodeData.getWidth(), barcodeData.getHeight());
            g2d.drawLine(0, barcodeData.getHeight(), barcodeData.getWidth(), 0);
        }
    }

    private void initializeBarcodeDimension(BarcodeData barcodeData) {
        barcodeDimension = barcodeGenerator.calcDimensions(barcodeData.getText());
    }

    private void initializeCanvas(Graphics2D g2d) {
        if (null == canvas) {
            canvas = new Java2DCanvasProvider(g2d, 0);
        } else {
            canvas.setGraphics2D(g2d);
        }
    }

    protected void transformAsNecessary(Graphics2D g2d, BarcodeData barcodeData) {
        if (null != barcodeDimension) {
            double horizontalScale = barcodeData.getWidth() / barcodeDimension.getWidthPlusQuiet();
            double verticalScale = barcodeData.getHeight() / barcodeDimension.getHeightPlusQuiet();
            double scale;
            double dx = 0;
            double dy = 0;

            if (horizontalScale < verticalScale) {
                scale = horizontalScale;
                dy = ((barcodeData.getHeight() / scale) - barcodeDimension.getHeightPlusQuiet()) / 2;
            } else {
                scale = verticalScale;
                dx = ((barcodeData.getWidth() / scale) - barcodeDimension.getWidthPlusQuiet()) / 2;
            }
            g2d.scale(scale, scale);
            g2d.translate(dx, dy);
        }
    }
}
