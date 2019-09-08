
package com.databazoo.devmodeler.tools.formatter;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

import com.databazoo.components.UIConstants;
import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.tools.Dbg;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;

/**
 * Formatter implementation working with delta's above standard SQL.
 *
 * @author bobus
 */
public class FormatterDiff extends FormatterSQL {

	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_INSERT = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_INSERT);
	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_DELETE = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_DELETE);
	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_CHANGE = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_CHANGE);

	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_INSERT_DARK = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_INSERT_DARK);
	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_DELETE_DARK = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_DELETE_DARK);
	private static final DefaultHighlighter.DefaultHighlightPainter PAINTER_CHANGE_DARK = new DefaultHighlighter.DefaultHighlightPainter(UIConstants.Colors.HILIGHT_CHANGE_DARK);

	public FormatterDiff(List<Delta> deltas, boolean isSource) {
		super();
		this.deltaList = deltas;
		this.isSource = isSource;
		this.hilighter = new DiffHighlighter();
	}

	FormatProcess getFormatter(FormattedTextField pane){
		return new DeltaFormatterProcess(pane);
	}

	private class DeltaFormatterProcess extends FormatterBase.FormatProcess {

		private int nextDelta = 0;
		private int currentDeltaLasts;
		private int startOfLine;
		private DefaultHighlighter.DefaultHighlightPainter currentPainter;
		private int currentDeltaMore = 0;

		DeltaFormatterProcess(FormattedTextField pane) {
			super(pane);
		}

		@Override
		protected void checkDeltas() {
			if (currentDeltaMore > 0 && currentDeltaLasts == 0) {
				for (int i = 0; i < currentDeltaMore; i++) {
					token = "\n";
					out(styleRegular);
				}
				currentDeltaMore = 0;
			}
			if (currentPainter != null) {
				try {
					hilighter.addHighlight(startOfLine, doc.getLength() - 1, currentPainter);
				} catch (BadLocationException e) {
					Dbg.notImportant(Dbg.MANIPULATING_TEXT_OUTSIDE, e);
				}
				currentPainter = null;
			}
			startOfLine = doc.getLength();
			if (currentDeltaLasts > 0) {
				Delta delta = deltaList.get(nextDelta);
				if (isSource) {
					if (delta instanceof DeleteDelta) {
						currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_DELETE_DARK : PAINTER_DELETE;
					} else {
						currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_CHANGE_DARK : PAINTER_CHANGE;
					}
				} else {
					if (delta instanceof InsertDelta) {
						currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_INSERT_DARK : PAINTER_INSERT;
					} else {
						currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_CHANGE_DARK : PAINTER_CHANGE;
					}
				}
				currentDeltaLasts--;
				if (currentDeltaLasts == 0) {
					nextDelta++;
				}
			}
			if (deltaList != null && deltaList.size() > nextDelta) {
				Delta delta = deltaList.get(nextDelta);
				if (isSource) {
					if (delta.getOriginal().getPosition() == line) {
						if (delta instanceof InsertDelta) {
							for (int j = delta.getRevised().getLines().size(); j > 0; j--) {
								token = "\n";
								out(styleRegular);
							}
							nextDelta++;
						} else if (delta instanceof DeleteDelta) {
							currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_DELETE_DARK : PAINTER_DELETE;
							currentDeltaLasts = delta.getOriginal().getLines().size() - 1;
							if (currentDeltaLasts == 0) {
								nextDelta++;
							}
						} else {
							currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_CHANGE_DARK : PAINTER_CHANGE;
							currentDeltaLasts = delta.getOriginal().getLines().size() - 1;
							currentDeltaMore = delta.getRevised().getLines().size() - delta.getOriginal().getLines().size();
							if (currentDeltaLasts == 0) {
								nextDelta++;
							}
						}
					}
				} else {
					if (delta.getRevised().getPosition() == line) {
						if (delta instanceof DeleteDelta) {
							for (int j = delta.getOriginal().getLines().size(); j > 0; j--) {
								token = "\n";
								out(styleRegular);
							}
							nextDelta++;
						} else if (delta instanceof InsertDelta) {
							currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_INSERT_DARK : PAINTER_INSERT;
							currentDeltaLasts = delta.getRevised().getLines().size() - 1;
							if (currentDeltaLasts == 0) {
								nextDelta++;
							}
						} else {
							currentPainter = UIConstants.isLafWithDarkSkin() ? PAINTER_CHANGE_DARK : PAINTER_CHANGE;
							currentDeltaLasts = delta.getRevised().getLines().size() - 1;
							currentDeltaMore = delta.getOriginal().getLines().size() - delta.getRevised().getLines().size();
							if (currentDeltaLasts == 0) {
								nextDelta++;
							}
						}
					}
				}
			}
		}
	}

	private static class DiffHighlighter extends DefaultHighlighter {

		private JTextComponent component;

		@Override
		public final void install(final JTextComponent c) {
			super.install(c);
			this.component = c;
		}

		@Override
		public final void deinstall(final JTextComponent c) {
			super.deinstall(c);
			this.component = null;
		}

		@Override
		public final void paint(final Graphics g) {
			final Highlighter.Highlight[] highlights = getHighlights();
			final int len = highlights.length;
			for (int i = 0; i < len; i++) {
				Highlighter.Highlight info = highlights[i];
				if (info.getClass().getName().contains("LayeredHighlightInfo")) {
					final Rectangle bounds = this.component.getBounds();
					final Insets insets = this.component.getInsets();
					bounds.x = insets.left;
					bounds.y = insets.top;
					bounds.width -= insets.left + insets.right;
					bounds.height -= insets.top + insets.bottom;
					for (; i < len; i++) {
						info = highlights[i];
						if (info.getClass().getName().contains("LayeredHighlightInfo")) {
							info.getPainter().paint(g, info.getStartOffset(), info.getEndOffset()+1, bounds, this.component);
						}
					}
				}
			}
		}
	}
}
