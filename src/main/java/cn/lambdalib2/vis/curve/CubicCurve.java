package cn.lambdalib2.vis.curve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CubicCurve {

    public static class Point implements Comparable<Point> {
        public double x, y;

        public Point() {}

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Point o) {
            return Double.compare(x, o.x);
        }
    }

    private final List<Point> pts = new ArrayList<>();

    public CubicCurve() {}

    public void addPoint(double x, double y) {
        pts.add(new Point(x, y));
        Collections.sort(pts);
    }

    public double valueAt(double x) {
        if (pts.isEmpty()) {
            return 0;
        }

        int index = 0;
        for (; index < pts.size() && pts.get(index).x < x; ++index) ;

        if (index == pts.size()) {

            Point p2 = pts.get(pts.size() - 1);
            double k = pts.size() >= 2 ? ik(index - 1, index - 2) : 0;
            return p2.y + (x - p2.x) * k;
        }

        if (index == 0) {

            Point p0 = getPoint(0);
            return p0.y + k(0, 1) * (x - p0.x);
        }

        Point p0 = getPoint(index - 1), p1 = getPoint(index);
        double l = p1.x - p0.x;
        double t = (x - p0.x) / l, t2 = t * t, t3 = t2 * t;
        double y0 = p0.y, y1 = p1.y, m0 = k(index - 1, l), m1 = k(index, l);

        return t3 * (m0 + m1 + 2 * y0 - 2 * y1)
                + t2 * (-2 * m0 - m1 - 3 * y0 + 3 * y1)
                + t * (m0)
                + y0;
    }

    private double k(int i, double l) {
        double ret;
        if (i == 0) {
            ret = pts.size() == 1 ? 0 : ik(i, i + 1);
        } else if (i == pts.size() - 1) {
            ret = ik(i, i - 1);
        } else {
            ret = 0.5 * (ik(i + 1, i) + ik(i, i - 1));
        }
        return ret * l;
    }

    private double ik(int i1, int i2) {
        Point p1 = pts.get(i1), p2 = pts.get(i2);
        return (p2.y - p1.y) / (p2.x - p1.x);
    }

    public int pointCount() {
        return pts.size();
    }

    public Point getPoint(int i) {
        return pts.get(i);
    }

    public void reset() {
        pts.clear();
    }
}
