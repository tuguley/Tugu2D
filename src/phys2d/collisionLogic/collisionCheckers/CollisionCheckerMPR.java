package phys2d.collisionLogic.collisionCheckers;

import phys2d.entities.Vec2D;
import phys2d.entities.shapes.Shape;

/**
 * This class contains methods which detect collision and return minimum
 * displacement to unstick shapes, using the Minkowski Portal Refinement
 * algorithm.
 * 
 * @author Afsheen
 *
 */
public final class CollisionCheckerMPR {

    /**
     * Uses the MPR algorithm to detect whether a collision has occurred between
     * shapes s1 and s2.
     * 
     * @param s1 the first shape.
     * @param s2 the second shape.
     * @return true if the shapes are colliding, false otherwise.
     */
    public static boolean isColliding(Shape s1, Shape s2) {
        SimplexDirStruct mprInfo = new SimplexDirStruct();
        computeSimplex(s1, s2, mprInfo);

        return mprInfo.isColliding;
    }

    /**
     * If the shapes are colliding, return the minimum displacement to unstick
     * them, otherwise, return the minimum displacement between the two shapes.
     * 
     * @param s1 the first shape.
     * @param s2 the second shape.
     */
    public static SimplexDirStruct getCollisionResolution(Shape s1, Shape s2) {
        SimplexDirStruct mprInfo = new SimplexDirStruct();
        computeSimplex(s1, s2, mprInfo);

        if (mprInfo.isColliding)
            computeCollisionResolution(s1, s2, mprInfo);

        else
            computeMinimumDisplacement(s1, s2, mprInfo);

        return mprInfo;
    }

    private static void computeCollisionResolution(Shape s1, Shape s2,
            SimplexDirStruct mprInfo) {
        // TODO Auto-generated method stub

    }

    /**
     * 
     * @param s1
     * @param s2
     * @param mprInfo
     */
    private static void computeMinimumDisplacement(Shape s1, Shape s2,
            SimplexDirStruct mprInfo) {
        // Honestly, im pretty sure GJK's march works fine.
        mprInfo.simplex.remove(0); // Remove the COM point.
        CollisionCheckerGJKEPA2.computeMinimumDisplacement(s1, s2, mprInfo);
    }

    /**
     * Evolve the simplex using MPR into it's final state.
     * 
     * @param s1 the first shape.
     * @param s2 the second shape.
     * @param mprInfo the structure where information about the MPR run is
     *        stored.
     */
    private static void computeSimplex(Shape s1, Shape s2,
            SimplexDirStruct mprInfo) {
        //@formatter:off
        /*
         * A---p>--B
         * .\...../.
         * ..\.../..
         * ...\./...
         * ....R....
         */
        //@formatter:on

        Vec2D RA, portal, RAnorm, RO;

        mprInfo.simplex.add(Vec2D.sub(s1.getCOM(), s2.getCOM())); // R: V0

        RO = mprInfo.simplex.get(0).getNegated();

        mprInfo.dir = RO;
        mprInfo.simplex.add(support(s1, s2, mprInfo.dir)); // A: V1

        RA = Vec2D.sub(mprInfo.simplex.get(1), mprInfo.simplex.get(0)); // RA
        RAnorm = RA.getNormal();

        // Find which side origin is on.
        if (RAnorm.dotProduct(mprInfo.dir) > 0) {
            mprInfo.dir = RAnorm;
        }
        else {
            mprInfo.dir = RAnorm.getNegated();
        }

        mprInfo.simplex.add(support(s1, s2, mprInfo.dir)); // B: V2

        // REFINEMENT PHASE. LOOP HERE?
        while (true) {

            // AB
            portal = Vec2D.sub(mprInfo.simplex.get(2), mprInfo.simplex.get(1));

            // Check if the origin is inside the simplex.
            /*
             * We already know that the origin is within the swept angle of RA
             * and RB. So we really only need to check if it's on the correct
             * side of the portal. Papers and the source algorithm say we need
             * to perform a full on triangle check, but I really see no reason
             * doing 2 redundant checks because we already knows it's within 2
             * of the 3 triangle edges.
             */
            // Check if origin is on correct side of portal.
            Vec2D newPt;
            Vec2D portalNorm = portal.getNormal(); // Normal pointing outwards
                                                   // from the triangle.

            // If the origin is outside the portal
            if (portalNorm.dotProduct(RO) > 0) {
                mprInfo.dir = portalNorm;
                newPt = support(s1, s2, mprInfo.dir);

                // See if the new point is past the origin.
                if (newPt.dotProduct(mprInfo.dir) < 0) { // If not past origin
                    mprInfo.isColliding = false;
                    return;
                }
                else {
                    //@formatter:off
                    /*
                     * ....C....
                     *.../...\..
                     * A---p>--B
                     * .\...../.
                     * ..1...2..
                     * ...\./...
                     * ....R....
                     */
                    //@formatter:on

                    // REFINE THE PORTAL.
                    // Check to see which point to discard from the simplex.
                    Vec2D RC = Vec2D.sub(newPt, mprInfo.simplex.get(0));

                    // If in RAC region, discard B.
                    if (RC.getNormal().dotProduct(RO) > 0) {
                        mprInfo.simplex.set(2, newPt);
                    }
                    else { // Else in RBC region, discard A.
                        mprInfo.simplex.set(1, newPt);
                    }

                }

            }
            else {
                // Else, it is inside the portal.
                mprInfo.isColliding = true;
                return;
            }

        }
    }

    /**
     * Returns the support point of the minkowski difference of s1 and s2 in
     * direction dir.
     * 
     * @param s1 the first shape.
     * @param s2 the second shape.
     * @param dir the direction to get the support point in.
     * @return the corresponding support mapping of dir for s1 - s2.
     */
    private static Vec2D support(Shape s1, Shape s2, Vec2D dir) {
        return (Vec2D.sub(s1.getMax(dir), s2.getMin(dir)));
    }

}
