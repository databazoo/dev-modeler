
package com.databazoo.devmodeler.tools.organizer;

import static com.databazoo.devmodeler.gui.Menu.L_REARRANGE_ALPHABETICAL;
import static com.databazoo.devmodeler.gui.Menu.L_REARRANGE_CIRCULAR;
import static com.databazoo.devmodeler.gui.Menu.L_REARRANGE_EXPLODE;
import static com.databazoo.devmodeler.gui.Menu.L_REARRANGE_FORCE_BASED;
import static com.databazoo.devmodeler.gui.Menu.L_REARRANGE_IMPLODE;

public interface OrganizerFactory {

    static Organizer get(){
        return get(L_REARRANGE_ALPHABETICAL);
    }
    static Organizer get(String actionCommand) {
        switch(actionCommand){
        case L_REARRANGE_CIRCULAR:		return getCircular();
        case L_REARRANGE_FORCE_BASED:	return getForceDirected();
        case L_REARRANGE_EXPLODE:		return getExplode();
        case L_REARRANGE_IMPLODE:		return getImplode();
        default:						return getAlphabetical();
        }
    }

    static Organizer getAlphabetical() {
        return new OrganizerAlphabetical();
    }

    static Organizer getCircular() {
        return new OrganizerCircular();
    }

    static Organizer getForceDirected() {
        return new OrganizerForceDirected();
    }

    static Organizer getExplode() {
        return new OrganizerExplode();
    }

    static Organizer getImplode() {
        return new OrganizerExplode(false);
    }
}
