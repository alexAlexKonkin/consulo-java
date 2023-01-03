package consulo.java.impl.bundle;

import com.intellij.java.impl.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.java.language.projectRoots.JavaSdk;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.PredefinedBundlesProvider;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.platform.Platform;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/12/2021
 */
@ExtensionImpl
public class JavaHomePredefinedBundleProvider extends PredefinedBundlesProvider {
  private static final String JAVA_HOME = "JAVA_HOME";

  @Override
  public void createBundles(@Nonnull Context context) {
    String environmentVariable = Platform.current().os().getEnvironmentVariable(JAVA_HOME);
    if (environmentVariable == null) {
      return;
    }

    JavaSdk javaSdk = JavaSdkImpl.getInstance();
    if (javaSdk.isValidSdkHome(environmentVariable)) {
      Sdk sdk = context.createSdkWithName(javaSdk, JAVA_HOME);
      SdkModificator sdkModificator = sdk.getSdkModificator();
      sdkModificator.setHomePath(environmentVariable);
      sdkModificator.setVersionString(javaSdk.getVersionString(environmentVariable));
      sdkModificator.commitChanges();

      javaSdk.setupSdkPaths(sdk);
    }
  }
}