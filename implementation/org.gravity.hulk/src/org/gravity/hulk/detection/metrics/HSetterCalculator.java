/**
 */
package org.gravity.hulk.detection.metrics;

import org.gravity.hulk.antipatterngraph.HMetric;

import org.gravity.hulk.detection.HClassBasedMetricCalculator;

import org.gravity.typegraph.basic.TClass;
// <-- [user defined imports]
// [user defined imports] -->

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>HSetter Calculator</b></em>'.
 * <!-- end-user-doc -->
 *
 *
 * @see org.gravity.hulk.detection.metrics.MetricsPackage#getHSetterCalculator()
 * @model
 * @generated
 */
public interface HSetterCalculator extends HClassBasedMetricCalculator {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	HMetric calculateMetric(TClass tClass);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	double calculateValue(TClass tClass);
	// <-- [user code injected with eMoflon]

	// [user code injected with eMoflon] -->
} // HSetterCalculator
