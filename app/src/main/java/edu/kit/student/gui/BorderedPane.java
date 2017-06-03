package edu.kit.student.gui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 *
 */
public class BorderedPane extends StackPane {

    private final static int titlePadding = 4;
    private final Region title;

    public BorderedPane(Node content, Region inTitle)
    {
        this.title = inTitle;
        getChildren().add(content);
        inTitle.setPadding(new Insets(0, 0, 0, titlePadding));
        inTitle.setStyle("-fx-background-color: -fx-background");
        getChildren().add(inTitle);
        setPadding(new Insets(10, 0, 10, 0));
        setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID,
                new CornerRadii(4), new BorderWidths(1))));
    }



    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        final double groupCbHeight = title.prefHeight(-1);
        final double groupCbWidth = title.prefWidth(groupCbHeight) + titlePadding;
        title.resize(groupCbWidth, groupCbHeight);

        // Move checkbox a bit right from the top left corner
        title.relocate(titlePadding * 2, -groupCbHeight / 2.0 - 1);
    }
}
