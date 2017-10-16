package com.databazoo.devmodeler.gui.window.datawindow;

import javax.swing.*;
import java.awt.*;

import com.databazoo.devmodeler.gui.DesignGUI;
import com.databazoo.devmodeler.model.explain.ExplainLoad;
import com.databazoo.devmodeler.model.explain.ExplainOperation;
import com.databazoo.tools.Schedule;

abstract class DataWindowOutputExplain extends DataWindowOutputData {
	JScrollPane outputScrollExplain;
	JPanel outputExplain;

	private int explainY;
	private int explainX;

	void prepareOutputExplain(){
		outputExplain = new JPanel();
		outputExplain.setBackground(Color.WHITE);
		outputExplain.setLayout(null);
		outputScrollExplain = new JScrollPane(outputExplain);
		outputScrollExplain.getVerticalScrollBar().setUnitIncrement(DesignGUI.SCROLL_AMOUNT);
		outputScrollExplain.getHorizontalScrollBar().setUnitIncrement(DesignGUI.SCROLL_AMOUNT);
	}

	void drawExplain(){
		Schedule.inEDT(() -> {
            outputExplain.removeAll();

            explainX = outputExplain.getWidth() - ExplainOperation.SPACING - ExplainOperation.DEFAULT_WIDTH;
            explainY = ExplainOperation.SPACING;

            ExplainOperation root = connection.getExplainTree(result);
            root.setLocation(new Point(explainX, explainY));
            outputExplain.add(root);
            drawExplainChildren(root);

            int corrX = ExplainOperation.PADDING - explainX;
            for (int i = 0; i < outputExplain.getComponentCount(); i++) {
                Component comp = outputExplain.getComponent(i);
                comp.setLocation(new Point(comp.getLocation().x + corrX, comp.getLocation().y));
            }
            outputExplain.setPreferredSize(new Dimension(outputExplain.getWidth() + corrX, explainY + ExplainOperation.DEFAULT_HEIGHT + ExplainOperation.PADDING));
            outputExplain.setSize(new Dimension(outputExplain.getWidth() + corrX, explainY + ExplainOperation.DEFAULT_HEIGHT + ExplainOperation.PADDING));
            for (int i = 0; i < outputExplain.getComponentCount(); i++) {
                if (outputExplain.getComponent(i) instanceof ExplainOperation) {
                    ExplainOperation comp = (ExplainOperation) outputExplain.getComponent(i);
                    if (comp.parentLink != null) {
                        comp.parentLink.checkSize();
                    }
                    comp.getParent().setComponentZOrder(comp, 0);
                }
            }
			outputScrollExplain.repaint();
        });
	}

	private void drawExplainChildren(ExplainOperation parent){
		for(int i=0; i<parent.childrenLinks.size(); i++){
			ExplainLoad link = parent.childrenLinks.get(i);
			ExplainOperation operation = (ExplainOperation)link.getRel2();

			if(operation.opName.equals("null")){
				continue;
			}

			operation.setParent(parent);
			operation.setLocation(
					parent.getLocation().x-ExplainOperation.DEFAULT_WIDTH-ExplainOperation.SPACING,
					parent.getTakenY() - (i==0 ? ExplainOperation.DEFAULT_HEIGHT+ExplainOperation.SPACING : 0)
			);
			if(i>0){
				operation.setTakenY(operation.getLocation().y + ExplainOperation.DEFAULT_HEIGHT + ExplainOperation.SPACING);
			}

			drawExplainChildren(operation);

			outputExplain.add(operation);
			outputExplain.add(link);

			if(explainX > operation.getLocation().x){
				explainX = operation.getLocation().x;
			}
			if(explainY < operation.getLocation().y){
				explainY = operation.getLocation().y;
			}
			link.checkSize();
			link.checkLineWidth();
		}
	}
}
