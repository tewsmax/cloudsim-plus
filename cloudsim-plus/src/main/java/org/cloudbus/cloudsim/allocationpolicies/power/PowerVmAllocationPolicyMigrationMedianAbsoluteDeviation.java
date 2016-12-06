/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.allocationpolicies.power;

import org.cloudbus.cloudsim.hosts.power.PowerHost;
import org.cloudbus.cloudsim.hosts.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.selectionpolicies.power.PowerVmSelectionPolicy;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * A VM allocation policy that uses Median Absolute Deviation (MAD) to compute
 * a dynamic threshold in order to detect host over utilization.
 *
 * <p>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * <ul>
 * <li><a href="http://dx.doi.org/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * </p>
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation extends PowerVmAllocationPolicyMigrationDynamicUpperThresholdAbstract {

    /**
     * Creates a PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation
     * with a {@link #getSafetyParameter() safety parameter} equals to 0
     * and no {@link #getFallbackVmAllocationPolicy() fallback policy}.
     *
     * @param vmSelectionPolicy the policy that defines how VMs are selected for migration
     */
    public PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation(PowerVmSelectionPolicy vmSelectionPolicy) {
        super(vmSelectionPolicy);
    }

    /**
     * Creates a PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation.
     *
     * @param vmSelectionPolicy          the policy that defines how VMs are selected for migration
     * @param safetyParameter            the safety parameter
     * @param fallbackVmAllocationPolicy the fallback VM allocation policy to be used when
     * the over utilization host detection doesn't have data to be computed
     */
    public PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation(PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, PowerVmAllocationPolicyMigration fallbackVmAllocationPolicy) {
        super(vmSelectionPolicy, safetyParameter, fallbackVmAllocationPolicy);
    }

    /**
     * Computes the host utilization MAD used for generating the host over utilization threshold.
     *
     * @param host the host
     * @return the host utilization MAD
     * @throws {@inheritDoc}
     */
    @Override
    public double computeHostUtilizationMeasure(PowerHostUtilizationHistory host) throws IllegalArgumentException {
        double[] data = host.getUtilizationHistory();
        if (MathUtil.countNonZeroBeginning(data) >= 12) { // 12 has been suggested as a safe value
            return MathUtil.mad(data);
        }

        throw new IllegalArgumentException("There is not enough Host history to compute Host utilization MAD");
    }

}
