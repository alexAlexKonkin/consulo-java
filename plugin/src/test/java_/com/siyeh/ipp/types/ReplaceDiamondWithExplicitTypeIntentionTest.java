/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ipp.types;

import com.siyeh.IntentionPowerPackBundle;
import com.siyeh.ipp.IPPTestCase;

public abstract class ReplaceDiamondWithExplicitTypeIntentionTest extends IPPTestCase {
  public void testAnonymousClass() {
    doTest();
  }

  public void testApplicableDiamond() {
    doTest();
  }

  public void testApplicableDiamondCheckFormatting() {
    doTest();
  }

  public void testExplicitTypeArgs() {
    doTest();
  }

  @Override
  protected String getIntentionName() {
    return IntentionPowerPackBundle.message("replace.diamond.with.explicit.type.arguments.intention.name");
  }

  @Override
  protected String getRelativePath() {
    return "types/diamond2explicit";
  }
}