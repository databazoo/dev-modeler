import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class HashCodeTest {
    @Test
    public void testIsShown() {
        List<ModelElement> elements = new ArrayList<>();
        elements.add(new ModelElement("test 1"));
        elements.add(new ModelElement("test 2"));
        elements.add(new ModelElement("test 3"));
        elements.add(new ModelElement("test 4"));
        elements.add(new ModelElement("test 5"));
        elements.add(new ModelElement("test X"));
        elements.add(new ModelElement("test X"));

        Set<ModelElement> set = new HashSet<>();
        set.addAll(elements);

        assertEquals(6, set.size());
        assertEquals(true, set.contains(new ModelElement("test 1")));

        Map<ModelElement, String> map = new HashMap<>();
        for (ModelElement element : elements) {
            map.put(element, element.getFullName());
        }
        assertEquals(6, map.size());
        assertEquals(true, map.containsKey(new ModelElement("test 1")));

        elements.get(0).setFullName("test renamed");

        assertEquals(6, set.size());
        assertEquals(false, set.contains(new ModelElement("test 1")));
        assertEquals(true, set.contains(new ModelElement("test renamed")));

        assertEquals(6, map.size());
        assertEquals(false, map.containsKey(new ModelElement("test 1")));
        assertEquals(true, map.containsKey(new ModelElement("test renamed")));
    }

    private class ModelElement{
        private String fullName;

        public ModelElement(String fullName) {
            this.fullName = fullName;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        @Override
        public int hashCode() {
            return fullName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof ModelElement){
                ModelElement elem = (ModelElement) obj;
                return fullName.equals(elem.fullName);
            }
            return false;
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
