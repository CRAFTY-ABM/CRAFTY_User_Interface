package de.cesr.crafty.gui.utils.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal-dependency LOESS smoother (linear local regression + tricube kernel)
 * <p>
 *  – bandwidth:  fraction of total points used in each local fit (0 < bandwidth ≤ 1)  
 *  – robustnessIters: number of extra robustness iterations (0 – 4 is typical)  
 *  – non-negative output: any negative fitted value is clamped to 0
 */
public final class LoessStandalone {

    // ── Public facade ───────────────────────────────────────────────────────────
    public static ArrayList<Double> loessSmoothingData(List<Double> input) {
        double bandwidth = 0.30;
        int robustnessIters = 2;
        return loessSmoothingData(input, bandwidth, robustnessIters);
    }

    // Overload that lets you tune the knobs if you want
    public static ArrayList<Double> loessSmoothingData(
            List<Double> input, double bandwidth, int robustnessIters) {

        int n = input.size();
        if (n == 0) return new ArrayList<>();

        // 1) Build working copies of x and y
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;                 // equally-spaced abscissa
            y[i] = input.get(i);
        }

        int span = Math.max(2, (int) Math.ceil(bandwidth * n));

        // Arrays reused across robustness iterations
        double[] fitted = new double[n];
        double[] residuals = new double[n];
        double[] robustnessWeights = new double[n];
        java.util.Arrays.fill(robustnessWeights, 1.0);

        // 2) Main loop: up to robustnessIters + 1 passes
        for (int iter = 0; iter <= robustnessIters; iter++) {

            for (int i = 0; i < n; i++) {
                fitted[i] = localLinearFit(i, x, y,
                                            span, robustnessWeights);
            }

            // No need to compute residuals/weights on last pass
            if (iter == robustnessIters) break;

            for (int i = 0; i < n; i++) {
                residuals[i] = Math.abs(y[i] - fitted[i]);
            }
            // Median absolute deviation (MAD)
            double median = median(residuals);
            if (median == 0) break;  // perfect fit; done

            // Re-weight using bisquare function
            double sixMAD = 6 * median;
            for (int i = 0; i < n; i++) {
                double r = residuals[i] / sixMAD;
                if (r >= 1) {
                    robustnessWeights[i] = 0;
                } else {
                    double tmp = 1 - r * r;
                    robustnessWeights[i] = tmp * tmp;
                }
            }
        }

        // 3) Marshal result (clamp negatives to zero)
        ArrayList<Double> out = new ArrayList<>(n);
        for (double v : fitted) out.add(Math.max(0, v));
        return out;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    /** Perform a weighted local linear regression centred at x[index]. */
    private static double localLinearFit(int index, double[] x, double[] y,
                                         int span, double[] robustW) {

        int n = x.length;
        int left  = Math.max(0, index - span / 2);
        int right = Math.min(n - 1, left + span - 1);
        left      = Math.max(0, right - span + 1); // keep window length = span

        double x0 = x[index];
        double maxDist = Math.max(x0 - x[left], x[right] - x0);
        if (maxDist == 0) maxDist = 1; // all x equal (degenerate)

        // Weighted least-squares accumulators
        double S0 = 0, S1 = 0, S2 = 0, T0 = 0, T1 = 0;

        for (int j = left; j <= right; j++) {
            double dist = Math.abs(x[j] - x0) / maxDist;
            double w = tricube(dist) * robustW[j];
            double xj = x[j];
            double yj = y[j];

            S0 += w;
            S1 += w * xj;
            S2 += w * xj * xj;
            T0 += w * yj;
            T1 += w * xj * yj;
        }

        // Solve for β in y = β0 + β1·x  (normal equations)
        double denom = S0 * S2 - S1 * S1;
        if (denom == 0) return y[index]; // singular → fallback to original y

        double beta1 = (S0 * T1 - S1 * T0) / denom;
        double beta0 = (T0 - beta1 * S1) / S0;
        return beta0 + beta1 * x0;
    }

    private static double tricube(double u) {
        if (u >= 1) return 0;
        double tmp = 1 - u * u * u;
        return tmp * tmp * tmp;
    }

    private static double median(double[] values) {
        double[] copy = values.clone();
        java.util.Arrays.sort(copy);
        int n = copy.length;
        return (n % 2 == 0) ?
                0.5 * (copy[n/2 - 1] + copy[n/2]) :
                copy[n/2];
    }
}
