// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.analysis.impl.codeInspection.dataFlow;

import com.intellij.java.analysis.impl.codeInspection.dataFlow.instructions.*;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaExpressionFactory;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaValue;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.java.analysis.impl.codeInspection.dataFlow.value.DfaVariableValue;
import consulo.application.progress.ProgressManager;
import consulo.util.lang.Pair;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FilteringIterator;
import consulo.util.collection.MultiMap;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import jakarta.annotation.Nonnull;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;

/**
 * @author peter
 */
public final class LiveVariablesAnalyzer {
  private final DfaValueFactory myFactory;
  private final Instruction[] myInstructions;
  private final MultiMap<Instruction, Instruction> myForwardMap;
  private final MultiMap<Instruction, Instruction> myBackwardMap;

  public LiveVariablesAnalyzer(ControlFlow flow, DfaValueFactory factory) {
    myFactory = factory;
    myInstructions = flow.getInstructions();
    myForwardMap = calcForwardMap();
    myBackwardMap = calcBackwardMap();
  }

  private List<Instruction> getSuccessors(Instruction ins) {
    return IntStreamEx.of(LoopAnalyzer.getSuccessorIndices(ins.getIndex(), myInstructions)).elements(myInstructions).toList();
  }

  private MultiMap<Instruction, Instruction> calcBackwardMap() {
    MultiMap<Instruction, Instruction> result = MultiMap.create();
    for (Instruction instruction : myInstructions) {
      for (Instruction next : myForwardMap.get(instruction)) {
        result.putValue(next, instruction);
      }
    }
    return result;
  }

  private MultiMap<Instruction, Instruction> calcForwardMap() {
    MultiMap<Instruction, Instruction> result = MultiMap.create();
    for (Instruction instruction : myInstructions) {
      if (isInterestingInstruction(instruction)) {
        for (Instruction next : getSuccessors(instruction)) {
          while (true) {
            if (isInterestingInstruction(next)) {
              result.putValue(instruction, next);
              break;
            }
            if (next.getIndex() + 1 >= myInstructions.length) {
              break;
            }
            next = myInstructions[next.getIndex() + 1];
          }
        }
      }
    }
    return result;
  }

  @Nullable
  private static DfaVariableValue getWrittenVariable(Instruction instruction) {
    if (instruction instanceof AssignInstruction) {
      DfaValue value = ((AssignInstruction) instruction).getAssignedValue();
      return value instanceof DfaVariableValue ? (DfaVariableValue) value : null;
    }
    if (instruction instanceof FlushVariableInstruction) {
      return ((FlushVariableInstruction) instruction).getVariable();
    }
    return null;
  }

  @Nonnull
  private static List<DfaVariableValue> getReadVariables(Instruction instruction) {
    if (instruction instanceof PushInstruction && !((PushInstruction) instruction).isReferenceWrite()) {
      DfaValue value = ((PushInstruction) instruction).getValue();
      if (value instanceof DfaVariableValue) {
        return Collections.singletonList((DfaVariableValue) value);
      }
    } else if (instruction instanceof EscapeInstruction) {
      return StreamEx.of(((EscapeInstruction) instruction).getEscapedVars())
          .flatMap(v -> StreamEx.of(v.getDependentVariables()).prepend(v))
          .distinct().toList();
    }
    return Collections.emptyList();
  }

  private boolean isInterestingInstruction(Instruction instruction) {
    if (instruction == myInstructions[0]) {
      return true;
    }
    if (!getReadVariables(instruction).isEmpty() || getWrittenVariable(instruction) != null) {
      return true;
    }
    return instruction instanceof FinishElementInstruction ||
        instruction instanceof GotoInstruction ||
        instruction instanceof ConditionalGotoInstruction ||
        instruction instanceof ControlTransferInstruction;
  }

  @Nullable
  private Map<FinishElementInstruction, BitSet> findLiveVars() {
    final Map<FinishElementInstruction, BitSet> result = new HashMap<>();

    boolean ok = runDfa(false, (instruction, liveVars) -> {
      if (instruction instanceof FinishElementInstruction) {
        BitSet set = result.get(instruction);
        if (set != null) {
          set.or(liveVars);
          return (BitSet) set.clone();
        } else if (!liveVars.isEmpty()) {
          result.put((FinishElementInstruction) instruction, (BitSet) liveVars.clone());
        }
      }

      DfaVariableValue written = getWrittenVariable(instruction);
      if (written != null) {
        liveVars = (BitSet) liveVars.clone();
        liveVars.clear(written.getID());
        for (DfaVariableValue var : written.getDependentVariables()) {
          liveVars.clear(var.getID());
        }
      } else {
        boolean cloned = false;
        for (DfaVariableValue value : getReadVariables(instruction)) {
          if (!liveVars.get(value.getID())) {
            if (!cloned) {
              liveVars = (BitSet) liveVars.clone();
              cloned = true;
            }
            liveVars.set(value.getID());
          }
        }
      }

      return liveVars;
    });
    return ok ? result : null;
  }

  void flushDeadVariablesOnStatementFinish() {
    final Map<FinishElementInstruction, BitSet> liveVars = findLiveVars();
    if (liveVars == null) {
      return;
    }

    final MultiMap<FinishElementInstruction, DfaVariableValue> toFlush = MultiMap.createSet();

    boolean ok = runDfa(true, (instruction, prevLiveVars) -> {
      if (instruction instanceof FinishElementInstruction) {
        BitSet currentlyLive = liveVars.get(instruction);
        if (currentlyLive == null) {
          currentlyLive = new BitSet();
        }
        int index = 0;
        while (true) {
          int setBit = prevLiveVars.nextSetBit(index);
          if (setBit < 0) {
            break;
          }
          if (!currentlyLive.get(setBit)) {
            toFlush.putValue((FinishElementInstruction) instruction, (DfaVariableValue) myFactory.getValue(setBit));
          }
          index = setBit + 1;
        }
        return currentlyLive;
      }

      return prevLiveVars;
    });

    if (ok) {
      for (FinishElementInstruction instruction : toFlush.keySet()) {
        Collection<DfaVariableValue> values = toFlush.get(instruction);
        // Do not flush special values and this value as they could be used implicitly
        // Assertions disabled variable may be used from CommonDataflow
        values.removeIf(var -> var.getDescriptor() instanceof SpecialField ||
            var.getDescriptor() instanceof DfaExpressionFactory.ThisDescriptor ||
            var.getDescriptor() instanceof DfaExpressionFactory.AssertionDisabledDescriptor);
        instruction.getVarsToFlush().addAll(values);
      }
    }
  }

  /**
   * @return true if completed, false if "too complex"
   */
  private boolean runDfa(boolean forward, BiFunction<Instruction, BitSet, BitSet> handleState) {
    Set<Instruction> entryPoints = new HashSet<>();
    if (forward) {
      entryPoints.add(myInstructions[0]);
    } else {
      entryPoints.addAll(ContainerUtil.findAll(myInstructions, FilteringIterator.instanceOf(ReturnInstruction.class)));
    }

    Deque<InstructionState> queue = new ArrayDeque<>(10);
    for (Instruction i : entryPoints) {
      queue.addLast(new InstructionState(i, new BitSet()));
    }

    int limit = myForwardMap.size() * 100;
    Map<BitSet, IntSet> processed = new HashMap<>();
    int steps = 0;
    while (!queue.isEmpty()) {
      if (steps > limit) {
        return false;
      }
      if (steps % 1024 == 0) {
        ProgressManager.checkCanceled();
      }
      InstructionState state = queue.removeFirst();
      Instruction instruction = state.first;
      Collection<Instruction> nextInstructions = forward ? myForwardMap.get(instruction) : myBackwardMap.get(instruction);
      BitSet nextVars = handleState.apply(instruction, state.second);
      for (Instruction next : nextInstructions) {
        IntSet instructionSet = processed.computeIfAbsent(nextVars, k -> IntSets.newHashSet());
        int index = next.getIndex() + 1;
        if (!instructionSet.contains(index)) {
          instructionSet.add(index);
          queue.addLast(new InstructionState(next, nextVars));
          steps++;
        }
      }
    }
    return true;
  }
}

class InstructionState extends Pair<Instruction, BitSet> {
  InstructionState(Instruction first, BitSet second) {
    super(first, second);
  }
}