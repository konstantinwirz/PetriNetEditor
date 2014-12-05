package edu.yapne.scene;


import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;


/**
 * Created by konstantin on 09/11/14.
 */
public class Transition extends  AbstractNode {

    private Rectangle rectangle;

    public Transition() {
        setupUi();
    }

    private void setupUi() {
        rectangle = RectangleBuilder.create()
                .width(getSize())
                .stroke(getDefaultStrokeColor())
                .fill(getDefaultFillColor())
                .height(getSize())
                .strokeWidth(getStrokeWidth())
                .build();

        setCenter(rectangle);
        setAlignment(rectangle, Pos.CENTER);
    }

    @Override
    protected final void onSizeChanged(double newSize) {
        rectangle.setWidth(newSize);
        rectangle.setHeight(newSize);
    }

    @Override
    protected final void onStrokeWidthChanged(double newWidth) {
        rectangle.setStrokeWidth(newWidth);
    }

}
