package difflib;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author mksenzov
 */
class DeltaComparator implements Comparator<Delta>, Serializable {
    private static final long serialVersionUID = 1L;
    static final Comparator<Delta> INSTANCE = new DeltaComparator();

    @Override
    public int compare(final Delta a, final Delta b) {
        final int posA = a.getOriginal().getPosition();
        final int posB = b.getOriginal().getPosition();
        if (posA > posB) {
            return 1;
        } else if (posA < posB) {
            return -1;
        }
        return 0;
    }
}