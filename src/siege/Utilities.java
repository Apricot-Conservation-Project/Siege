package siege;

import arc.math.geom.Point2;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public final class Utilities {
    // Probably does not need to ever be touched again

    /**
     * Finds the point with the least sum distance to all given points, accurate to within the given precision.
     * @param points The set of points to find the geometric median of.
     * @param precision The approximate maximum distance acceptable between the returned value and the theoretical true median.
     * @return The approximate geometric median of the set of points.
     */
    public static Point2D.Float geometricMedian(Point2D.Float[] points, float precision) {
        if (points.length == 0) {
            throw new IllegalArgumentException("Point array cannot have length zero.");
        }
        if (points.length == 1) {
            return points[0];
        }
        // Get average point to start geometric median approximation
        float sumX = 0, sumY = 0;
        for (Point2D.Float point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        if (points.length == 2) {
            return new Point2D.Float(sumX / points.length, sumY / points.length);
        }
        final Point2D.Float mean = new Point2D.Float(sumX / points.length, sumY / points.length);
        // samples[0] = center point (starts at mean)
        Point2D.Float[] samples = new Point2D.Float[] {mean, new Point2D.Float(), new Point2D.Float(), new Point2D.Float(), new Point2D.Float()};
        // Start with precision equal to the greatest distance between any point and the mean
        float currentPrecision = Float.MIN_VALUE;
        for (Point2D.Float point : points) {
            float distance = (float) point.distance(mean);
            if (distance > currentPrecision) {
                currentPrecision = distance;
            }
        }
        Point2D.Float[] offsets = new Point2D.Float[] {new Point2D.Float(0, 0), new Point2D.Float(-currentPrecision, -currentPrecision), new Point2D.Float(-currentPrecision, currentPrecision), new Point2D.Float(currentPrecision, -currentPrecision), new Point2D.Float(currentPrecision, currentPrecision)};
        Point2D.Float median;
        // Test a center point and cross of four nearby points, the best median approximation becomes the center for the next round, if the center is best, finish.
        while (true) {
            double minDistance = Double.MAX_VALUE;
            int minIndex = -1;
            for (int i = 0; i < offsets.length; i++) {
                Point2D.Float offset = offsets[i];
                Point2D.Float sample = new Point2D.Float(samples[0].x + offset.x, samples[0].y + offset.y);
                double sumDistance = 0;
                for (Point2D.Float point : points) {
                    sumDistance += point.distance(sample);
                }
                if (sumDistance < minDistance - 0.00000000001) {
                    minDistance = sumDistance;
                    minIndex = i;
                }
            }
            if (minIndex == 0) {
                // Use successively finer precision until needs are satisfied
                if (currentPrecision <= precision) {
                    median = samples[0];
                    break;
                }
                currentPrecision = currentPrecision / 2f;
                offsets = new Point2D.Float[] {new Point2D.Float(0, 0), new Point2D.Float(-currentPrecision, -currentPrecision), new Point2D.Float(-currentPrecision, currentPrecision), new Point2D.Float(currentPrecision, -currentPrecision), new Point2D.Float(currentPrecision, currentPrecision)};
                continue;
            }
            samples[0] = samples[minIndex];
        }
        return median;
    }

    /**
     * A pair of values of any types.
     * @param <T> The type of the first value
     * @param <U> The type of the second value
     */
    public static class Tuple<T, U> {

        public final T a;
        public final U b;

        /**
         * Creates a tuple given its two values.
         * @param a The tuple's first value
         * @param b The tuple's second value
         */
        public Tuple(T a, U b) {
            this.a = a;
            this.b = b;
        }

        /**
         * Returns one of the tuple's values, treating the first as index 0 and the second as index 1.
         *
         * @param index The index of the value to retrieve
         * @return The value in the tuple at the given index
         */
        public Object get(int index) {
            assert index == 0 || index == 1;
            return index == 0 ? a : b;
        }

        /**
         * Returns true if the compared tuple's items are both equal to this tuple's items
         *
         * @param other The object to compare. Automatically false if not a tuple.
         * @return Whether this is equal to the given object
         */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Tuple)) {
                return false;
            }
            return (((Tuple<?, ?>) other).a.equals(this.a) && ((Tuple<?, ?>) other).b.equals(this.b));
        }

        @Override
        public String toString() {
            return "(" + this.a.toString() + ", " + this.b.toString() + ")";
        }
    }

    /**
     * Represents a boundless grid, with a value at each whole number 2D point.
     * @param <T> The type contained in each grid pixel
     */
    public static class ArrayGrid<T> {
        // Outer represents columns, inner rows
        private final ArrayList<ArrayList<T>> quadrant1; // +, +
        private final ArrayList<ArrayList<T>> quadrant2; // -, +
        private final ArrayList<ArrayList<T>> quadrant3; // -, -
        private final ArrayList<ArrayList<T>> quadrant4; // +, -

        private final T defaultValue;
        private int lowerBoundX;
        private int lowerBoundY;
        private int upperBoundX;
        private int upperBoundY;

        public ArrayGrid(T defaultValue) {
            quadrant1 = new ArrayList<>();
            quadrant2 = new ArrayList<>();
            quadrant3 = new ArrayList<>();
            quadrant4 = new ArrayList<>();
            this.defaultValue = defaultValue;
        }

        public T get(Point2 index) {
            Tuple<ArrayList<ArrayList<T>>, Point2> quadrantIndex = getQuadrantIndex(index);
            return quadrantIndex.a.get(quadrantIndex.b.x).get(quadrantIndex.b.y);
        }

        public T get(int x, int y) {
            return get(new Point2(x, y));
        }

        public void set(Point2 index, T value) {
            Tuple<ArrayList<ArrayList<T>>, Point2> quadrantIndex = getQuadrantIndex(index);
            quadrantIndex.a.get(quadrantIndex.b.x).set(quadrantIndex.b.y, value);
        }

        public void set(int x, int y, T value) {
            set(new Point2(x, y), value);
        }

        private Tuple<ArrayList<ArrayList<T>>, Point2> getQuadrantIndex(Point2 index) {
            int x = index.x;
            int y = index.y;
            ArrayList<ArrayList<T>> quadrant;
            if (x >= 0) {
                if (y >= 0) {
                    quadrant = quadrant1;
                } else {
                    y = -y - 1;
                    quadrant = quadrant4;
                }
            } else {
                x = -x - 1;
                if (y >= 0) {
                    quadrant = quadrant2;
                } else {
                    y = -y - 1;
                    quadrant = quadrant3;
                }
            }
            while (x > quadrant.size() - 1) {
                quadrant.add(new ArrayList<>());
            }
            while (y > quadrant.get(x).size() - 1) {
                quadrant.get(x).add(defaultValue);
            }
            if (x < lowerBoundX) {
                lowerBoundX = x;
            }
            if (y < lowerBoundY) {
                lowerBoundY = y;
            }
            if (x > upperBoundX) {
                upperBoundX = x;
            }
            if (y > upperBoundY) {
                upperBoundY = y;
            }
            return new Tuple<>(quadrant, new Point2(x, y));
        }

        public int getLowerBoundX() {
            return lowerBoundX;
        }

        public int getLowerBoundY() {
            return lowerBoundY;
        }

        public int getUpperBoundX() {
            return upperBoundX;
        }

        public int getUpperBoundY() {
            return upperBoundY;
        }
    }
}
