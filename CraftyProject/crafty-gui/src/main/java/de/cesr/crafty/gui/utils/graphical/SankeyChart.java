package de.cesr.crafty.gui.utils.graphical;

//SankeyChart.java — ordered links: source top→bottom goes to target top→bottom
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class SankeyChart extends Region {

	public static Region sankey(Map<String, HashMap<String, Integer>> flows, Map<String, Color> colors) {
		SankeyChart chart = new SankeyChart();
		chart.setData(flows);
		chart.setColors(colors);
		return chart;
	}

	// ---------- public API ----------
	public void setData(Map<String, HashMap<String, Integer>> flows) {
		this.data = (flows == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(flows);
		render();
	}

	public void setColors(Map<String, Color> colors) {
		customColors.clear();
		if (colors != null)
			customColors.putAll(colors);
		render();
	}

	public void setNodeWidth(double w) {
		nodeWidth = w;
		render();
	}

	public void setBaseGap(double g) {
		baseGap = g;
		render();
	}

	public void setCurvePad(double p) {
		curvePadding = p;
		render();
	}

	public void setFont(Font f) {
		labelFont = f;
		render();
	}

	// ---------- internals ----------
	private Map<String, HashMap<String, Integer>> data = new LinkedHashMap<>();
	private final Map<String, Color> customColors = new HashMap<>();

	private final Group root = new Group();
	private final Rectangle bg = new Rectangle();
	private final Group linksGroup = new Group();
	private final Group nodesGroup = new Group();

	private double nodeWidth = 18;
	private double baseGap = 12;
	private double curvePadding = 140;
	private Font labelFont = Font.font(13);
	private final Insets pad = new Insets(16, 16, 16, 16);

	// interaction state
	private static class LinkView {
		final String source;
		final CubicCurve curve;
		final Paint basePaint;

		LinkView(String s, String t, double v, CubicCurve c, Paint p) {
			source = s;
			curve = c;
			basePaint = p;
		}
	}

	private final List<LinkView> linkViews = new ArrayList<>();
	private String selectedSource = null;

	public SankeyChart() {
		setMinSize(0, 0);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		getChildren().add(root);
		root.getChildren().addAll(bg, linksGroup, nodesGroup);
		widthProperty().addListener(_ -> render());
		heightProperty().addListener(_ -> render());
	}

	@Override
	protected void layoutChildren() {
		render();
	}

	@Override
	protected double computePrefWidth(double h) {
		return 900;
	}

	@Override
	protected double computePrefHeight(double w) {
		return 500;
	}

	private static class NodeBox {
		final String name;
		final double total;
		final Color color;
		double y, h, offset;

		NodeBox(String n, double tot, Color c) {
			name = n;
			total = tot;
			color = c;
		}
	}

	private static class Link {
		final String s, t;
		final double v;

		Link(String s, String t, double v) {
			this.s = s;
			this.t = t;
			this.v = v;
		}
	}

	private void render() {
		double W = snap(getWidth()), H = snap(getHeight());
		if (W <= 0 || H <= 0)
			return;

		bg.setX(0);
		bg.setY(0);
		bg.setWidth(W);
		bg.setHeight(H);
		bg.setFill(Color.TRANSPARENT);
		bg.setOnMouseClicked(_ -> {
			selectedSource = null;
			updateHighlight();
		});

		linksGroup.getChildren().clear();
		nodesGroup.getChildren().clear();
		linkViews.clear();

		if (data.isEmpty())
			return;

		// Totals & links
		Map<String, Double> leftTotals = new LinkedHashMap<>();
		Map<String, Double> rightTotals = new LinkedHashMap<>();
		List<Link> links = new ArrayList<>();
		for (var e : data.entrySet()) {
			double sum = 0;
			for (var t : e.getValue().entrySet()) {
				if (t.getValue() <= 0)
					continue;
				links.add(new Link(e.getKey(), t.getKey(), t.getValue()));
				rightTotals.merge(t.getKey(), (double) t.getValue(), Double::sum);
				sum += t.getValue();
			}
			if (sum > 0)
				leftTotals.put(e.getKey(), sum);
		}
		if (leftTotals.isEmpty() || rightTotals.isEmpty())
			return;

		// Sort nodes (by total for a stable layout; y is computed next)
		var leftList = leftTotals.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
				.collect(Collectors.toList());
		var rightList = rightTotals.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
				.collect(Collectors.toList());

		Map<String, NodeBox> L = new LinkedHashMap<>();
		Map<String, NodeBox> R = new LinkedHashMap<>();
		int i = 0;
		for (var e : leftList)
			L.put(e.getKey(), new NodeBox(e.getKey(), e.getValue(), colorFor(e.getKey(), true, i++)));
		i = 0;
		for (var e : rightList)
			R.put(e.getKey(), new NodeBox(e.getKey(), e.getValue(), colorFor(e.getKey(), false, i++)));

		// margins for labels
		double leftLabW = L.values().stream().mapToDouble(n -> textW(n.name)).max().orElse(0);
		double rightLabW = R.values().stream().mapToDouble(n -> textW(n.name)).max().orElse(0);
		double leftMargin = leftLabW + 10;
		double rightMargin = rightLabW + 10;

		double chartLeft = snap(pad.getLeft() + leftMargin);
		double chartRight = snap(W - pad.getRight() - rightMargin);
		double chartWidth = Math.max(60, chartRight - chartLeft);

		double leftX = snap(chartLeft);
		double rightX = snap(chartLeft + chartWidth - nodeWidth);

		// unified flow unit
		double availH = H - pad.getTop() - pad.getBottom();
		double sumLeft = L.values().stream().mapToDouble(n -> n.total).sum();
		double sumRight = R.values().stream().mapToDouble(n -> n.total).sum();
		double unitLeftCap = (availH - baseGap * Math.max(0, L.size() - 1)) / Math.max(1e-6, sumLeft);
		double unitRightCap = (availH - baseGap * Math.max(0, R.size() - 1)) / Math.max(1e-6, sumRight);
		double flowUnit = Math.max(0, Math.min(unitLeftCap, unitRightCap));

		double usedHLeft = sumLeft * flowUnit;
		double usedHRight = sumRight * flowUnit;
		double gapLeft = (L.size() > 1) ? (availH - usedHLeft) / (L.size() - 1) : 0;
		double gapRight = (R.size() > 1) ? (availH - usedHRight) / (R.size() - 1) : 0;
		double gapL = Math.max(baseGap, gapLeft);
		double gapR = Math.max(baseGap, gapRight);

		// Layout nodes
		double y = pad.getTop();
		for (NodeBox n : L.values()) {
			n.h = snap(n.total * flowUnit);
			n.y = snap(y);
			n.offset = 0;
			y += n.h + gapL;
		}
		y = pad.getTop();
		for (NodeBox n : R.values()) {
			n.h = snap(n.total * flowUnit);
			n.y = snap(y);
			y += n.h + gapR;
		}

		// --------- NEW: order links by target Y within each source; process sources by
		// Y ----------
		Map<String, List<Link>> bySource = new HashMap<>();
		for (Link lk : links)
			bySource.computeIfAbsent(lk.s, _-> new ArrayList<>()).add(lk);
		// Sort each source's outgoing links by target top (y)
		for (List<Link> out : bySource.values()) {
			out.sort(Comparator.comparingDouble(lk -> R.get(lk.t).y));
		}
		// Sources processed top -> bottom (by their y)
		List<String> sourcesByY = new ArrayList<>(L.keySet());
		sourcesByY.sort(Comparator.comparingDouble(k -> L.get(k).y));
		// -----------------------------------------------------------------------------------------

		// Clip corridor
		Rectangle clip = new Rectangle(snap(leftX + nodeWidth), snap(pad.getTop()), snap(rightX - (leftX + nodeWidth)),
				snap(H - pad.getTop() - pad.getBottom()));
		linksGroup.setClip(clip);

		// counts
		Map<String, Integer> outCount = new HashMap<>(), inCount = new HashMap<>();
		for (Link lk : links) {
			outCount.merge(lk.s, 1, Integer::sum);
			inCount.merge(lk.t, 1, Integer::sum);
		}
		Map<String, Integer> outSeen = new HashMap<>(), inSeen = new HashMap<>();
		Map<String, Double> rOffset = new HashMap<>();

		// Draw links (ordered)
		for (String sName : sourcesByY) {
			NodeBox s = L.get(sName);
			if (s == null)
				continue;
			List<Link> out = bySource.getOrDefault(sName, Collections.emptyList());
			for (int idx = 0; idx < out.size(); idx++) {
				Link lk = out.get(idx);
				NodeBox t = R.get(lk.t);
				if (t == null)
					continue;

				double nominal = lk.v * flowUnit;
				double srcRemain = Math.max(0, s.h - s.offset);
				double tgtRemain = Math.max(0, t.h - rOffset.getOrDefault(t.name, 0.0));

				boolean lastOut = (idx == out.size() - 1); // last link for this source (ordered list)
				boolean lastIn = inSeen.getOrDefault(lk.t, 0) + 1 == inCount.get(lk.t);

				double thick = nominal;
				if (lastOut)
					thick = srcRemain;
				if (lastIn)
					thick = Math.min(thick, tgtRemain);
				thick = Math.min(thick, Math.min(srcRemain, tgtRemain));
				if (thick <= 0) {
					outSeen.merge(lk.s, 1, Integer::sum);
					inSeen.merge(lk.t, 1, Integer::sum);
					continue;
				}

				double sy = snap(s.y + s.offset + thick / 2.0);
				double ty = snap(t.y + rOffset.getOrDefault(t.name, 0.0) + thick / 2.0);

				double startX = snap(leftX + nodeWidth) + 0.25;
				double endX = snap(rightX) - 0.25;

				CubicCurve cc = new CubicCurve();
				cc.setStartX(startX);
				cc.setStartY(sy);
				cc.setControlX1(Math.min(endX - nodeWidth, startX + curvePadding));
				cc.setControlY1(sy);
				cc.setControlX2(Math.max(startX, endX - curvePadding));
				cc.setControlY2(ty);
				cc.setEndX(endX);
				cc.setEndY(ty);

				cc.setStrokeLineCap(StrokeLineCap.BUTT);
				cc.setStrokeWidth(snap(thick));
				cc.setFill(null);

				Paint paint = gradient(s.color, t.color);
				cc.setStroke(paint);
				cc.setOpacity(0.95);

				Tooltip.install(cc, new Tooltip(lk.s + " → " + lk.t + " : " + (int) lk.v));
				cc.setOnMouseEntered(_ -> setCursor(Cursor.HAND));
				cc.setOnMouseExited(_ -> setCursor(Cursor.DEFAULT));
				cc.setOnMouseClicked(e -> {
					if (e.getButton() == MouseButton.PRIMARY) {
						System.out.println(lk.s + " -> " + lk.t + " : " + (int) lk.v);
					}
					e.consume();
				});

				linksGroup.getChildren().add(cc);
				linkViews.add(new LinkView(lk.s, lk.t, lk.v, cc, paint));

				s.offset += thick;
				rOffset.merge(t.name, thick, Double::sum);
				outSeen.merge(lk.s, 1, Integer::sum);
				inSeen.merge(lk.t, 1, Integer::sum);
			}
		}

		// Nodes (clickable on left to highlight)
		for (NodeBox n : L.values())
			drawNodeAndLabel(nodesGroup, leftX, n, true, leftLabW, true);
		for (NodeBox n : R.values())
			drawNodeAndLabel(nodesGroup, rightX, n, false, rightLabW, false);

		updateHighlight();
	}

	private void drawNodeAndLabel(Group g, double x, NodeBox n, boolean leftSide, double labelW, boolean clickable) {
		Rectangle r = new Rectangle(snap(x), snap(n.y), snap(nodeWidth), snap(n.h));
		r.setArcWidth(0);
		r.setArcHeight(0);
		r.setFill(n.color.interpolate(Color.TRANSPARENT, 0.1));
		r.setStroke(Color.WHITE);
		Tooltip.install(r, new Tooltip(n.name + " : " + (int) n.total));

		if (clickable) {
			r.setCursor(Cursor.HAND);
			r.setOnMouseClicked(e -> {
				selectedSource = (n.name.equals(selectedSource)) ? null : n.name;
				updateHighlight();
				e.consume();
			});
		}

		Text label = new Text(n.name);
		label.setFont(labelFont);
		label.setFill(Color.web("#333"));
		double lx = leftSide ? snap(x - 8 - labelW) : snap(x + nodeWidth + 8);
		double ly = snap(n.y + n.h / 2 + label.getLayoutBounds().getHeight() / 4);
		label.setX(lx);
		label.setY(ly);

		g.getChildren().addAll(r, label);
	}

	private void updateHighlight() {
		final Paint highlight = Color.web("#2d6cdf");
		for (LinkView lv : linkViews) {
			if (selectedSource == null) {
				lv.curve.setStroke(lv.basePaint);
				lv.curve.setOpacity(0.95);
			} else if (lv.source.equals(selectedSource)) {
				lv.curve.setStroke(highlight);
				lv.curve.setOpacity(1.0);
			} else {
				lv.curve.setStroke(lv.basePaint);
				lv.curve.setOpacity(0.18);
			}
		}
	}

	// ---- helpers ----
	private Color colorFor(String key, boolean leftSide, int idx) {
		Color c = customColors.get(key);
		if (c != null)
			return c;
		return leftSide ? sourcePalette(idx) : targetPalette(idx);
	}

	private static double snap(double v) {
		return Math.round(v);
	}

	private double textW(String s) {
		Text t = new Text(s);
		t.setFont(labelFont);
		return Math.ceil(t.getLayoutBounds().getWidth());
	}

//	private static LinearGradient gradient(Color a, Color b) {
//		return new LinearGradient(0, 0, 1, 0, true, CycleMethod.REFLECT, new Stop(0, a), new Stop(1, b));
//	}
	private static LinearGradient gradient(Color a, Color b) {
		 double opacity=0.7;
	    Color aWithOpacity = new Color(a.getRed(), a.getGreen(), a.getBlue(), opacity);
	    Color bWithOpacity = new Color(b.getRed(), b.getGreen(), b.getBlue(), opacity);
	    return new LinearGradient(
	        0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
	        new Stop(0, aWithOpacity),
	        new Stop(1, bWithOpacity)
	    );
	}

	private static Color sourcePalette(int i) {
		Color[] p = { Color.web("#4C6EF5"), Color.web("#37B24D"), Color.web("#F59F00"), Color.web("#12B886"),
				Color.web("#E8590C"), Color.web("#6741D9") };
		return p[i % p.length];
	}

	private static Color targetPalette(int i) {
		Color[] p = { Color.web("#748FFC"), Color.web("#69DB7C"), Color.web("#FCC419"), Color.web("#63E6BE"),
				Color.web("#FFA94D"), Color.web("#B197FC") };
		return p[i % p.length];
	}
}
