package consulo.java.manifest.editor.completionProviders;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.language.editor.QualifiedNameProvider;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationProvider;
import consulo.navigation.NavigationItem;
import consulo.ui.image.Image;
import org.osmorc.manifest.lang.headerparser.HeaderParser;
import org.osmorc.manifest.lang.psi.Clause;
import org.osmorc.manifest.lang.psi.Header;
import org.osmorc.manifest.lang.psi.HeaderValuePart;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 8:57/04.05.13
 */
public class HeaderValueCompletionProvider extends TextFieldWithAutoCompletionListProvider<Object> {

  private final Header myHeaderByName;
  private final HeaderParser myHeaderParser;

  public HeaderValueCompletionProvider(Header headerByName, HeaderParser headerParser) {
    super(null);
    myHeaderByName = headerByName;
    myHeaderParser = headerParser;
  }

  @Nonnull
  @Override
  public Collection<Object> getItems(String prefix, boolean cached, CompletionParameters parameters) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Collection<Object>>() {
      @Override
      public Collection<Object> compute() {
        Clause[] clauses = myHeaderByName.getClauses();
        if (clauses.length == 0) {
          return Collections.emptyList();
        }
        HeaderValuePart value = clauses[0].getValue();
        if (value == null) {
          return Collections.emptyList();
        }

        List<Object> objects = new ArrayList<Object>();
        PsiReference[] references = myHeaderParser.getReferences(value);
        for (PsiReference reference : references) {
          for (Object o : reference.getVariants()) {
            if (myHeaderParser.isAcceptable(o)) {
              objects.add(o);
            }
          }
        }
        return objects;
      }
    });
  }

  @Nullable
  @Override
  protected Image getIcon(@Nonnull Object item) {
    if (item instanceof NavigationItem) {
      ItemPresentation itemPresentation = ItemPresentationProvider.getItemPresentation((NavigationItem) item);
      if (itemPresentation != null) {
        return itemPresentation.getIcon();
      }
    }
    return null;
  }

  @Nonnull
  @Override
  protected String getLookupString(@Nonnull Object item) {
    if (item instanceof PsiElement) {
      for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensionList()) {
        String result = provider.getQualifiedName((PsiElement) item);
        if (result != null) {
          return result;
        }
      }
    }
    return item.toString();
  }

  @jakarta.annotation.Nullable
  @Override
  protected String getTailText(@Nonnull Object item) {
    return null;
  }

  @jakarta.annotation.Nullable
  @Override
  protected String getTypeText(@Nonnull Object item) {
    return null;
  }

  @Override
  public int compare(Object item1, Object item2) {
    return 0;
  }
}
