package com.databazoo.devmodeler.model;

import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.LineComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.gui.Canvas;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.databazoo.components.elements.LineComponent.CLICK_TOLERANCE;
import static com.databazoo.components.elements.LineComponent.LEFT_BOTTOM_RIGHT_TOP;
import static com.databazoo.components.elements.LineComponent.LEFT_TOP_RIGHT_BOTTOM;
import static com.databazoo.components.elements.LineComponent.ONE_PLUS_HALF_TOLERANCE;
import static com.databazoo.components.elements.LineComponent.RIGHT_BOTTOM_LEFT_TOP;
import static com.databazoo.components.elements.LineComponent.RIGHT_TOP_LEFT_BOTTOM;
import static com.databazoo.components.elements.LineComponent.TWO_PLUS_HALF_TOLERANCE;

public interface ConstraintUtil {

    BufferedImage CROW_FOOT_RIGHT = rotateCrowsFoot(Math.toRadians(180), true, true);
    BufferedImage CROW_FOOT_LEFT = rotateCrowsFoot(Math.toRadians(0), true, true);
    BufferedImage CROW_FOOT_UP = rotateCrowsFoot(Math.toRadians(90), true, true);
    BufferedImage CROW_FOOT_DOWN = rotateCrowsFoot(Math.toRadians(270), true, true);

    BufferedImage LINES_0_RIGHT = rotateCrowsFoot(Math.toRadians(180), true, false);
    BufferedImage LINES_0_LEFT = rotateCrowsFoot(Math.toRadians(0), true, false);
    BufferedImage LINES_0_UP = rotateCrowsFoot(Math.toRadians(90), true, false);
    BufferedImage LINES_0_DOWN = rotateCrowsFoot(Math.toRadians(270), true, false);

    BufferedImage LINES_1_RIGHT = rotateCrowsFoot(Math.toRadians(180), false, false);
    BufferedImage LINES_1_LEFT = rotateCrowsFoot(Math.toRadians(0), false, false);
    BufferedImage LINES_1_UP = rotateCrowsFoot(Math.toRadians(90), false, false);
    BufferedImage LINES_1_DOWN = rotateCrowsFoot(Math.toRadians(270), false, false);

    /**
     * Calculate position of the cardinality sign in straight mode.
     *
     * Calculated positions are stored in {@code component.arrow1Location} and {@code component.arrow2Location} and
     * cardinality images are stored in {@code component.arrow1} and {@code component.arrow2}
     *
     * @param component constraint reference
     * @param behavior constraint behavior (to provide cardinality information)
     */
    static void calculateArrowPosition(LineComponent component, Constraint.Behavior behavior) {
        double angle;

        Dimension relSize = new Dimension(component.getRel1().getWidth() - Relation.SHADOW_GAP, component.getRel1().getHeight() - Relation.SHADOW_GAP);
        Dimension conSize = new Dimension(component.getWidth() - 2 - CLICK_TOLERANCE, component.getHeight() - 2 - CLICK_TOLERANCE);

        double relSideRatio = relSize.height * 1.0 / relSize.width;
        double conSideRatio = conSize.height * 1.0 / conSize.width;

        if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM) {
            if (relSideRatio > conSideRatio) {
                component.arrow1Location = new Point(relSize.width / 2 + CLICK_TOLERANCE / 2, (int) (relSize.width * conSideRatio / 2) + CLICK_TOLERANCE / 2);
            } else {
                component.arrow1Location = new Point((int) (relSize.height / conSideRatio / 2) + CLICK_TOLERANCE / 2, relSize.height / 2 + CLICK_TOLERANCE / 2);
            }
            component.arrow1Location.x -= 14 + 16;
            component.arrow1Location.y -= 14 + 16;
            angle = Math.atan(conSideRatio);

        } else if (component.getDirection() == RIGHT_BOTTOM_LEFT_TOP) {
            if (relSideRatio > conSideRatio) {
                component.arrow1Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
            } else {
                component.arrow1Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
            }
            component.arrow1Location.x -= 16 + 16;
            component.arrow1Location.y -= 15 + 16;
            angle = Math.atan(conSideRatio) + Math.toRadians(180);

        } else if (component.getDirection() == LEFT_BOTTOM_RIGHT_TOP) {
            if (relSideRatio > conSideRatio) {
                component.arrow1Location = new Point((relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
            } else {
                component.arrow1Location = new Point(((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
            }
            component.arrow1Location.x -= 14 + 16;
            component.arrow1Location.y -= 15 + 16;
            angle = Math.atan(1 / conSideRatio) + Math.toRadians(-90);

        } else {
            if (relSideRatio > conSideRatio) {
                component.arrow1Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
            } else {
                component.arrow1Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, (relSize.height / 2) + CLICK_TOLERANCE / 2);
            }
            component.arrow1Location.x -= 15 + 16;
            component.arrow1Location.y -= 15 + 16;
            angle = Math.atan(1 / conSideRatio) + Math.toRadians(90);
        }
        component.arrow1 = rotateCrowsFoot(angle, true, true);

        if ((behavior.attr1 != null && behavior.attr1.getBehavior().isAttNull()) || (Settings.getBool(Settings.L_PERFORM_PARENT_CARD) && behavior.attr2 != null)) {
            relSize = new Dimension(component.getRel2().getWidth() - Relation.SHADOW_GAP, component.getRel2().getHeight() - Relation.SHADOW_GAP);
            conSize = new Dimension(component.getWidth() - 2 - CLICK_TOLERANCE, component.getHeight() - 2 - CLICK_TOLERANCE);

            relSideRatio = relSize.height * 1.0 / relSize.width;
            conSideRatio = conSize.height * 1.0 / conSize.width;

            if (component.getDirection() == RIGHT_BOTTOM_LEFT_TOP) {
                if (relSideRatio > conSideRatio) {
                    component.arrow2Location = new Point(relSize.width / 2 + CLICK_TOLERANCE / 2, (int) (relSize.width * conSideRatio / 2) + CLICK_TOLERANCE / 2);
                } else {
                    component.arrow2Location = new Point((int) (relSize.height / conSideRatio / 2) + CLICK_TOLERANCE / 2, relSize.height / 2 + CLICK_TOLERANCE / 2);
                }
                component.arrow2Location.x -= 14 + 16;
                component.arrow2Location.y -= 15 + 16;
                angle = Math.atan(conSideRatio);

            } else if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM) {
                if (relSideRatio > conSideRatio) {
                    component.arrow2Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
                } else {
                    component.arrow2Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
                }
                component.arrow2Location.x -= 16 + 16;
                component.arrow2Location.y -= 15 + 16;
                angle = Math.atan(conSideRatio) + Math.toRadians(180);

            } else if (component.getDirection() == RIGHT_TOP_LEFT_BOTTOM) {
                if (relSideRatio > conSideRatio) {
                    component.arrow2Location = new Point((relSize.width / 2) + CLICK_TOLERANCE / 2, conSize.height - ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
                } else {
                    component.arrow2Location = new Point(((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, conSize.height - (relSize.height / 2) + CLICK_TOLERANCE / 2);
                }
                component.arrow2Location.x -= 15 + 16;
                component.arrow2Location.y -= 16 + 16;
                angle = Math.atan(1 / conSideRatio) + Math.toRadians(-90);

            } else {
                if (relSideRatio > conSideRatio) {
                    component.arrow2Location = new Point(conSize.width - (relSize.width / 2) + CLICK_TOLERANCE / 2, ((int) (relSize.width * conSideRatio / 2)) + CLICK_TOLERANCE / 2);
                } else {
                    component.arrow2Location = new Point(conSize.width - ((int) (relSize.height / conSideRatio / 2)) + CLICK_TOLERANCE / 2, (relSize.height / 2) + CLICK_TOLERANCE / 2);
                }
                component.arrow2Location.x -= 15 + 16;
                component.arrow2Location.y -= 15 + 16;
                angle = Math.atan(1 / conSideRatio) + Math.toRadians(90);
            }
            component.arrow2 = rotateCrowsFoot(angle, behavior.attr1.getBehavior().isAttNull(), false);
        } else {
            component.arrow2 = null;
        }
    }

    /**
     * Calculate position of the cardinality sign in Z mode.
     *
     * Calculated positions are stored in {@code component.arrow1Location} and {@code component.arrow2Location} and
     * cardinality images are stored in {@code component.arrow1} and {@code component.arrow2}
     *
     * @param component constraint reference
     * @param behavior constraint behavior (to provide cardinality information)
     */
    static void calculateZArrowPosition(LineComponent component, Constraint.Behavior behavior) {
        Dimension relSize = new Dimension(component.getRel1().getWidth() - Relation.SHADOW_GAP, component.getRel1().getHeight() - Relation.SHADOW_GAP);
        Dimension conSize = new Dimension(component.getWidth() - 2 - CLICK_TOLERANCE, component.getHeight() - 2 - CLICK_TOLERANCE);

        boolean drawArrow2 = (behavior.attr1 != null && behavior.attr1.getBehavior().isAttNull()) || (Settings.getBool(Settings.L_PERFORM_PARENT_CARD) && behavior.attr2 != null);
        if (!drawArrow2) {
            component.arrow2 = null;
        }

        if(conSize.width > relSize.width + 28){
            if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM || component.getDirection() == LEFT_BOTTOM_RIGHT_TOP) {
                component.arrow1 = CROW_FOOT_LEFT;
                component.arrow1Location = new Point(
                        relSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                        component.getDirection() == LEFT_TOP_RIGHT_BOTTOM ? TWO_PLUS_HALF_TOLERANCE - 32 : conSize.height + TWO_PLUS_HALF_TOLERANCE - 32);
                if (drawArrow2) {
                    component.arrow2 = component.isDashed() ? LINES_0_RIGHT : LINES_1_RIGHT;
                    component.arrow2Location = new Point(
                            conSize.width - relSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                            component.getDirection() != LEFT_TOP_RIGHT_BOTTOM ? TWO_PLUS_HALF_TOLERANCE - 32 : conSize.height + TWO_PLUS_HALF_TOLERANCE - 32);
                }
            } else {
                component.arrow1 = CROW_FOOT_RIGHT;
                component.arrow1Location = new Point(
                        conSize.width - relSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                        component.getDirection() == RIGHT_TOP_LEFT_BOTTOM ? TWO_PLUS_HALF_TOLERANCE - 32 : conSize.height + TWO_PLUS_HALF_TOLERANCE - 32);
                if (drawArrow2) {
                    component.arrow2 = component.isDashed() ? LINES_0_LEFT : LINES_1_LEFT;
                    component.arrow2Location = new Point(
                            relSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                            component.getDirection() != RIGHT_TOP_LEFT_BOTTOM ? TWO_PLUS_HALF_TOLERANCE - 32 : conSize.height + TWO_PLUS_HALF_TOLERANCE - 32);
                }
            }
        } else if (conSize.height > relSize.height/2 + component.getRel2().getHeight()/2 + 28 && conSize.width < relSize.width - 17) {
            if (component.getDirection() == LEFT_TOP_RIGHT_BOTTOM || component.getDirection() == RIGHT_TOP_LEFT_BOTTOM) {
                component.arrow1 = CROW_FOOT_UP;
                component.arrow1Location = new Point(
                        conSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                        relSize.height/2 + TWO_PLUS_HALF_TOLERANCE - 32);
                if (drawArrow2) {
                    component.arrow2 = component.isDashed() ? LINES_0_DOWN : LINES_1_DOWN;
                    component.arrow2Location = new Point(
                            conSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                            conSize.height - component.getRel2().getHeight()/2 + TWO_PLUS_HALF_TOLERANCE - 32);
                }
            } else {
                component.arrow1 = CROW_FOOT_DOWN;
                component.arrow1Location = new Point(
                        conSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                        conSize.height - relSize.height/2 + TWO_PLUS_HALF_TOLERANCE - 32);
                if (drawArrow2) {
                    component.arrow2 = component.isDashed() ? LINES_0_UP : LINES_1_UP;
                    component.arrow2Location = new Point(
                            conSize.width/2 + ONE_PLUS_HALF_TOLERANCE - 32,
                            component.getRel2().getHeight()/2 + TWO_PLUS_HALF_TOLERANCE - 32);
                }
            }
        } else {
            component.arrow1 = null;
            component.arrow2 = null;
        }
    }

    /**
     * Create a buffered image of the "crow's foot" (0..n) or twin lines (1..1)
     *
     * @param rads desired rotation
     * @param fromZero cardinality choice
     * @param toMany cardinality choice
     * @return 64x64 buffered image
     */
    static BufferedImage rotateCrowsFoot(double rads, boolean fromZero, boolean toMany) {
        Color color = UIConstants.Colors.getLabelForeground();
        BufferedImage dimg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dimg.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (rads != 0D) {
            graphics.rotate(rads, 32, 32);
        }

        if (toMany) {
            graphics.setColor(Canvas.instance.getBackground());
            graphics.fillRect(0, 30, 45, 4);

            graphics.setColor(color);
            graphics.drawLine(0, 22, 48, 32);
            graphics.drawLine(0, 32, 48, 32);
            graphics.drawLine(0, 42, 48, 32);
        } else {
            graphics.setColor(color);
            graphics.drawLine(39, 28, 39, 37);
        }

        if (fromZero) {
            graphics.setColor(Canvas.instance.getBackground());
            graphics.fillOval(43, 28, 9, 9);

            graphics.setColor(color);
            graphics.drawOval(43, 28, 9, 9);
        } else {
            graphics.setColor(color);
            graphics.drawLine(44, 28, 44, 37);
        }

        return dimg;
    }
}
