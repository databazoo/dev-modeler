
package com.databazoo.devmodeler.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.databazoo.components.FontFactory;
import com.databazoo.components.UIConstants;
import com.databazoo.components.elements.DraggableComponent;
import com.databazoo.devmodeler.config.Settings;
import com.databazoo.devmodeler.config.Theme;
import com.databazoo.devmodeler.model.Attribute;
import com.databazoo.devmodeler.model.Constraint;
import com.databazoo.devmodeler.model.IModelElement;
import com.databazoo.devmodeler.model.Relation;
import com.databazoo.devmodeler.model.Workspace;
import com.databazoo.devmodeler.model.reference.ConstraintReference;
import com.databazoo.devmodeler.model.reference.LineComponentReference;
import com.databazoo.devmodeler.model.reference.RelationReference;
import com.databazoo.devmodeler.project.Project;
import com.databazoo.tools.Schedule;
import com.databazoo.tools.Usage;

/**
 * A small window displaying table information and table's surroundings.
 *
 * @author bobus
 */
public class Neighborhood extends JPanel {
	private static final Font HEADER_FONT = FontFactory.getSans(Font.PLAIN, Settings.getInt(Settings.L_FONT_CANVAS_SIZE));
	public static final Neighborhood instance = new Neighborhood();

	private boolean firstDraw = true;

	public final NeighborhoodCanvas canvas;
	private final JScrollPane scrolls;
	private IModelElement lastSelectedElement;
	private JButton btnWorkspace;
	private JButton btnSyncInfo;
	private JButton btnAnalyzeIndexes;

	private Neighborhood(){
		canvas = new NeighborhoodCanvas();
		scrolls = new JScrollPane(canvas);
		scrolls.setMinimumSize(new Dimension(0, 0));
		scrolls.getVerticalScrollBar().setUnitIncrement(16);

		setLayout(new BorderLayout(0,0));
		add(scrolls, BorderLayout.CENTER);
		add(getButtonPanel(), BorderLayout.SOUTH);

		Schedule.inWorker(Schedule.CLICK_DELAY, () -> {
            canvas.setHeight(0);
            enableButtons(false, false, false);
        });
	}

	public void draw(IModelElement selectedElem) {
		lastSelectedElement = selectedElem;
		if(lastSelectedElement != null){
			scrolls.getVerticalScrollBar().setValue(0);
			if(lastSelectedElement instanceof Relation){
				canvas.draw((Relation)lastSelectedElement);
				enableButtons(true, Project.getCurrent().getType() != Project.TYPE_ABSTRACT, true);
			}else{
				canvas.removeAll();
				canvas.setHeight(0);
				enableButtons(false, false, false);
			}
		}else{
			canvas.removeAll();
			canvas.setHeight(0);
			enableButtons(false, false, false);
		}
		if(firstDraw && canvas.lastSetHeight>0){
			Schedule.inEDT(50, () -> {
                firstDraw = false;
                draw(lastSelectedElement);
            });
		}
	}

	private void display(boolean show){
		setVisible(show);
	}

	private JPanel getButtonPanel(){
		btnWorkspace = new JButton("as workspace", Theme.getSmallIcon(Theme.ICO_WORKSPACE));
		btnWorkspace.addActionListener(e -> {
			Usage.log(UsageElement.NEIGHBORHOOD_BTN_WORKSPACE);
			Workspace.select(Workspace.create((DraggableComponent)lastSelectedElement));
		});

		btnSyncInfo = new JButton("sync table info", Theme.getSmallIcon(Theme.ICO_SYNCHRONIZE));
		btnSyncInfo.addActionListener(e -> {
			Usage.log(UsageElement.NEIGHBORHOOD_BTN_SYNC);
            if(lastSelectedElement instanceof Relation){
                ((Relation)lastSelectedElement).syncInfoWithServer();
            }
        });

		JPanel panel = new JPanel(new GridLayout(0,2,0,0));
		panel.add(btnWorkspace);
		panel.add(btnSyncInfo);

		/*if(Config.APP_GENERATION >= 2.0f){
			btnAnalyzeIndexes = new JButton("check indexes", Theme.getSmallIcon(Theme.ICO_APPROVED));
			btnAnalyzeIndexes.setEnabled(false);
			panel.add(btnAnalyzeIndexes);
		}*/
		return panel;
	}

	private void enableButtons (boolean eSaveAsWorkspace, boolean eSyncInfo, boolean eAnalyzeIndexes) {
		btnWorkspace.setEnabled(eSaveAsWorkspace);
		btnSyncInfo.setEnabled(eSyncInfo);
		if(btnAnalyzeIndexes != null) {
			btnAnalyzeIndexes.setEnabled(eAnalyzeIndexes);
		}
	}

	public final class NeighborhoodCanvas extends JComponent {
		int lastSetHeight = 0;
		private void setHeight(int newHeight){
			lastSetHeight = newHeight;
			Schedule.inEDT(() -> {
                int maxHeight = Canvas.instance.getScrollSize().height/2;
                switch (Settings.getStr(Settings.L_LAYOUT_NEIGHBORHOOD)) {
                    case "v":
                        if(lastSetHeight+5 < maxHeight){
                            lastSetHeight = maxHeight-6;
                        }
                        break;
                    case "h":
                        lastSetHeight = 0;
                        break;
                }
                setPreferredSize(new Dimension(0,lastSetHeight));
                scrolls.setPreferredSize(new Dimension(0, lastSetHeight > 0 ? (lastSetHeight+6 > maxHeight ? maxHeight : lastSetHeight+6) : 0));
                //instance.validate();
                if(instance.getParent() != null) {
                    instance.getParent().validate();
                }
                display(lastSetHeight>0);
                canvas.repaint();
            });
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Canvas.instance.getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		private void draw(Relation rel){
			removeAll();
			List<Relation> parents = new ArrayList<>();
			List<Relation> children = new ArrayList<>();
			LineComponentReference link;

			for(Constraint con: rel.getConstraints()){
				if(con.getRel1().equals(rel)){
					Relation elem = (Relation)con.getRel2();
					if(elem != null && !elem.equals(rel) && !parents.contains(elem)) {
						parents.add(elem);
					}
				}else{
					Relation elem = (Relation)con.getRel1();
					if(elem != null && !elem.equals(rel) && !children.contains(elem)) {
						children.add(elem);
					}
				}
			}
			Collections.sort(parents);
			Collections.sort(children);

			int parentSpace = (int) Math.round(Math.ceil(parents.size() * 0.5)) * Canvas.GRID_SIZE * 2;
			int childSpace = (int) Math.round(Math.ceil(children.size() * 0.5)) * Canvas.GRID_SIZE * 2;

			NeighborhoodRelation relShadow = new NeighborhoodRelation(rel);
			relShadow.setLocation((getWidth() - relShadow.getWidth())/2+3, Canvas.GRID_SIZE + parentSpace);
			add(relShadow);

			int offsetX = (getWidth() - relShadow.getWidth())/2;

			int i = 0;
			int i_mod = 0;
			if(parents.size()%2 > 0){
				i = 1;
				i_mod = 2;
				NeighborhoodRelationMini elem = new NeighborhoodRelationMini(parents.get(0));
				elem.setLocation(getWidth()/2-(elem.getWidth()-6)/2, Canvas.GRID_SIZE);
				add(elem);
				add(link = new ConstraintReference(null, null, relShadow, elem));
				link.respectsZoom = false;
				link.checkSize();
			}
			boolean placeLeft = true;
			for(;i<parents.size();i++){
				NeighborhoodRelationMini elem = new NeighborhoodRelationMini(parents.get(i));

				int left = placeLeft ? offsetX : offsetX + Canvas.GRID_SIZE + elem.getWidth();
				int top = Canvas.GRID_SIZE + (i_mod/2)*Canvas.GRID_SIZE*2;

				elem.setLocation(left, top);
				add(elem);
				add(link = new ConstraintReference(null, null, relShadow, elem));
				link.respectsZoom = false;
				link.checkSize();
				placeLeft = !placeLeft;
				i_mod++;
			}

			int size = children.size();
			if(size%2 > 0){
				size--;
			}
			placeLeft = true;
			for(int j=0;j<size;j++){
				NeighborhoodRelationMini elem = new NeighborhoodRelationMini(children.get(j));

				int left = placeLeft ? offsetX : offsetX + Canvas.GRID_SIZE + elem.getWidth();
				int top = Canvas.GRID_SIZE*2 + parentSpace + 106 + (j/2)*Canvas.GRID_SIZE*2;

				elem.setLocation(left, top);
				add(elem);
				add(link = new ConstraintReference(null, null, relShadow, elem));
				link.respectsZoom = false;
				link.checkSize();
				placeLeft = !placeLeft;
			}
			if(children.size()%2 > 0){
				int j = children.size()-1;
				NeighborhoodRelationMini elem = new NeighborhoodRelationMini(children.get(j));
				elem.setLocation(getWidth()/2-(elem.getWidth()-6)/2, Canvas.GRID_SIZE*2 + parentSpace + 106 + (j/2)*Canvas.GRID_SIZE*2);
				add(elem);
				add(link = new ConstraintReference(null, null, relShadow, elem));
				link.respectsZoom = false;
				link.checkSize();
			}

			for(Component com : getComponents()){
				if(com instanceof DraggableComponent){
					setComponentZOrder(com, 0);
				}
			}

			setHeight(Canvas.GRID_SIZE + parentSpace + 102 + childSpace + Canvas.GRID_SIZE);
			repaint();
		}
	}

	public final class NeighborhoodRelation extends RelationReference {

		NeighborhoodRelation(Relation rel) {
			super(rel);
			attributes.clear();
			rel.getDataInfo(attributes);
			setSize(new Dimension(Canvas.DEFAULT_ENTITY_WIDTH, 24+attributes.size()*Attribute.V_SIZE));

			setBackground(UIConstants.Colors.getPanelBackground());
			setForeground(UIConstants.Colors.getLabelForeground());

			attributes.forEach(Attribute::checkSizeNoZoom);
			displayNameWidth = UIConstants.GRAPHICS.getFontMetrics(HEADER_FONT).stringWidth(displayName);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(HEADER_FONT, (Graphics2D) g);
			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for(int i=0; i<attributes.size(); i++){
				Attribute attr = attributes.get(i);

				int top = i*Attribute.V_SIZE+14;

				if(attr.getBackground() != null){
					graphics.setColor(attr.getBackground());
					//graphics.setColor(Color.ORANGE);
					graphics.fillRect(1, top, attr.getWidth(), attr.getHeight());
				}

				graphics.setFont(attr.getFont());
				graphics.setColor(UIConstants.Colors.getLabelForeground());
				graphics.drawString(attr.getBehavior().getName(), 1, Attribute.V_SIZE-4 + top);
				if(attr.getTypeColor() != null) {
					graphics.setColor(attr.getTypeColor());
				}
				graphics.drawString(attr.getFullType(), attr.getTypeOffset(), Attribute.V_SIZE-4 + top);
			}
		}

		@Override public void clicked(){
			Usage.log(UsageElement.NEIGHBORHOOD_CLICKED);
			rel.clicked();
		}

		@Override public void rightClicked() {
			Usage.log(UsageElement.NEIGHBORHOOD_CONTEXT);
			super.rightClicked();
		}

		@Override
		public Point getAbsCenter(){
			return new Point(getLocation().x + (int)Math.round(getSize().width/2.0), getLocation().y + (int)Math.round(getSize().height/2.0));
		}
	}

	/**
	 * The little tables surrounding the main table.
	 */
	public final class NeighborhoodRelationMini extends RelationReference {
		private int displayNameWidth2;
		private String displayName2;

		NeighborhoodRelationMini(Relation rel) {
			super(null, rel);
			setSize(new Dimension(3*Canvas.GRID_SIZE+6, 32));

			setBackground(UIConstants.Colors.getPanelBackground());
			setForeground(UIConstants.Colors.getLabelForeground());

			FontMetrics fm = UIConstants.GRAPHICS.getFontMetrics(HEADER_FONT);
			displayNameWidth = fm.stringWidth(displayName);
			if(displayNameWidth > getWidth()-6){
				displayName = rel.getName().substring(0, rel.getName().length()/2);
				displayNameWidth = fm.stringWidth(displayName);
				displayName2 = rel.getName().substring(rel.getName().length()/2);
				displayNameWidth2 = fm.stringWidth(displayName2);
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			String name = displayName;
			displayName = null;
			super.paintComponent(HEADER_FONT, (Graphics2D) g);
			displayName = name;

			Graphics2D graphics = (Graphics2D) g;
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics.setFont(HEADER_FONT);
			if(displayName2 != null){
				graphics.drawString(displayName, (getWidth()-displayNameWidth-5)/2, HEADER_FONT.getSize());
				graphics.drawString(displayName2, (getWidth()-displayNameWidth2-5)/2, HEADER_FONT.getSize()+12);
			}else{
				graphics.drawString(displayName, (getWidth()-displayNameWidth-5)/2, HEADER_FONT.getSize()+6);
			}
		}

		@Override public void clicked(){
			Usage.log(UsageElement.NEIGHBORHOOD_CLICKED);
			rel.clicked();
		}

		@Override public void rightClicked() {
			Usage.log(UsageElement.NEIGHBORHOOD_CONTEXT);
			super.rightClicked();
		}

		@Override
		public Point getAbsCenter(){
			return new Point(getLocation().x + (int)Math.round(getSize().width/2.0), getLocation().y + (int)Math.round(getSize().height/2.0));
		}
	}
}
