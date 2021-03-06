/*
Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.google.security.zynamics.reil.translators.arm;

import com.google.security.zynamics.reil.OperandSize;
import com.google.security.zynamics.reil.ReilHelpers;
import com.google.security.zynamics.reil.ReilInstruction;
import com.google.security.zynamics.reil.translators.ITranslationEnvironment;
import com.google.security.zynamics.reil.translators.InternalTranslationException;
import com.google.security.zynamics.reil.translators.TranslationHelpers;
import com.google.security.zynamics.reil.translators.mips.Helpers;
import com.google.security.zynamics.zylib.disassembly.IInstruction;
import com.google.security.zynamics.zylib.general.Pair;

import java.util.List;

public class ARMSdivTranslator extends ARMBaseTranslator {
  @Override
  protected void translateCore(final ITranslationEnvironment environment,
      final IInstruction instruction, final List<ReilInstruction> instructions) {
    final String destination =
        instruction.getOperands().get(0).getRootNode().getChildren().get(0).getValue();
    final String dividend =
        instruction.getOperands().get(1).getRootNode().getChildren().get(0).getValue();
    final String divisor =
        instruction.getOperands().get(2).getRootNode().getChildren().get(0).getValue();

    final OperandSize dw = OperandSize.DWORD;

    long baseOffset = ReilHelpers.nextReilAddress(instruction, instructions);

    final Pair<String, String> sourceRegister1Abs =
        Helpers.generateAbs(environment, baseOffset, dividend, dw, instructions);
    final String sourceRegister1Absolute = sourceRegister1Abs.second();
    baseOffset = ReilHelpers.nextReilAddress(instruction, instructions);

    final Pair<String, String> sourceRegister2Abs =
        Helpers.generateAbs(environment, baseOffset, divisor, dw, instructions);
    final String sourceRegister2Absolute = sourceRegister2Abs.second();
    baseOffset = ReilHelpers.nextReilAddress(instruction, instructions);

    final String divResult = environment.getNextVariableString();

    instructions.add(ReilHelpers.createDiv(baseOffset++, dw, sourceRegister1Absolute, dw,
        sourceRegister2Absolute, dw, divResult));

    final String xoredSigns = environment.getNextVariableString();
    final String divToggleMask = environment.getNextVariableString();
    final String xoredDivResult = environment.getNextVariableString();

    // Find out if the two operands had different signs and adjust the result accordingly
    instructions.add(ReilHelpers.createXor(baseOffset++, dw, sourceRegister1Abs.first(), dw,
        sourceRegister2Abs.first(), dw, xoredSigns));
    instructions.add(ReilHelpers.createSub(baseOffset++, dw, String.valueOf(0L), dw, xoredSigns, dw,
        divToggleMask));
    instructions.add(ReilHelpers.createXor(baseOffset++, dw, divToggleMask, dw, divResult, dw,
        xoredDivResult));
    instructions.add(ReilHelpers.createAdd(baseOffset++, dw, xoredDivResult, dw, xoredSigns, dw,
        destination));
  }

  @Override
  public void translate(final ITranslationEnvironment environment, final IInstruction instruction,
      final List<ReilInstruction> instructions) throws InternalTranslationException {
    TranslationHelpers.checkTranslationArguments(environment, instruction, instructions, "ORN");
    translateAll(environment, instruction, "ORN", instructions);
  }
}
