package org.gravity.modisco.processing.fwd;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.modisco.java.AbstractMethodDeclaration;
import org.eclipse.modisco.java.AbstractMethodInvocation;
import org.eclipse.modisco.java.AbstractTypeDeclaration;
import org.eclipse.modisco.java.AbstractTypeQualifiedExpression;
import org.eclipse.modisco.java.AbstractVariablesContainer;
import org.eclipse.modisco.java.AnonymousClassDeclaration;
import org.eclipse.modisco.java.ArrayAccess;
import org.eclipse.modisco.java.Assignment;
import org.eclipse.modisco.java.BodyDeclaration;
import org.eclipse.modisco.java.CastExpression;
import org.eclipse.modisco.java.ClassInstanceCreation;
import org.eclipse.modisco.java.ConditionalExpression;
import org.eclipse.modisco.java.ConstructorDeclaration;
import org.eclipse.modisco.java.ConstructorInvocation;
import org.eclipse.modisco.java.EnumConstantDeclaration;
import org.eclipse.modisco.java.Expression;
import org.eclipse.modisco.java.FieldAccess;
import org.eclipse.modisco.java.FieldDeclaration;
import org.eclipse.modisco.java.InfixExpression;
import org.eclipse.modisco.java.MethodDeclaration;
import org.eclipse.modisco.java.MethodInvocation;
import org.eclipse.modisco.java.ParenthesizedExpression;
import org.eclipse.modisco.java.SingleVariableAccess;
import org.eclipse.modisco.java.SingleVariableDeclaration;
import org.eclipse.modisco.java.StringLiteral;
import org.eclipse.modisco.java.SuperConstructorInvocation;
import org.eclipse.modisco.java.SuperFieldAccess;
import org.eclipse.modisco.java.SuperMethodInvocation;
import org.eclipse.modisco.java.ThisExpression;
import org.eclipse.modisco.java.Type;
import org.eclipse.modisco.java.TypeAccess;
import org.eclipse.modisco.java.TypeLiteral;
import org.eclipse.modisco.java.UnresolvedItemAccess;
import org.eclipse.modisco.java.UnresolvedMethodDeclaration;
import org.eclipse.modisco.java.VariableDeclaration;
import org.eclipse.modisco.java.VariableDeclarationFragment;
import org.eclipse.osgi.util.NLS;
import org.gravity.eclipse.exceptions.ProcessingException;
import org.gravity.modisco.MAbstractMethodDefinition;
import org.gravity.modisco.MDefinition;
import org.gravity.modisco.MGravityModel;
import org.gravity.modisco.MMethodDefinition;
import org.gravity.modisco.MMethodInvocation;
import org.gravity.modisco.Messages;
import org.gravity.modisco.processing.AbstractTypedModiscoProcessor;
import org.gravity.modisco.util.MoDiscoUtil;
import org.gravity.modisco.util.NameUtil;

/**
 *
 * A Preprocessor for resolving the static type on an access
 *
 */
public class StaticTypePreprocessing extends AbstractTypedModiscoProcessor<MAbstractMethodDefinition> {

	private static final Logger LOGGER = Logger.getLogger(StaticTypePreprocessing.class);

	private MGravityModel model;

	@Override
	public boolean process(MGravityModel model, Collection<MAbstractMethodDefinition> elements,
			IProgressMonitor monitor) {
		this.model = model;
		final List<MAbstractMethodDefinition> failed = elements.parallelStream()
				.filter(definition -> !addStaticTypeAccesses(definition)).collect(Collectors.toList());
		if (failed.isEmpty()) {
			return true;
		}
		failed.forEach(definition -> LOGGER
				.error(NLS.bind(Messages.errorStaticTypeFailed, definition.getName())));
		return false;
	}

	@Override
	public Class<MAbstractMethodDefinition> getSupportedType() {
		return MAbstractMethodDefinition.class;
	}

	/**
	 * adds static invocation types to all accesses in the method
	 *
	 * @param method The method
	 * @return if the method has been processed sucessfully
	 */
	private boolean addStaticTypeAccesses(MAbstractMethodDefinition method) {
		for (final AbstractMethodInvocation methodInvoc : method.getMMethodInvocations()) {
			if (!(methodInvoc instanceof MMethodInvocation)) {
				continue;
			}
			Type type;
			try {
				type = getStaticType(methodInvoc, method);
			} catch (final ProcessingException e) {
				LOGGER.error(e.getMessage(), e);
				return false;
			}
			if (type == null && !methodInvoc.getMethod().isProxy()) {
				return false;
			}
			((MMethodInvocation) methodInvoc).setMStaticType(type);
		}
		return true;
	}

	/**
	 * Searches the static type of the object used in a method invocation in a
	 * calling method
	 *
	 * @param methodInvoc The method invocation
	 * @param method      The calling method
	 * @return The static type of the object on which the invoked method is called
	 * @throws ProcessingException
	 */
	private Type getStaticType(AbstractMethodInvocation methodInvoc, MAbstractMethodDefinition method)
			throws ProcessingException {
		Type type = null;
		if (methodInvoc instanceof MethodInvocation) {
			type = getStaticType(((MethodInvocation) methodInvoc).getExpression(), method);
			if (type == null) {
				if (LOGGER.isEnabledFor(Level.WARN)) {
					LOGGER.warn(NLS.bind(Messages.errorFindStaticType, new String[] {NameUtil.getFullyQualifiedName(methodInvoc.getMethod()), NameUtil.getFullyQualifiedName(method)}));
				}
				// If we cannot find the static type assume the declaring type
				type = getDeclaringType(methodInvoc.getMethod());
			}

		} else if (methodInvoc instanceof SuperMethodInvocation) {
			// super method invoc cannot happen with a qualifier other than "this"
			// => static type is always the type that defines this method
			type = getDeclaringType(method);
		} else if (methodInvoc instanceof ClassInstanceCreation) {
			type = getStaticType(((ClassInstanceCreation) methodInvoc).getExpression(), method);
		} else if (methodInvoc instanceof ConstructorInvocation) {
			// ConstructorInvocation : this()
			// => does not have a qualifier
			// => static type is always type that defines this method
			type = method.getAbstractTypeDeclaration();
		} else if (methodInvoc instanceof SuperConstructorInvocation) {
			// seems to never happen?..
			LOGGER.log(Level.ERROR,
					Messages.unsupportedSuperConstructorInvocation);
			throw new ProcessingException(methodInvoc);
		} else {
			LOGGER.error(NLS.bind(Messages.unknownInvocationType, methodInvoc.getClass().getName()));
			throw new ProcessingException(methodInvoc);
		}
		return type;
	}

	/**
	 * Tries to resolve the static type of an access to a variable
	 *
	 * @param expression The variable access expression
	 * @param method     The method containing the access
	 * @return The static type
	 * @throws ProcessingException
	 */
	private Type getStaticType(SingleVariableAccess expression, MAbstractMethodDefinition method)
			throws ProcessingException {
		final VariableDeclaration var = expression.getVariable();
		if (var == null) {
			/*
			 * Handling of constructs not supported by MoDisco like:
			 *
			 * try(Scanner s = new Scanner("")){ ... s.nextInt(); ... }
			 *
			 * Assume static type to be type in which a called method has been defined.
			 */
			final EObject container = expression.eContainer();
			if (container instanceof MethodInvocation) {
				return ((MethodInvocation) container).getMethod().getAbstractTypeDeclaration();
			} else {
				throw new ProcessingException(Messages.preprocessingOfUnknown);
			}
		} else if (var instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration) var).getType().getType();
		} else if (var instanceof VariableDeclarationFragment) {
			final AbstractVariablesContainer container = ((VariableDeclarationFragment) var).getVariablesContainer();
			final TypeAccess access = container.getType();
			if (access != null) {
				return access.getType();
			}
		} else if (var instanceof EnumConstantDeclaration) {
			return getStaticType(expression.getQualifier(), method);
		} else if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn(NLS.bind(Messages.unknownVarDecl, var.getClass().getName()));
		}
		return getStaticTypeFromInitializer(var.getInitializer());
	}

	/**
	 * Tries to guess the static type of a variable from the initializer
	 *
	 * @param initializer The expression used as initializer
	 * @return The possible static type of the variable
	 */
	private Type getStaticTypeFromInitializer(Expression initializer) {
		if (initializer != null) {
			if (initializer instanceof MethodInvocation) {
				final AbstractMethodDeclaration targetMethod = ((MethodInvocation) initializer).getMethod();
				if (targetMethod instanceof MMethodDefinition) {
					return ((MMethodDefinition) targetMethod).getReturnType().getType();
				}
			}

			if (initializer instanceof ClassInstanceCreation) {
				return ((ClassInstanceCreation) initializer).getType().getType();
			}
			if (initializer instanceof CastExpression) {
				return ((CastExpression) initializer).getType().getType();
			}
		}
		return null;
	}

	/**
	 * Tries to resolve the static type of an access contained in an expression
	 *
	 * @param expression The expression
	 * @param method     The method containing the access
	 * @return The static type
	 * @throws ProcessingException
	 */
	private Type getStaticType(Expression expression, MAbstractMethodDefinition method) throws ProcessingException {
		if (expression == null) {
			// If the access is not contained in an expression the access is to a direct
			// member of the type declaring the method
			return getDeclaringType(method);
		}
		if (expression instanceof TypeAccess) {
			return ((TypeAccess) expression).getType();
		}
		if (expression instanceof FieldAccess) {
			return getStaticType(((FieldAccess) expression).getExpression(), method);
		}
		if (expression instanceof SingleVariableAccess) {
			return getStaticType((SingleVariableAccess) expression, method);
		}
		if (expression instanceof ParenthesizedExpression) {
			return getStaticType(((ParenthesizedExpression) expression).getExpression(), method);
		}
		if (expression instanceof AbstractTypeQualifiedExpression) {
			return getStaticType((AbstractTypeQualifiedExpression) expression, method);
		}
		if (expression instanceof ClassInstanceCreation) {
			return ((ClassInstanceCreation) expression).getType().getType();
		}
		if (expression instanceof StringLiteral) {
			return MoDiscoUtil.getOrCreateJavaLangString(this.model);
		}
		if (expression instanceof Assignment) {
			return getStaticType(((Assignment) expression).getLeftHandSide(), method);
		}
		if (expression instanceof CastExpression) {
			return ((CastExpression) expression).getType().getType();
		}
		if (expression instanceof TypeLiteral) {
			return ((TypeLiteral) expression).getType().getType();
		}
		if (expression instanceof AbstractVariablesContainer) {
			return ((AbstractVariablesContainer) expression).getType().getType();
		}
		if (expression instanceof ArrayAccess) {
			return getStaticType(((ArrayAccess) expression).getArray(), method);
		}
		if (expression instanceof InfixExpression) {
			return getStaticType((InfixExpression) expression, method);
		}
		if (expression instanceof ConditionalExpression) {
			return getStaticType(((ConditionalExpression) expression).getExpression(), method);
		}
		if (expression instanceof UnresolvedItemAccess) {
			return getStaticType((UnresolvedItemAccess) expression, method);
		}
		if (expression instanceof AbstractMethodInvocation) {
			final AbstractMethodDeclaration calledOn = ((AbstractMethodInvocation) expression).getMethod();
			if (calledOn instanceof MethodDeclaration) {
				final TypeAccess returnType = ((MethodDeclaration) calledOn).getReturnType();
				if (returnType == null) {
					if (calledOn instanceof UnresolvedMethodDeclaration) {
						return null;
					}
					throw new ProcessingException();
				}
				return returnType.getType();
			} else if (calledOn instanceof ConstructorDeclaration) {
				return ((ConstructorDeclaration) calledOn).getAbstractTypeDeclaration();
			}
		}
		LOGGER.error(NLS.bind(Messages.unsupportedStaticTypeFromExpressionKind,
				expression.getClass().getSimpleName()));
		return null;
	}

	/**
	 * Tries to resolve the static call type of an unresolved item access
	 *
	 * @param expression The access expression
	 * @param method     The method containing the access
	 * @return The static type
	 */
	private Type getStaticType(UnresolvedItemAccess expression, MAbstractMethodDefinition method) {
		final String nameOfAccessedElement = expression.getElement().getName();
		// Check if the item is a parameter
		for (final SingleVariableDeclaration parameter : method.getParameters()) {
			if (nameOfAccessedElement.equals(parameter.getName())) {
				return parameter.getType().getType();
			}
		}
		// Check is the accessed item is a field
		final Type declaringType = getDeclaringType(method);
		if (declaringType instanceof AbstractTypeDeclaration) {
			final Type type = searchTypeOfFieldWithName(nameOfAccessedElement, (AbstractTypeDeclaration) declaringType);
			if (type != null) {
				return type;
			}
		}
		if (LOGGER.isEnabledFor(Level.WARN)) {
			LOGGER.warn(NLS.bind(Messages.errorResolveStaticTypeOfAccessedUnresolved, expression));
		}
		return null;
	}

	/**
	 * Searches if a type declares a field with a given name and returns its type
	 *
	 * @param name The name of the field
	 * @param type The type containing the field
	 * @return The type of the field or null if there is no field with this name
	 */
	private Type searchTypeOfFieldWithName(String name, AbstractTypeDeclaration type) {
		return type.getBodyDeclarations().parallelStream().filter(body -> body instanceof FieldDeclaration)
				.map(body -> (FieldDeclaration) body)
				.filter(field -> field.getFragments().stream().anyMatch(fragment -> fragment.getName().equals(name)))
				.map(field -> field.getType().getType()).findAny().orElse(null);
	}

	/**
	 * Tries to get the static type of an access contained in an infix expression
	 *
	 * @param expression The infix expression
	 * @param method     The method containing the access
	 * @return The static type
	 * @throws ProcessingException
	 */
	private Type getStaticType(final InfixExpression expression, MAbstractMethodDefinition method)
			throws ProcessingException {
		Type type = getStaticType(expression.getLeftOperand(), method);
		if (type == null) {
			type = getStaticType(expression.getRightOperand(), method);
		}
		return type;
	}

	/**
	 * Tries to get the static type of an super or this access
	 *
	 * @param expression The access
	 * @param method     The method containing the access
	 * @return The static type
	 * @throws ProcessingException If the expression is not supported
	 */
	private Type getStaticType(AbstractTypeQualifiedExpression expression, MAbstractMethodDefinition method)
			throws ProcessingException {
		final TypeAccess qualifier = expression.getQualifier();
		if (qualifier == null) {
			if (expression instanceof ThisExpression) {
				return getDeclaringType(method);
			}
			if (expression instanceof SuperMethodInvocation) {
				return getDeclaringType(((SuperMethodInvocation) expression).getMethod());
			}
			if (expression instanceof SuperFieldAccess) {
				// Dirty hack for super.field access in libs
				return (Type) ((MDefinition) ((SuperFieldAccess) expression).getField().getVariable().eContainer())
						.eContainer();
			}
			throw new ProcessingException(NLS.bind(Messages.unsupportedExpression, expression.eClass().getName()) );
		}
		return qualifier.getType();
	}

	/**
	 * Searches the type declaring this method definition
	 *
	 * @param body The method definition
	 * @return the declaring type
	 */
	private Type getDeclaringType(BodyDeclaration body) {
		Type type = body.getAbstractTypeDeclaration();
		if (type == null) {
			final EObject container = body.eContainer();
			if (container instanceof AnonymousClassDeclaration) {
				type = getDeclaringType((AnonymousClassDeclaration) container);
			} else if (container instanceof AbstractTypeDeclaration) {
				type = (AbstractTypeDeclaration) container;
			} else {
				LOGGER.error(NLS.bind(Messages.unknownDeclaringType, body));
			}
		}
		return type;
	}

	/**
	 * Searches the type declaring this anonymous class
	 *
	 * @param anon The anonymous class
	 * @return the declaring type
	 */
	private Type getDeclaringType(AnonymousClassDeclaration anon) {
		final EObject container = anon.eContainer();
		if (container instanceof ClassInstanceCreation) {
			final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) container;
			final TypeAccess typeAccess = classInstanceCreation.getType();
			if (typeAccess == null) {
				LOGGER.error(NLS.bind(Messages.errorClassInstanceCreationNoType, classInstanceCreation));
				return null;
			}
			return typeAccess.getType();
		} else if (container instanceof EnumConstantDeclaration) {
			final EnumConstantDeclaration enumConst = (EnumConstantDeclaration) container;
			return enumConst.getAbstractTypeDeclaration();
		}
		LOGGER.error(NLS.bind(Messages.unknownContainerAnon, container.eClass().getName()));
		return null;
	}
}
