
package plugins.components;

import com.databazoo.components.textInput.UndoableTextField;

/**
 * Text input with support for undo/redo, text search, tab size, etc.
 * @author bobus
 */
public class TextInput extends UndoableTextField {
	public TextInput(String text, boolean monoFont) {
		super(text, monoFont);
	}
}
