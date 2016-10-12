package bjoern.plugins.vsa.transformer.esil.commands;

import bjoern.plugins.vsa.domain.ValueSet;

public class MulCommand extends ArithmeticCommand {
	@Override
	protected ValueSet execute(ValueSet leftOperand, ValueSet rightOperand) {
		return leftOperand.mul(rightOperand);
	}
}
