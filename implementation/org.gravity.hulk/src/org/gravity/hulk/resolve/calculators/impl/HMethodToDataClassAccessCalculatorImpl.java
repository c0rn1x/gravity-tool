/**
 */
package org.gravity.hulk.resolve.calculators.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
// <-- [user defined imports]
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HAntiPatternGraph;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.codesmells.HDataClassSmell;
import org.gravity.hulk.detection.impl.HMetricCalculatorImpl;
import org.gravity.hulk.refactoringgraph.HMethodToDataClassAccess;
import org.gravity.hulk.refactoringgraph.RefactoringgraphFactory;
import org.gravity.hulk.resolve.calculators.CalculatorsPackage;
import org.gravity.hulk.resolve.calculators.HMethodToDataClassAccessCalculator;
import org.gravity.typegraph.basic.TAccess;
import org.gravity.typegraph.basic.TClass;
import org.gravity.typegraph.basic.TFieldDefinition;
import org.gravity.typegraph.basic.TMember;
import org.gravity.typegraph.basic.TMethodDefinition;
import org.gravity.typegraph.basic.TSyntethicMethod;
// [user defined imports] -->
import org.gravity.typegraph.basic.annotations.TAnnotatable;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>HMethod To Data Class Access Calculator</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * </p>
 *
 * @generated
 */
public class HMethodToDataClassAccessCalculatorImpl extends HMetricCalculatorImpl
implements HMethodToDataClassAccessCalculator {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected HMethodToDataClassAccessCalculatorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return CalculatorsPackage.Literals.HMETHOD_TO_DATA_CLASS_ACCESS_CALCULATOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean detect(final HAntiPatternGraph apg) {

		final Object[] result1_black = HMethodToDataClassAccessCalculatorImpl
				.pattern_HMethodToDataClassAccessCalculator_0_1_ActivityNode1_blackB(this);
		if (result1_black == null) {
			throw new RuntimeException("Pattern matching failed." + " Variables: " + "[this] = " + this + ".");
		}
		// ForEach
		for (final Object[] result2_black : HMethodToDataClassAccessCalculatorImpl
				.pattern_HMethodToDataClassAccessCalculator_0_2_ActivityNode2_blackBFF(apg)) {
			final HBlobAntiPattern blob = (HBlobAntiPattern) result2_black[1];
			final TClass tClass = (TClass) result2_black[2];
			// ForEach
			for (final Object[] result3_black : HMethodToDataClassAccessCalculatorImpl
					.pattern_HMethodToDataClassAccessCalculator_0_3_ActivityNode3_blackBF(tClass)) {
				final TMember tMember = (TMember) result3_black[1];
				// ForEach
				for (final Object[] result4_black : HMethodToDataClassAccessCalculatorImpl
						.pattern_HMethodToDataClassAccessCalculator_0_4_ActivityNode18_blackBFF(blob)) {
					final HDataClassSmell dataclass = (HDataClassSmell) result4_black[1];
					final TClass tdataClass = (TClass) result4_black[2];

					final Object[] result5_bindingAndBlack = HMethodToDataClassAccessCalculatorImpl
							.pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_bindingAndBlackFFBB(this,
									tdataClass);
					if (result5_bindingAndBlack == null) {
						throw new RuntimeException("Pattern matching failed." + " Variables: " + "[this] = " + this
								+ ", " + "[tdataClass] = " + tdataClass + ".");
					}
					final EList<TMember> tAllEffectedMembers = (EList<TMember>) result5_bindingAndBlack[0];
					final EList<TMember> tAllMembers = (EList<TMember>) result5_bindingAndBlack[1];
					//
					final Object[] result6_black = HMethodToDataClassAccessCalculatorImpl
							.pattern_HMethodToDataClassAccessCalculator_0_6_ActivityNode17_blackBBFF(tAllMembers,
									tMember);
					if (result6_black != null) {
						//nothing TMember someMember = (TMember) result6_black[2];
						//nothing TAccess someAccess = (TAccess) result6_black[3];

						final Object[] result7_black = HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_7_ActivityNode4_blackBBBBB(apg, this,
										dataclass, tdataClass, tMember);
						if (result7_black == null) {
							throw new RuntimeException("Pattern matching failed." + " Variables: " + "[apg] = " + apg
									+ ", " + "[this] = " + this + ", " + "[dataclass] = " + dataclass + ", "
									+ "[tdataClass] = " + tdataClass + ", " + "[tMember] = " + tMember + ".");
						}
						final Object[] result7_green = HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_7_ActivityNode4_greenBBFBBB(apg, this,
										dataclass, tdataClass, tMember);
						final HMethodToDataClassAccess metric = (HMethodToDataClassAccess) result7_green[2];

						// ForEach
						for (final Object[] result8_black : HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_8_ActivityNode5_blackFB(tAllMembers)) {
							final TMember dcMember = (TMember) result8_black[0];
							//
							final Object[] result9_black = HMethodToDataClassAccessCalculatorImpl
									.pattern_HMethodToDataClassAccessCalculator_0_9_ActivityNode25_blackFBB(tMember,
											dcMember);
							if (result9_black != null) {
								//nothing TAccess calling = (TAccess) result9_black[0];
								//
								HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_10_ActivityNode20_expressionFB(
										metric);

							} else {
							}

						}
						// ForEach
						for (final Object[] result11_black : HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_11_ActivityNode21_blackFB(
										tAllEffectedMembers)) {
							final TMember dcMember2 = (TMember) result11_black[0];
							//
							final Object[] result12_black = HMethodToDataClassAccessCalculatorImpl
									.pattern_HMethodToDataClassAccessCalculator_0_12_ActivityNode26_blackBFB(tMember,
											dcMember2);
							if (result12_black != null) {
								//nothing TAccess access = (TAccess) result12_black[1];
								//
								HMethodToDataClassAccessCalculatorImpl
								.pattern_HMethodToDataClassAccessCalculator_0_13_ActivityNode6_expressionFB(
										metric);

							} else {
							}

						}

					} else {
					}

				}

			}

		}
		return HMethodToDataClassAccessCalculatorImpl.pattern_HMethodToDataClassAccessCalculator_0_14_expressionF();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<TMember> getAllAffectedMembers(final TClass tClass) {
		// [user code injected with eMoflon]

		final Set<TMember> allMembers = new HashSet<>();
		final Deque<TClass> stack = new LinkedList<>();
		stack.add(tClass);
		while (!stack.isEmpty()) {
			final TClass tNextClass = stack.pop();
			for (final TMember member : tNextClass.getDefines()) {
				TMember redefined;
				if (member instanceof TMethodDefinition) {
					final TMethodDefinition method = (TMethodDefinition) member;
					redefined = method.getOverriding();
				} else if (member instanceof TFieldDefinition) {
					final TFieldDefinition field = (TFieldDefinition) member;
					redefined = field.getHiding();
				} else if (member instanceof TSyntethicMethod) {
					// Ignore synthetic methods
					continue;
				} else {
					throw new RuntimeException("Unknown TMember subtype.");
				}
				if (!allMembers.contains(redefined)) {
					allMembers.add(member);
				}
			}
			stack.addAll(tNextClass.getChildClasses());
		}
		final EList<TMember> container = new BasicEList<>();
		container.addAll(allMembers);
		return container;

	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eInvoke(final int operationID, final EList<?> arguments) throws InvocationTargetException {
		switch (operationID) {
		case CalculatorsPackage.HMETHOD_TO_DATA_CLASS_ACCESS_CALCULATOR___DETECT__HANTIPATTERNGRAPH:
			return detect((HAntiPatternGraph) arguments.get(0));
		case CalculatorsPackage.HMETHOD_TO_DATA_CLASS_ACCESS_CALCULATOR___GET_ALL_AFFECTED_MEMBERS__TCLASS:
			return getAllAffectedMembers((TClass) arguments.get(0));
		}
		return super.eInvoke(operationID, arguments);
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_1_ActivityNode1_blackB(
			final HMethodToDataClassAccessCalculator _this) {
		return new Object[] { _this };
	}

	public static final Iterable<Object[]> pattern_HMethodToDataClassAccessCalculator_0_2_ActivityNode2_blackBFF(
			final HAntiPatternGraph apg) {
		final LinkedList<Object[]> _result = new LinkedList<>();
		for (final HAnnotation tmpBlob : apg.getHAnnotations()) {
			if (tmpBlob instanceof HBlobAntiPattern) {
				final HBlobAntiPattern blob = (HBlobAntiPattern) tmpBlob;
				final TAnnotatable tmpTClass = blob.getTAnnotated();
				if (tmpTClass instanceof TClass) {
					final TClass tClass = (TClass) tmpTClass;
					_result.add(new Object[] { apg, blob, tClass });
				}

			}
		}
		return _result;
	}

	public static final Iterable<Object[]> pattern_HMethodToDataClassAccessCalculator_0_3_ActivityNode3_blackBF(
			final TClass tClass) {
		final LinkedList<Object[]> _result = new LinkedList<>();
		for (final TMember tMember : tClass.getDefines()) {
			_result.add(new Object[] { tClass, tMember });
		}
		return _result;
	}

	public static final Iterable<Object[]> pattern_HMethodToDataClassAccessCalculator_0_4_ActivityNode18_blackBFF(
			final HBlobAntiPattern blob) {
		final LinkedList<Object[]> _result = new LinkedList<>();
		for (final HDataClassSmell dataclass : blob.getHDataClassSmells()) {
			final TAnnotatable tmpTdataClass = dataclass.getTAnnotated();
			if (tmpTdataClass instanceof TClass) {
				final TClass tdataClass = (TClass) tmpTdataClass;
				_result.add(new Object[] { blob, dataclass, tdataClass });
			}

		}
		return _result;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_bindingFFBB(
			final HMethodToDataClassAccessCalculator _this, final TClass tdataClass) {
		final EList<TMember> tAllEffectedMembers = _this.getAllAffectedMembers(tdataClass);
		if (tAllEffectedMembers != null) {
			final EList<TMember> tAllMembers = tdataClass.getAllTMembers();
			if (tAllMembers != null) {
				return new Object[] { tAllEffectedMembers, tAllMembers, _this, tdataClass };
			}
		}
		return null;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_bindingAndBlackFFBB(
			final HMethodToDataClassAccessCalculator _this, final TClass tdataClass) {
		final Object[] result_pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_binding = pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_bindingFFBB(
				_this, tdataClass);
		if (result_pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_binding != null) {
			final EList<TMember> tAllEffectedMembers = (EList<TMember>) result_pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_binding[0];
			final EList<TMember> tAllMembers = (EList<TMember>) result_pattern_HMethodToDataClassAccessCalculator_0_5_ActivityNode13_binding[1];

			if (!tAllEffectedMembers.equals(tAllMembers)) {
				return new Object[] { tAllEffectedMembers, tAllMembers, _this, tdataClass };
			}
		}
		return null;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_6_ActivityNode17_blackBBFF(
			final EList<TMember> tAllMembers, final TMember tMember) {
		for (final TMember someMember : tAllMembers) {
			if (!someMember.equals(tMember)) {
				for (final TAccess someAccess : tMember.getTAccessing()) {
					if (someMember.getAccessedBy().contains(someAccess)) {
						return new Object[] { tAllMembers, tMember, someMember, someAccess };
					}
				}
			}
		}
		return null;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_7_ActivityNode4_blackBBBBB(
			final HAntiPatternGraph apg, final HMethodToDataClassAccessCalculator _this, final HDataClassSmell dataclass,
			final TClass tdataClass, final TMember tMember) {
		return new Object[] { apg, _this, dataclass, tdataClass, tMember };
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_7_ActivityNode4_greenBBFBBB(
			final HAntiPatternGraph apg, final HMethodToDataClassAccessCalculator _this, final HDataClassSmell dataclass,
			final TClass tdataClass, final TMember tMember) {
		final HMethodToDataClassAccess metric = RefactoringgraphFactory.eINSTANCE.createHMethodToDataClassAccess();
		apg.getHAnnotations().add(metric);
		_this.getHAnnotation().add(metric);
		metric.setTAnnotated(tMember);
		metric.setHDataClassSmell(dataclass);
		metric.setHDataClass(tdataClass);
		metric.setTMethodDefinition(tMember);
		return new Object[] { apg, _this, metric, dataclass, tdataClass, tMember };
	}

	public static final Iterable<Object[]> pattern_HMethodToDataClassAccessCalculator_0_8_ActivityNode5_blackFB(
			final EList<TMember> tAllMembers) {
		final LinkedList<Object[]> _result = new LinkedList<>();
		for (final TMember dcMember : tAllMembers) {
			_result.add(new Object[] { dcMember, tAllMembers });
		}
		return _result;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_9_ActivityNode25_blackFBB(final TMember tMember,
			final TMember dcMember) {
		if (!dcMember.equals(tMember)) {
			for (final TAccess calling : tMember.getTAccessing()) {
				if (dcMember.getAccessedBy().contains(calling)) {
					return new Object[] { calling, tMember, dcMember };
				}
			}
		}
		return null;
	}

	public static final double pattern_HMethodToDataClassAccessCalculator_0_10_ActivityNode20_expressionFB(
			final HMethodToDataClassAccess metric) {
		final double _localVariable_0 = metric.increment();
		final double _result = _localVariable_0;
		return _result;
	}

	public static final Iterable<Object[]> pattern_HMethodToDataClassAccessCalculator_0_11_ActivityNode21_blackFB(
			final EList<TMember> tAllEffectedMembers) {
		final LinkedList<Object[]> _result = new LinkedList<>();
		for (final TMember dcMember2 : tAllEffectedMembers) {
			_result.add(new Object[] { dcMember2, tAllEffectedMembers });
		}
		return _result;
	}

	public static final Object[] pattern_HMethodToDataClassAccessCalculator_0_12_ActivityNode26_blackBFB(
			final TMember tMember, final TMember dcMember2) {
		if (!dcMember2.equals(tMember)) {
			for (final TAccess access : tMember.getAccessedBy()) {
				if (dcMember2.getTAccessing().contains(access)) {
					return new Object[] { tMember, access, dcMember2 };
				}
			}
		}
		return null;
	}

	public static final double pattern_HMethodToDataClassAccessCalculator_0_13_ActivityNode6_expressionFB(
			final HMethodToDataClassAccess metric) {
		final double _localVariable_0 = metric.increment();
		final double _result = _localVariable_0;
		return _result;
	}

	public static final boolean pattern_HMethodToDataClassAccessCalculator_0_14_expressionF() {
		final boolean _result = Boolean.TRUE;
		return _result;
	}

	// <-- [user code injected with eMoflon]

	@Override
	public String getGuiName() {
		return "Calculate the Method accesses from Blob to DataClass";
	}

	// [user code injected with eMoflon] -->
} //HMethodToDataClassAccessCalculatorImpl
