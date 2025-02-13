package de.cesr.crafty.utils.graphical;

import java.util.HashMap;
import java.util.Set;

import eu.hansolo.fx.charts.SankeyPlot;
import eu.hansolo.fx.charts.SankeyPlot.StreamFillMode;
import eu.hansolo.fx.charts.SankeyPlotBuilder;
import eu.hansolo.fx.charts.data.PlotItem;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;

public class SankeyPlotGraph {

//	public static SankeyPlot sankey;

	public static SankeyPlot AFtsToSankeyPlot(HashMap<String, HashMap<String, Integer>> h,
			HashMap<String, Color> colors) {
		SankeyPlot san = SankeyPlotBuilder.create().build();
		HashMap<String, HashMap<String, PlotItem>> hashItems = new HashMap<>();
		HashMap<String, PlotItem> senderHash = new HashMap<>();
		HashMap<String, PlotItem> plothash = new HashMap<>();

		h.forEach((sender, hash) -> {
			hash.forEach((reciever, value) -> {
				if (value > 0) {
					PlotItem plotItem2 = new PlotItem(reciever, colors.get(reciever), 1);
					plothash.put(reciever, plotItem2);
				}
			});
			hashItems.put(sender, plothash);
			PlotItem plotItem1 = new PlotItem(sender, colors.get(sender), 0);
			senderHash.put(sender, plotItem1);
			san.addItem(plotItem1);
		});
		plothash.values().forEach((v) -> {
			san.addItem(v);
		});

		// define relation (valus) between items
		h.forEach((sender, hash) -> {
			hash.forEach((reciever, value) -> {
				senderHash.get(sender).addToOutgoing(hashItems.get(sender).get(reciever), value);

			});
		});
		configuration(san);
		return san;
	}

	public static SankeyPlot AFtsToSankeyPlot(HashMap<String, HashMap<String, Integer>> h,
			HashMap<String, Color> colors, Set<String> setManagers) {
		SankeyPlot sankey = SankeyPlotBuilder.create().build();
		HashMap<String, HashMap<String, PlotItem>> hashItems = new HashMap<>();
		HashMap<String, PlotItem> senderHash = new HashMap<>();
		HashMap<String, PlotItem> plothash = new HashMap<>();
		if (setManagers.size() == 0) {
			return null;
		}
		h.forEach((sender, hash) -> {
			if (setManagers.contains(sender)) {
				hash.forEach((reciever, value) -> {
					if (value > 0) {
						PlotItem plotItem2 = new PlotItem(reciever, colors.get(reciever), 1);
						plothash.put(reciever, plotItem2);
					}
				});
				if (!areAllValuesZero(hash)) {
					hashItems.put(sender, plothash);
					PlotItem plotItem1 = new PlotItem(sender, colors.get(sender), 0);
					senderHash.put(sender, plotItem1);
					sankey.addItem(plotItem1);
				}
			}
		});
		plothash.values().forEach((v) -> {
			sankey.addItem(v);
		});

		// define relation (valus) between items
		h.forEach((sender, hash) -> {
			hash.forEach((reciever, value) -> {
				if (setManagers.contains(sender) && hashItems.containsKey(sender))
					senderHash.get(sender).addToOutgoing(hashItems.get(sender).get(reciever), value);
			});
		});
		configuration(sankey);
		return sankey;
	}

	public static boolean areAllValuesZero(HashMap<String, Integer> hash) {
		for (Integer value : hash.values()) {
			if (value != 0) {
				return false;
			}
		}
		return true;
	}

	private static void configuration(SankeyPlot sankey) {
		sankey.setStreamFillMode(StreamFillMode.GRADIENT);
		sankey.setSelectionColor(Color.rgb(0, 0, 250, 0.5));
		sankey.setAutoItemGap(false);
		sankey.setAutoItemWidth(false);
		sankey.setPadding(new Insets(20, 20, 20, 20));
//	Canvas canvas = (Canvas) sankey.getChildren().iterator().next();
	}

}