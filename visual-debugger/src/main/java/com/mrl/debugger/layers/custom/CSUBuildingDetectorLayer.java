package com.mrl.debugger.layers.custom;


import com.mrl.debugger.StaticViewProperties;
import com.mrl.debugger.Util;
import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaseDtoLayer;
import com.mrl.debugger.remote.dto.BuildingDetectorDto;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = true, caption = "Building Detector", tag = "CSUBuildingDetectorLayer", drawAllData = false)
public class CSUBuildingDetectorLayer extends MrlBaseDtoLayer<BuildingDetectorDto> {

    private static final Stroke STROKE_DEFAULT = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {
        g.setStroke(STROKE_DEFAULT);
        if (isDrawAllData()
                && (StaticViewProperties.selectedObject == null || !agentDtoMap.containsKey(StaticViewProperties.selectedObject.getID()))) {
            for (BuildingDetectorDto p : getAllDtoSet()) {
                paintBuilding(p, t, g);
                paintDto(transform(p, t), g);
            }
        } else if (StaticViewProperties.selectedObject != null
                && agentDtoMap.containsKey(StaticViewProperties.selectedObject.getID())) {
            BuildingDetectorDto dto = agentDtoMap.get(StaticViewProperties.selectedObject.getID());
            paintBuilding(dto, t, g);
            paintDto(transform(dto, t), g);
        }
        return new ArrayList<>();
    }

    @Override
    protected BuildingDetectorDto transform(BuildingDetectorDto p, ScreenTransform t) {
        //必须创建一个新的副本
        BuildingDetectorDto result = new BuildingDetectorDto();
        HashSet<Polygon> polygons = new HashSet<>();
        HashMap<Polygon, Boolean> polygonControllableMap = new HashMap<>();
        for (Polygon polygon : p.getDynamicClusterConvexHulls()) {
            boolean controllable = p.getPolygonControllableMap().get(polygon);
            Polygon transformedPolygon = Util.transform(polygon, t);
            polygons.add(transformedPolygon);
            polygonControllableMap.put(transformedPolygon, controllable);
        }
        result.setDynamicClusterConvexHulls(polygons);
        result.setPolygonControllableMap(polygonControllableMap);
        return result;
    }

    @Override
    protected void paintDto(BuildingDetectorDto p, Graphics2D g) {
        g.setStroke(STROKE_DEFAULT);
        p.getDynamicClusterConvexHulls().forEach(e -> {
            if (p.getPolygonControllableMap().get(e)) {
                g.setColor(new Color(0x1E4D2B));
            } else {
                g.setColor(Color.RED);
            }
            g.draw(e);
        });
    }


    private void paintBuilding(BuildingDetectorDto p, ScreenTransform t, Graphics2D g) {
        if (p.getTargetBuilding() != null) {
            p.getBorderBuildings().forEach(e -> {
                fillShape(getShape((Building) world.getEntity(new EntityID(e)), t), g, Color.YELLOW);
            });

            p.getInDirectionBuildings().forEach(e -> {
                fillShape(getShape((Building) world.getEntity(new EntityID(e)), t), g, new Color(10, 53, 210));
            });

            Building targetBuilding = (Building) world.getEntity(new EntityID(p.getTargetBuilding()));
            Polygon shape = getShape(targetBuilding, t);
            fillShape(shape, g, Color.CYAN);
        }
    }

    private void fillShape(Polygon polygon, Graphics2D g, Color color) {
        g.setColor(color);
        g.fill(polygon);
    }

    private Polygon getShape(Area area, ScreenTransform t) {
        List<Edge> edges = area.getEdges();
        if (edges.isEmpty()) {
            return new Polygon();
        }
        int count = edges.size();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int i = 0;
        for (Edge e : edges) {
            xs[i] = t.xToScreen(e.getStartX());
            ys[i] = t.yToScreen(e.getStartY());
            ++i;
        }
        return new Polygon(xs, ys, count);
    }

}
