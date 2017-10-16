
package com.databazoo.devmodeler.tools.organizer;

import com.databazoo.devmodeler.model.DB;
import com.databazoo.devmodeler.model.Schema;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.SchemaReference;

public interface Organizer {

    void organize(Workspace ws);

    void organize(SchemaReference schema, int cycles);

    void organize(SchemaReference schema);

    void organize(DB db);

    void organize(Schema schema, int cycles);

    void organize(Schema schema);
}
