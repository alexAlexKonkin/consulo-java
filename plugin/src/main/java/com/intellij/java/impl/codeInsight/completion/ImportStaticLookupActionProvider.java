package com.intellij.java.impl.codeInsight.completion;

import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupActionProvider;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ui.image.Image;

/**
 * @author peter
 */
public class ImportStaticLookupActionProvider implements LookupActionProvider {
  @Override
  public void fillActions(final LookupElement element, Lookup lookup, Consumer<LookupElementAction> consumer) {
    final StaticallyImportable item = element.as(StaticallyImportable.CLASS_CONDITION_KEY);
    if (item == null || !item.canBeImported()) {
      return;
    }

    final Image checkIcon = AllIcons.Actions.Checked;
    final Image icon = item.willBeImported() ? checkIcon : Image.empty(checkIcon.getWidth(), checkIcon.getHeight());
    consumer.consume(new LookupElementAction(icon, "Import statically") {
      @Override
      public Result performLookupAction() {
        item.setShouldBeImported(!item.willBeImported());
        return new Result.ChooseItem(element);
      }
    });
  }
}
