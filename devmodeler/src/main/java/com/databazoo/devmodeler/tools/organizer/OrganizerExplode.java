package com.databazoo.devmodeler.tools.organizer;

import java.awt.*;

import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.SchemaReference;
import com.databazoo.devmodeler.tools.Geometry;

public class OrganizerExplode implements Organizer {

    private static final float EXPLOSION_MULTIPLIER = 1.25F;
    private final float multiplier;

    OrganizerExplode() {
        this(true);
    }

    OrganizerExplode(boolean explode) {
        multiplier = explode ? EXPLOSION_MULTIPLIER : 1 / EXPLOSION_MULTIPLIER;
    }

    @Override public void organize(Workspace ws) {
        for (SchemaReference schema : ws.getSchemas()) {
            organize(schema);
            schema.setLocation(Geometry.getSnappedPosition(
                    Math.round(schema.getX() * multiplier),
                    Math.round(schema.getY() * multiplier)
            ));
        }
    }

    @Override public void organize(SchemaReference schema, int cycles) {
        for (int i = 0; i < cycles; i++) {
            organize(schema);
        }
    }

    @Override public void organize(SchemaReference schema) {
        final Component[] components = schema.getComponents();
        for (Component component : components) {
            if (component instanceof DraggableComponent) {
                component.setLocation(Geometry.getSnappedPosition(
                        Math.round(component.getX() * multiplier),
                        Math.round(component.getY() * multiplier)
                ));
            }
        }
    }

    @Override public void organize(DB db) {
        for (Schema schema : db.getSchemas()) {
            organize(schema);
            schema.setLocation(Geometry.getSnappedPosition(
                    Math.round(schema.getX() * multiplier),
                    Math.round(schema.getY() * multiplier)
            ));
        }
    }

    @Override public void organize(Schema schema, int cycles) {
        for (int i = 0; i < cycles; i++) {
            organize(schema);
        }
    }

    @Override public void organize(Schema schema) {
        final Component[] components = schema.getComponents();
        for (Component component : components) {
            if (component instanceof DraggableComponent) {
                component.setLocation(Geometry.getSnappedPosition(
                        Math.round(component.getX() * multiplier),
                        Math.round(component.getY() * multiplier)
                ));
            }
        }
    }
}
