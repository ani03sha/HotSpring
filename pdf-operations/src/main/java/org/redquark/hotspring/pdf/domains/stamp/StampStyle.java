package org.redquark.hotspring.pdf.domains.stamp;

import lombok.Data;

@Data
public class StampStyle {

    private String text;
    private Double fontHeight;
    private Double rotation;
    private String color;
    private Double opacity;
    private Double horizontalOffset;
    private Double verticalOffset;
}
