// "Bind Constructor Parameters to Fields" "true"

import javax.annotation.Nullable;

public class TestBefore {

    @Nullable
    private final String myName;
    @Nullable
    private final String myName2;

    public TestBefore(@Nullable String name, @Nullable String name2) {
        super();
        myName = name;
        myName2 = name2;
    }
}
