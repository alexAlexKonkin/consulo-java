// "Create Field for Parameter 'name'" "true"

package codeInsight.createFieldFromParameterAction.test1;

import javax.annotation.Nullable;

public class TestBefore {

    @Nullable
    private final String myName;

    public TestBefore(@Nullable String name) {
        super();
        myName = name;
    }
}