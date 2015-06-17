package phys2d.collisionLogic.collisionCheckers;

import java.util.ArrayList;

import phys2d.collisionLogic.tools.LinePolyTools;
import phys2d.entities.Vec2D;
import phys2d.entities.shapes.Shape;
import phys2d.entities.shapes.polygons.Polygon;

/**
 * A collection of methods that utilizes the GJK and EPA algorithms to solve
 * problems such as getting collision normals between shapes and finding the
 * closest displacement between them.
 * 
 * @author afsheen
 */

public final class CollisionCheckerGJKEPA {

    /**
     * Return whether the polygons p1, p2 are colliding using the GJK algorithm.
     * 
     * @param p1
     * @param p2
     * @return true if p1 and p2 are colliding. false otherwise
     */
    public static boolean isColliding(Shape s1, Shape s2) {
        ArrayList<Vec2D> computedSimplex = computeGJKsimplex(s1, s2);
        return computedSimplex.size() == 3 ? true : false;
        // because a point is removed from the simplex when it
        // does not contain the Vec2D.ORIGIN, it is impossible for the simplex
        // to contain 3 points. Hence false is returned.
    }

    /**
     * Return the vector by which s2 needs to be translated, so as not to
     * collide with s1 <br>
     * <b>FOR USE MAINLY IN DISCRETE COLLISION DETECTION.</b>
     * 
     * @param s1
     * @param s2
     * @return
     */
    public static Vec2D getCollisionResolutionGJKEPA(Shape s1, Shape s2) {

        ArrayList<Vec2D> computedSimplex;

        computedSimplex = computeGJKsimplex(s1, s2);
        computedSimplex = Polygon.arrangePoints(computedSimplex);

        if (computedSimplex.size() < 3) { // if no collision, return 0 vector
            return Vec2D.ORIGIN;
        }
        else {
            return getCollisionResolutionEPA(s1, s2, computedSimplex);
        }
    }

    /**
     * Computes the simplex which contains the Vec2D.ORIGIN
     * 
     * @param p1
     * @param p2
     * @return the end simplex of the computation.
     */
    private static ArrayList<Vec2D> computeGJKsimplex(Shape s1, Shape s2) {
        ArrayList<Vec2D> simplex = new ArrayList<Vec2D>(3);
        Vec2D dir;
        Vec2D supp;

        // HashSet<Vec2D> checkedPts = new HashSet<Vec2D>();

        dir = Vec2D.sub(s2.getCOM(), s1.getCOM()).getNormalized();
        supp = support(s1, s2, dir);
        simplex.add(supp);

        // checkedPts.add(supp);

        // Polygon pSum = polyDifference((Polygon) s1, (Polygon) s2);

        dir = Vec2D.getNegated(dir);
        while (true) {

            supp = support(s1, s2, dir);
            simplex.add(supp);

            /*
             * //The following condition was created to combat infinite loops
             * generated by float point errors //when the simplex was extremely
             * close to the origin. Since in the worst case there is an
             * //extremely small penetration (negligible), the safer alternative
             * of just returning false is taken. if(!checkedPts.add(supp)){ //If
             * the set contained the value already, return false.
             * //System.out.println("simplex: " + simplex);
             * //System.out.println("bypass used"); //simplex.remove(0);
             * //return simplex; }
             */

            // The above is no longer needed because of the addition of a
            // tolerance
            // to the getLeftOrRight method.

            // if the point isnt past the Vec2D.ORIGIN, then the mink diff
            // cannot contain the Vec2D.ORIGIN
            if (simplex.get(simplex.size() - 1).dotProduct(dir) < 0) {
                simplex.remove(0); // signifies that the origin was not found
                return simplex;
            }

            // now check if the simplex contains the Vec2D.ORIGIN
            if (simplex.size() == 3
                    && LinePolyTools.isPointInPolygon(
                            new Polygon(simplex.toArray(new Vec2D[] {})),
                            Vec2D.ORIGIN))
                return simplex; // if it is in the simplex, then collision

            // if it does not contain the Vec2D.ORIGIN, find the new search
            // direction
            Vec2D[] pts = Polygon
                    .arrangePoints(simplex.toArray(new Vec2D[] {}));

            // Simplex is a line. Find if the origin is to the left or the
            // right.
            if (pts.length == 2) {
                if (LinePolyTools.getLeftOrRight(pts[0], pts[1], Vec2D.ORIGIN) > 0)
                    dir = Vec2D.sub(pts[1], pts[0]).getNormal();
                else
                    dir = Vec2D.getNegated(Vec2D.sub(pts[1], pts[0])
                            .getNormal());

            }

            // The simplex is a triangle. Find the voronoi region in which the
            // ORIGIN lies.
            else {
                removeInvalidPointAndSetDir(pts, simplex, dir);
            }
        }
    }

    /**
     * Helper for GJK related functions. Takes in a triangular simplex, an array
     * containing the simplex points in clockwise order and the current
     * direction.
     * 
     * @param pts sorted simplex points (clockwise)
     * @param simplex the gjk simplex
     * @param dir current gjk search direction
     */
    private static void removeInvalidPointAndSetDir(Vec2D[] pts,
            ArrayList<Vec2D> simplex, Vec2D dir) {
        if (LinePolyTools.getLeftOrRight(pts[0], pts[1], Vec2D.ORIGIN) > 0) {
            dir = Vec2D.sub(pts[1], pts[0]).getNormal();
            simplex.remove(pts[2]);
        }

        else if (LinePolyTools.getLeftOrRight(pts[1], pts[2], Vec2D.ORIGIN) > 0) {
            dir = Vec2D.sub(pts[2], pts[1]).getNormal();
            simplex.remove(pts[0]);

        }

        else if (LinePolyTools.getLeftOrRight(pts[2], pts[0], Vec2D.ORIGIN) > 0) {
            dir = Vec2D.sub(pts[0], pts[2]).getNormal();
            simplex.remove(pts[1]);
        }
        else {
            System.out.println("Vec2D.ORIGIN is ON the simplex. GJK CHECKER"); // uh.??
            System.exit(1);
        }
        dir.normalize();
    }

    /**
     * Returns the displacement required to unstick both the shapes.
     * 
     * @param s1
     * @param s2
     * @param simplex
     * @return
     */
    private static Vec2D getCollisionResolutionEPA(Shape s1, Shape s2,
            ArrayList<Vec2D> simplex) {
        final double TOLERANCE = 0.1;

        int x = 0;

        while (true) { // Begin the EPA algorithm

            x++;
            if (x > 100) {
                System.out.println("EPA Checker failure");
                System.exit(0);
            }

            // First find the closest edge to the origin
            Vec2D[] closestEdge = new Vec2D[] {
                    simplex.get(simplex.size() - 1), simplex.get(0) };
            Vec2D closestDisp = LinePolyTools.ptToLineDisp(Vec2D.ORIGIN,
                    closestEdge);

            double closestDist = closestDisp.getSquaredLength();
            int insertionIndex = 0;

            Vec2D[] edge;
            Vec2D disp;
            double dist;

            for (int i = 0; i < simplex.size() - 1; i++) {
                edge = new Vec2D[] { simplex.get(i), simplex.get(i + 1) };
                disp = LinePolyTools.ptToLineDisp(Vec2D.ORIGIN, edge);
                dist = disp.getSquaredLength();

                if (dist < closestDist) {
                    closestEdge = edge;
                    closestDisp = disp;
                    closestDist = dist;
                    insertionIndex = i + 1;
                }
            }

            closestDist = Math.sqrt(closestDist);

            // Upon completion, we should have all the information of the
            // closest edge of the mink diff to the origin
            Vec2D edgeNorm = closestDisp.getNormalized();
            Vec2D point = support(s1, s2, edgeNorm);

            double newPtDistFromOrigin = point.dotProduct(edgeNorm);

            // If our new expansion point is already on the edge (or very close
            // to) then return that edge normal
            if (newPtDistFromOrigin - closestDist <= TOLERANCE) {

                edgeNorm.scaleBy(newPtDistFromOrigin);
                return edgeNorm;
            }
            else {
                simplex.add(insertionIndex, point);
            }
        }

    }

    /**
     * Return a point which is guaranteed to an edge of the minkowski difference
     * of p1 and p2. Along the dir axis. This is used to build the simplex to be
     * used in GJK
     * 
     * @param s1
     * @param s2
     * @param dir
     * @return
     */
    private static Vec2D support(Shape s1, Shape s2, Vec2D dir) {
        Vec2D pt1, pt2, supp;
        // get the max of s1 in dir direction
        pt1 = s1.getMax(dir);

        // get the min of s2 in dir direction
        pt2 = s2.getMin(dir);

        // perform the minkowski difference
        // supp is a point on the minkowski difference of p1, p2
        supp = Vec2D.sub(pt1, pt2);

        return supp;
    }

    /**
     * I actually dont remember why I coded this. But i think it is something to
     * do with swept collision detection to get the closest displacement to
     * check if the relvel will cause a collsion in the next frame.
     * 
     * @param s1
     * @param s2
     * @return
     */
    public static Vec2D getDisplacementBetweenShapes(Shape s1, Shape s2) {

        // Polygon p = LinePolyTools.polyDifference((Polygon)s1, (Polygon)s2);
        Vec2D[] simplex = new Vec2D[2]; // simplex will only be a line. Hence
                                        // forcing it to a size 2 array
        Vec2D dir;

        dir = Vec2D.sub(s2.getCOM(), s1.getCOM());
        simplex[0] = support(s1, s2, dir);

        dir = Vec2D.getNegated(dir);
        simplex[1] = support(s1, s2, dir);

        return getDisplacementBetweenShapes(s1, s2, simplex, dir);

    }

    public static Vec2D getDisplacementBetweenShapes(Shape s1, Shape s2,
            Vec2D[] simplex, Vec2D dir) {
        final double TOLERANCE = 0.1;

        int x = 0;
        int failureTol = s1.getPoints().length + s2.getPoints().length + 100;

        while (true) {

            x++;
            if (x > failureTol) {
                System.out
                        .println("DISPLACEMENT BETWEEN SHAPES Checker failure");
                System.out.println(s1 + "\n" + s2);
                System.exit(0);
            }

            Vec2D closestPt = LinePolyTools.getClosestPtToOrigin(simplex);

            if (closestPt.equals(Vec2D.ORIGIN)) {
                return closestPt;
            }

            dir = Vec2D.getNegated(closestPt).getNormalized();

            Vec2D supp = support(s1, s2, dir);

            // see if the new support point is actually making progress towards
            // the origin
            if (Vec2D.sub(supp, simplex[0]).dotProduct(dir) <= TOLERANCE) {
                return closestPt; // the closest feature of the line
            }

            // Remove the further point from the origin
            if (simplex[0].getSquaredLength() < simplex[1].getSquaredLength())
                simplex[1] = supp;
            else
                simplex[0] = supp;

        }

    }

    /**
     * 
     * @param s1
     * @param s2
     * @return
     */
    public static Object[] hybridGJKSolver(Shape s1, Shape s2) {
        ArrayList<Vec2D> simplex = new ArrayList<Vec2D>(3);
        Vec2D dir;
        Vec2D supp;

        int x = 0;
        int failureTol = 100;

        // HashSet<Vec2D> checkedPts = new HashSet<Vec2D>();

        dir = Vec2D.sub(s2.getCOM(), s1.getCOM()).getNormalized();
        supp = support(s1, s2, dir);
        simplex.add(supp);

        dir = dir.getNegated();

        while (true) {
            System.out.println("In hybrid solver");
            x++;
            if (x > failureTol) {
                System.out.println("HYBRID CHECKER FAILURE");
                System.out.println(s1 + "\n" + s2);
                System.exit(0);
            }

            supp = support(s1, s2, dir);
            simplex.add(supp);

            // if the point isnt past the Vec2D.ORIGIN, then the mink diff
            // cannot contain the Vec2D.ORIGIN
            if (simplex.get(simplex.size() - 1).dotProduct(dir) < 0) {
                // no explicit collision
                if (simplex.size() != 2) {
                    if (simplex.size() != 3) {
                        System.out.println("ERROR IN HYBRID SOLVER");
                        System.exit(1);
                    }

                    // If triangular simplex, remove the point not contributing
                    // to
                    // the correct voronoi region containing the origin.
                    removeInvalidPointAndSetDir(Polygon.arrangePoints(simplex
                            .toArray(new Vec2D[] {})), simplex, dir);

                }
                // Since no explicit collision, use the swept solver.
                return new Object[] {
                        getDisplacementBetweenShapes(s1, s2,
                                simplex.toArray(new Vec2D[] {}), dir), false };
            }

            // now check if the simplex contains the Vec2D.ORIGIN
            if (simplex.size() == 3
                    && LinePolyTools.isPointInPolygon(
                            new Polygon(simplex.toArray(new Vec2D[] {})),
                            Vec2D.ORIGIN)) {
                // explicit collision here
                Vec2D resolution = getCollisionResolutionEPA(s1, s2, simplex);
                return new Object[] { resolution,
                        resolution.equals(Vec2D.ORIGIN) ? false : true };
            }

            // if it does not contain the Vec2D.ORIGIN, find the new search
            // direction
            Vec2D[] pts = Polygon
                    .arrangePoints(simplex.toArray(new Vec2D[] {}));

            // Simplex is a line. Find if the origin is to the left or the
            // right.
            if (pts.length == 2) {
                if (LinePolyTools.getLeftOrRight(pts[0], pts[1], Vec2D.ORIGIN) > 0)
                    dir = Vec2D.sub(pts[1], pts[0]).getNormal();
                else
                    dir = Vec2D.getNegated(Vec2D.sub(pts[1], pts[0])
                            .getNormal());

            }

            // The simplex is a triangle. Find the voronoi region in which the
            // ORIGIN lies.
            else {
                if (LinePolyTools.getLeftOrRight(pts[0], pts[1], Vec2D.ORIGIN) > 0) {
                    dir = Vec2D.sub(pts[1], pts[0]).getNormal();
                    simplex.remove(pts[2]);
                }

                else if (LinePolyTools.getLeftOrRight(pts[1], pts[2],
                        Vec2D.ORIGIN) > 0) {
                    dir = Vec2D.sub(pts[2], pts[1]).getNormal();
                    simplex.remove(pts[0]);

                }

                else if (LinePolyTools.getLeftOrRight(pts[2], pts[0],
                        Vec2D.ORIGIN) > 0) {
                    dir = Vec2D.sub(pts[0], pts[2]).getNormal();
                    simplex.remove(pts[1]);
                }
                else {
                    System.out
                            .println("Vec2D.ORIGIN is ON the simplex. GJK CHECKER"); // uh.??
                    System.exit(1);
                }
                dir.normalize();
            }
        }

    }

}
