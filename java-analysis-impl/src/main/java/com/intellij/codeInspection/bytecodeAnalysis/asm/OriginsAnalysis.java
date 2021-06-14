// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.bytecodeAnalysis.asm;

import consulo.internal.org.objectweb.asm.Opcodes;
import consulo.internal.org.objectweb.asm.tree.AbstractInsnNode;
import consulo.internal.org.objectweb.asm.tree.InsnList;
import consulo.internal.org.objectweb.asm.tree.analysis.*;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * @author lambdamix
 */
public class OriginsAnalysis
{
	private static final SourceInterpreter ourInterpreter = new SourceInterpreter(Opcodes.API_VERSION)
	{
		@Override
		public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value)
		{
			return value;
		}
	};

	private static class PreValue extends SourceValue
	{
		final boolean local;
		final int slot;

		PreValue(boolean local, int slot, int size)
		{
			super(size);
			this.local = local;
			this.slot = slot;
		}
	}

	private static class Location
	{
		final boolean local;
		final int slot;

		Location(boolean local, int slot)
		{
			this.local = local;
			this.slot = slot;
		}
	}

	private static class InsnLocation extends Location
	{
		final int insnIndex;

		private InsnLocation(boolean local, int insnIndex, int slot)
		{
			super(local, slot);
			this.insnIndex = insnIndex;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(!(o instanceof InsnLocation))
			{
				return false;
			}
			InsnLocation insnLocation = (InsnLocation) o;
			if(local != insnLocation.local)
			{
				return false;
			}
			if(insnIndex != insnLocation.insnIndex)
			{
				return false;
			}
			if(slot != insnLocation.slot)
			{
				return false;
			}
			return true;
		}

		@Override
		public int hashCode()
		{
			// +1 is to avoid collisions
			int result = 31 * insnIndex + slot + 1;
			return local ? result : -result;
		}
	}

	/**
	 * @param frames       fix point of frames
	 * @param instructions method instructions
	 * @param graph        method control flow graph
	 * @return array, array[i] == true means that the result of a method execution may originate at an i-th instruction
	 * @throws AnalyzerException
	 */
	@Nonnull
	public static boolean[] resultOrigins(Frame<? extends Value>[] frames, InsnList instructions, ControlFlowGraph graph)
			throws AnalyzerException
	{
		IntList[] backTransitions = new IntList[instructions.size()];
		for(int i = 0; i < backTransitions.length; i++)
		{
			backTransitions[i] = IntLists.newArrayList();
		}
		LinkedList<InsnLocation> queue = new LinkedList<>();
		HashSet<InsnLocation> queued = new HashSet<>();
		for(int from = 0; from < instructions.size(); from++)
		{
			for(int to : graph.transitions[from])
			{
				IntList froms = backTransitions[to];
				froms.add(from);
				int opcode = instructions.get(to).getOpcode();
				if(opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN)
				{
					InsnLocation sourceLoc = new InsnLocation(false, from, frames[to].getStackSize() - 1);
					if(queued.add(sourceLoc))
					{
						queue.push(sourceLoc);
					}
				}
			}
		}

		boolean[] result = new boolean[instructions.size()];

		while(!queue.isEmpty())
		{
			InsnLocation resultLocation = queue.pop();

			int insnIndex = resultLocation.insnIndex;
			AbstractInsnNode insn = instructions.get(insnIndex);
			int opcode = insn.getOpcode();
			Location preLocation = previousLocation(frames[insnIndex], resultLocation, insn);
			if(preLocation == null)
			{
				if(opcode != Opcodes.INVOKEINTERFACE && opcode != Opcodes.GETFIELD && !(opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD))
				{
					result[insnIndex] = true;
				}
			}
			else
			{
				IntList froms = backTransitions[insnIndex];
				for(int i = 0; i < froms.size(); i++)
				{
					InsnLocation preILoc = new InsnLocation(preLocation.local, froms.get(i), preLocation.slot);
					if(queued.add(preILoc))
					{
						queue.push(preILoc);
					}
				}
			}
		}

		return result;
	}

	/**
	 * @param frame    a start frame with an interesting value
	 * @param location location of an interesting value *after* execution of an instruction
	 * @param insn     an executed instruction
	 * @return location of an interesting value *before* execution of an instruction (in the past) or null if it is not traceable
	 * @throws AnalyzerException
	 */
	@Nullable
	private static Location previousLocation(Frame<? extends Value> frame, Location location, AbstractInsnNode insn) throws AnalyzerException
	{
		int insnType = insn.getType();
		if(insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE || insnType == AbstractInsnNode.FRAME)
		{
			return location;
		}
		int opCode = insn.getOpcode();
		if(location.local && !((opCode >= Opcodes.ISTORE && opCode <= Opcodes.ASTORE) || opCode == Opcodes.IINC))
		{
			return location;
		}
		Frame<SourceValue> preFrame = makePreFrame(frame);
		preFrame.execute(insn, ourInterpreter);
		if(location.local)
		{
			SourceValue preVal = preFrame.getLocal(location.slot);
			if(preVal instanceof PreValue)
			{
				PreValue val = (PreValue) preVal;
				return new Location(val.local, val.slot);
			}
		}
		else
		{
			SourceValue preVal = preFrame.getStack(location.slot);
			if(preVal instanceof PreValue)
			{
				PreValue val = (PreValue) preVal;
				return new Location(val.local, val.slot);
			}
		}
		return null;
	}

	@Nonnull
	private static Frame<SourceValue> makePreFrame(@Nonnull Frame<? extends Value> frame)
	{
		Frame<SourceValue> preFrame = new Frame<>(frame.getLocals(), frame.getMaxStackSize());
		for(int i = 0; i < frame.getLocals(); i++)
		{
			preFrame.setLocal(i, new PreValue(true, i, frame.getLocal(i).getSize()));
		}
		for(int i = 0; i < frame.getStackSize(); i++)
		{
			preFrame.push(new PreValue(false, i, frame.getStack(i).getSize()));
		}
		return preFrame;
	}
}