public class Test {
  @javax.annotation.Nullable
  public final String o;

  public Test(String q) { o = q; }

  public void test() {
    if (o != null) {
      bar();
      o.hashCode();
    }
  }

  public void bar() {}
}