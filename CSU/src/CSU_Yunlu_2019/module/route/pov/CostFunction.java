package CSU_Yunlu_2019.module.route.pov;

import java.awt.Point;

import CSU_Yunlu_2019.module.route.pov.graph.PointNode;



/**
 * The cost from current node to its successor node.
 */
public interface CostFunction {

	double cost(PointNode from, PointNode to, Point startPoint);
}
