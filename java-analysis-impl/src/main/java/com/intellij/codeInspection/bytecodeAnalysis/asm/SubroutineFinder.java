// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.bytecodeAnalysis.asm;

import consulo.internal.org.objectweb.asm.Opcodes;
import consulo.internal.org.objectweb.asm.tree.*;
import consulo.internal.org.objectweb.asm.tree.analysis.AnalyzerException;

import java.util.List;

abstract class SubroutineFinder implements Opcodes
{
	InsnList insns;
	List<TryCatchBlockNode>[] handlers;
	Subroutine[] subroutines;
	int n;

	void findSubroutine(int insn, Subroutine sub, List<AbstractInsnNode> calls) throws AnalyzerException
	{
		while(true)
		{
			if(insn < 0 || insn >= n)
			{
				throw new AnalyzerException(null, "Execution can fall off end of the code");
			}
			if(subroutines[insn] != null)
			{
				return;
			}
			subroutines[insn] = sub.copy();
			AbstractInsnNode node = insns.get(insn);

			// calls findSubroutine recursively on normal successors
			if(node instanceof JumpInsnNode)
			{
				if(node.getOpcode() == JSR)
				{
					// do not follow a JSR, it leads to another subroutine!
					calls.add(node);
				}
				else
				{
					JumpInsnNode jNode = (JumpInsnNode) node;
					findSubroutine(insns.indexOf(jNode.label), sub, calls);
				}
			}
			else if(node instanceof TableSwitchInsnNode)
			{
				TableSwitchInsnNode tsNode = (TableSwitchInsnNode) node;
				findSubroutine(insns.indexOf(tsNode.dflt), sub, calls);
				for(int i = tsNode.labels.size() - 1; i >= 0; --i)
				{
					LabelNode l = tsNode.labels.get(i);
					findSubroutine(insns.indexOf(l), sub, calls);
				}
			}
			else if(node instanceof LookupSwitchInsnNode)
			{
				LookupSwitchInsnNode lsNode = (LookupSwitchInsnNode) node;
				findSubroutine(insns.indexOf(lsNode.dflt), sub, calls);
				for(int i = lsNode.labels.size() - 1; i >= 0; --i)
				{
					LabelNode l = lsNode.labels.get(i);
					findSubroutine(insns.indexOf(l), sub, calls);
				}
			}

			// calls findSubroutine recursively on exception handler successors
			List<TryCatchBlockNode> insnHandlers = handlers[insn];
			if(insnHandlers != null)
			{
				for(TryCatchBlockNode tcb : insnHandlers)
				{
					findSubroutine(insns.indexOf(tcb.handler), sub, calls);
				}
			}

			// if insn does not falls through to the next instruction, return.
			switch(node.getOpcode())
			{
				case GOTO:
				case RET:
				case TABLESWITCH:
				case LOOKUPSWITCH:
				case IRETURN:
				case LRETURN:
				case FRETURN:
				case DRETURN:
				case ARETURN:
				case RETURN:
				case ATHROW:
					return;
			}
			insn++;
		}
	}
}