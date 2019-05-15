package org.gravity.modisco.dataflow;

import org.eclipse.gmt.modisco.java.AssertStatement;
import org.eclipse.gmt.modisco.java.Block;
import org.eclipse.gmt.modisco.java.BreakStatement;
import org.eclipse.gmt.modisco.java.CatchClause;
import org.eclipse.gmt.modisco.java.ConstructorInvocation;
import org.eclipse.gmt.modisco.java.ContinueStatement;
import org.eclipse.gmt.modisco.java.DoStatement;
import org.eclipse.gmt.modisco.java.EmptyStatement;
import org.eclipse.gmt.modisco.java.EnhancedForStatement;
import org.eclipse.gmt.modisco.java.Expression;
import org.eclipse.gmt.modisco.java.ExpressionStatement;
import org.eclipse.gmt.modisco.java.ForStatement;
import org.eclipse.gmt.modisco.java.IfStatement;
import org.eclipse.gmt.modisco.java.LabeledStatement;
import org.eclipse.gmt.modisco.java.ReturnStatement;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.gmt.modisco.java.SuperConstructorInvocation;
import org.eclipse.gmt.modisco.java.SwitchCase;
import org.eclipse.gmt.modisco.java.SwitchStatement;
import org.eclipse.gmt.modisco.java.SynchronizedStatement;
import org.eclipse.gmt.modisco.java.ThrowStatement;
import org.eclipse.gmt.modisco.java.TryStatement;
import org.eclipse.gmt.modisco.java.TypeDeclarationStatement;
import org.eclipse.gmt.modisco.java.UnresolvedLabeledStatement;
import org.eclipse.gmt.modisco.java.VariableDeclarationFragment;
import org.eclipse.gmt.modisco.java.VariableDeclarationStatement;
import org.eclipse.gmt.modisco.java.WhileStatement;

public class StatementHandlerDataFlow {

	public static FlowNode handle(Statement statement, FlowNode member) {
		if (statement == null) {
			return member; // assume nothing to do is success
		}
		if (statement instanceof AssertStatement) {
			AssertStatement assertStatement = (AssertStatement) statement;
			return handle(assertStatement, member);

		} else if (statement instanceof Block) {
			Block block = (Block) statement;
			return handle(block, member);

		} else if (statement instanceof BreakStatement) {
			BreakStatement breakStatement = (BreakStatement) statement;
			return handle(breakStatement, member);

		} else if (statement instanceof CatchClause) {
			CatchClause catchClause = (CatchClause) statement;
			return handle(catchClause, member);

		} else if (statement instanceof ConstructorInvocation) {
			ConstructorInvocation constructorInvocation = (ConstructorInvocation) statement;
			return handle(constructorInvocation, member);

		} else if (statement instanceof ContinueStatement) {
			ContinueStatement continueStatement = (ContinueStatement) statement;
			return handle(continueStatement, member);

		} else if (statement instanceof DoStatement) {
			DoStatement doStatement = (DoStatement) statement;
			return handle(doStatement, member);

		} else if (statement instanceof EmptyStatement) {
			EmptyStatement emptyStatement = (EmptyStatement) statement;
			return handle(emptyStatement, member);

		} else if (statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStetement = (EnhancedForStatement) statement;
			return handle(enhancedForStetement, member);

		} else if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			return handle(expressionStatement, member);

		} else if (statement instanceof ForStatement) {
			ForStatement forStatement = (ForStatement) statement;
			return handle(forStatement, member);

		} else if (statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement) statement;
			return handle(ifStatement, member);

		} else if (statement instanceof LabeledStatement) {
			LabeledStatement labeledStatement = (LabeledStatement) statement;
			return handle(labeledStatement, member);

		} else if (statement instanceof ReturnStatement) {
			ReturnStatement returnStatement = (ReturnStatement) statement;
			return handle(returnStatement, member);

		} else if (statement instanceof SuperConstructorInvocation) {
			SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation) statement;
			return handle(superConstructorInvocation, member);

		} else if (statement instanceof SwitchCase) {
			SwitchCase switchCase = (SwitchCase) statement;
			return handle(switchCase, member);

		} else if (statement instanceof SwitchStatement) {
			SwitchStatement switchStatement = (SwitchStatement) statement;
			return handle(switchStatement, member);

		} else if (statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
			return handle(synchronizedStatement, member);

		} else if (statement instanceof ThrowStatement) {
			ThrowStatement throwStatement = (ThrowStatement) statement;
			return handle(throwStatement, member);

		} else if (statement instanceof TryStatement) {
			TryStatement tryStatement = (TryStatement) statement;
			return handle(tryStatement, member);

		} else if (statement instanceof TypeDeclarationStatement) {
			TypeDeclarationStatement typeDeclarationStatement = (TypeDeclarationStatement) statement;
			return handle(typeDeclarationStatement, member);

		} else if (statement instanceof UnresolvedLabeledStatement) {
			UnresolvedLabeledStatement unresolvedLabeledStatement = (UnresolvedLabeledStatement) statement;
			return handle(unresolvedLabeledStatement, member);

		} else if (statement instanceof VariableDeclarationStatement) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
			return handle(variableDeclarationStatement, member);

		} else if (statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement) statement;
			return handle(whileStatement, member);

		} else {
			return member;
		}
	}

	private static FlowNode handle(WhileStatement whileStatement, FlowNode member) {
		if (whileStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(whileStatement.getBody(), member);
		ExpressionHandlerDataFlow.handle(whileStatement.getExpression(), member);
		return member;
	}

	// TODO
	private static FlowNode handle(VariableDeclarationStatement variableDeclarationStatement, FlowNode member) {
		if (variableDeclarationStatement == null) {
			return member; // assume nothing to do is success
		}
		for (VariableDeclarationFragment fragment : variableDeclarationStatement.getFragments()) {
			MiscHandlerDataFlow.handle(fragment, member);
		}
		return member;
	}

	private static FlowNode handle(TypeDeclarationStatement typeDeclarationStatement, FlowNode member) {
		if (typeDeclarationStatement == null) {
			return member; // assume nothing to do is success
		}
		return MiscHandlerDataFlow.handle(typeDeclarationStatement.getDeclaration(), member);
	}

	private static FlowNode handle(TryStatement tryStatement, FlowNode member) {
		if (tryStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(tryStatement.getBody(), member);
		handle(tryStatement.getFinally(), member);
		for (CatchClause clause : tryStatement.getCatchClauses()) {
			handle(clause, member);
		}
		return member;
	}

	private static FlowNode handle(ThrowStatement throwStatement, FlowNode member) {
		if (throwStatement == null) {
			return member; // assume nothing to do is success
		}
		ExpressionHandlerDataFlow.handle(throwStatement.getExpression(), member);
		return member;
	}

	private static FlowNode handle(SynchronizedStatement synchronizedStatement, FlowNode member) {
		if (synchronizedStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(synchronizedStatement.getBody(), member);
		ExpressionHandlerDataFlow.handle(synchronizedStatement.getExpression(), member);
		return member;
	}

	private static FlowNode handle(SwitchStatement switchStatement, FlowNode member) {
		if (switchStatement == null) {
			return member; // assume nothing to do is success
		}
		for (Statement statement : switchStatement.getStatements()) {
			handle(statement, member);
		}
		ExpressionHandlerDataFlow.handle(switchStatement.getExpression(), member);
		return member;
	}

	private static FlowNode handle(SwitchCase switchCase, FlowNode member) {
		if (switchCase == null) {
			return member; // assume nothing to do is success
		}
		return ExpressionHandlerDataFlow.handle(switchCase.getExpression(), member);
	}

	private static FlowNode handle(SuperConstructorInvocation superConstructorInvocation, FlowNode member) {
		if (superConstructorInvocation == null) {
			return member; // assume nothing to do is success
		}
		ExpressionHandlerDataFlow.handle(superConstructorInvocation.getExpression(), member);
		for (Expression expression : superConstructorInvocation.getArguments()) {
			ExpressionHandlerDataFlow.handle(expression, member);
		}
		return member;
	}

	// TODO
	private static FlowNode handle(ReturnStatement returnStatement, FlowNode member) {
		if (returnStatement == null) {
			return member; // assume nothing to do is success
		}
		return ExpressionHandlerDataFlow.handle(returnStatement.getExpression(), member);
	}

	private static FlowNode handle(LabeledStatement labeledStatement, FlowNode member) {
		if (labeledStatement == null) {
			return member; // assume nothing to do is success
		}
		return handle(labeledStatement.getBody(), member);
	}

	// TODO
	private static FlowNode handle(IfStatement ifStatement, FlowNode member) {
		if (ifStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(ifStatement.getElseStatement(), member);
		handle(ifStatement.getThenStatement(), member);
		ExpressionHandlerDataFlow.handle(ifStatement.getExpression(), member);
		return member;
	}

	// TODO
	private static FlowNode handle(ForStatement forStatement, FlowNode member) {
		if (forStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(forStatement.getBody(), member);
		ExpressionHandlerDataFlow.handle(forStatement.getExpression(), member);
		for (Expression expression : forStatement.getInitializers()) {
			ExpressionHandlerDataFlow.handle(expression, member);
		}
		for (Expression expression : forStatement.getUpdaters()) {
			ExpressionHandlerDataFlow.handle(expression, member);
		}
		return member;
	}

	private static FlowNode handle(ExpressionStatement expressionStatement, FlowNode member) {
		if (expressionStatement == null) {
			return member; // assume nothing to do is success
		}
		return ExpressionHandlerDataFlow.handle(expressionStatement.getExpression(), member);
	}

	private static FlowNode handle(EnhancedForStatement enhancedForStatement, FlowNode member) {
		if (enhancedForStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(enhancedForStatement.getBody(), member);
		ExpressionHandlerDataFlow.handle(enhancedForStatement.getExpression(), member);
		return member;
	}

	private static FlowNode handle(EmptyStatement emptyStatement, FlowNode member) {
		return member;
	}

	private static FlowNode handle(DoStatement doStatement, FlowNode member) {
		if (doStatement == null) {
			return member; // assume nothing to do is success
		}
		handle(doStatement.getBody(), member);
		ExpressionHandlerDataFlow.handle(doStatement.getExpression(), member);
		return member;
	}

	private static FlowNode handle(ContinueStatement continueStatement, FlowNode member) {
		if (continueStatement == null) {
			return member; // assume nothing to do is success
		}
		return handle(continueStatement.getLabel(), member);
	}

	// TODO
	private static FlowNode handle(ConstructorInvocation constructorInvocation, FlowNode member) {
		if (constructorInvocation == null) {
			return member; // assume nothing to do is success
		}
		for (Expression argument : constructorInvocation.getArguments()) {
			ExpressionHandlerDataFlow.handle(argument, member);
		}
		/*
		if(member.getAbstractMethodInvocations().contains(constructorInvocation)){
			return true;
		}
		if (!member.getAbstractMethodInvocations().add(constructorInvocation)) {
			return false;
		}
		*/
		return member;
	}

	private static FlowNode handle(BreakStatement breakStatement, FlowNode member) {
		if (breakStatement == null) {
			return member; // assume nothing to do is success
		}
		return handle(breakStatement.getLabel(), member);
	}

	private static FlowNode handle(CatchClause catchClause, FlowNode member) {
		if (catchClause == null) {
			return member; // assume nothing to do is success
		}
		handle(catchClause.getBody(), member);
		MiscHandlerDataFlow.handle(catchClause.getException(), member);
		return member;
	}
	
	private static FlowNode handle(AssertStatement assertStatement, FlowNode member) {
		if (assertStatement == null) {
			return member; // assume nothing to do is success
		}
		ExpressionHandlerDataFlow.handle(assertStatement.getExpression(), member);
		ExpressionHandlerDataFlow.handle(assertStatement.getMessage(), member);
		return member;
	}

	private static FlowNode handle(Block block, FlowNode member) {
		if (block == null) {
			return member; // assume nothing to do is success
		}
		// TODO: Needs to be handled differently; new member for each statement
		for (Statement statement : block.getStatements()) {
			handle(statement, member);
		}
		return member;
	}

}