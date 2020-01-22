package mrl_2019.world.tools;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.standard.entities.StandardEntity;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 6:19 PM
 */
public class LineInfo {
    private Line2D line;
    private StandardEntity entity;
    private boolean blocking;

    public LineInfo(Line2D line, StandardEntity entity, boolean blocking) {
        this.line = line;
        this.entity = entity;
        this.blocking = blocking;
    }

    public Line2D getLine() {
        return line;
    }

    public StandardEntity getEntity() {
        return entity;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
