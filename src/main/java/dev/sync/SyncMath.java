package dev.sync;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/** Pure functions. No game state. Units: blocks and milliseconds. */
final class SyncMath {
    private SyncMath() {}

    /** Velocity in blocks per millisecond over the newest samples inside windowMs. Zero when not enough data. */
    static Vec3d velocityPerMs(List<PositionHistory.Sample> s, double windowMs) {
        if (s.size() < 2) return Vec3d.ZERO;
        PositionHistory.Sample newest = s.get(s.size() - 1);
        PositionHistory.Sample base = s.get(s.size() - 2);
        for (int i = s.size() - 2; i >= 0; i--) {
            double dt = (newest.nanos() - s.get(i).nanos()) / 1.0e6;
            if (dt > windowMs) break;
            base = s.get(i);
        }
        double dtMs = (newest.nanos() - base.nanos()) / 1.0e6;
        if (dtMs < 20.0) return Vec3d.ZERO;
        Vec3d v = newest.pos().subtract(base.pos()).multiply(1.0 / dtMs);
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) return Vec3d.ZERO;
        return v;
    }

    /** Linear extrapolation by leadMs, displacement capped at maxDisp blocks. */
    static Vec3d predict(Vec3d pos, Vec3d velPerMs, double leadMs, double maxDisp, boolean vertical) {
        Vec3d v = vertical ? velPerMs : new Vec3d(velPerMs.x, 0.0, velPerMs.z);
        Vec3d d = v.multiply(leadMs);
        double len = d.length();
        if (!Double.isFinite(len)) return pos;
        if (len > maxDisp && len > 0.0) d = d.multiply(maxDisp / len);
        return pos.add(d);
    }

    /** Distance from eye to the first hit on box along dir within reach. NaN on miss. */
    static double rayHitDistance(Vec3d eye, Vec3d dir, Box box, double reach) {
        if (box.contains(eye)) return 0.0;
        Vec3d end = eye.add(dir.multiply(reach));
        Optional<Vec3d> hit = box.raycast(eye, end);
        if (hit.isEmpty()) return Double.NaN;
        return hit.get().distanceTo(eye);
    }
}
