/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.java.debugger.impl.ui.tree.render;

import com.intellij.java.debugger.DebuggerBundle;
import com.intellij.java.debugger.engine.DebugProcess;
import com.intellij.java.debugger.engine.evaluation.EvaluateException;
import com.intellij.java.debugger.engine.evaluation.EvaluationContext;
import com.intellij.java.debugger.impl.ClassLoadingUtils;
import com.intellij.java.debugger.impl.engine.FullValueEvaluatorProvider;
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl;
import com.intellij.java.debugger.impl.settings.NodeRendererSettings;
import com.intellij.java.debugger.impl.ui.impl.watch.ValueDescriptorImpl;
import consulo.execution.debug.frame.XFullValueEvaluator;
import consulo.images.desktop.awt.impl.editor.ImageEditorManagerImpl;
import consulo.internal.com.sun.jdi.*;
import consulo.java.rt.JavaRtClassNames;
import consulo.logging.Logger;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Created by Egor on 04.10.2014.
 */
class ImageObjectRenderer extends ToStringBasedRenderer implements FullValueEvaluatorProvider
{
	private static final Logger LOG = Logger.getInstance(ImageObjectRenderer.class);

	public ImageObjectRenderer(final NodeRendererSettings rendererSettings)
	{
		super(rendererSettings, "Image", null, null);
		setClassName("java.awt.Image");
		setEnabled(true);
	}

	@Nullable
	@Override
	public XFullValueEvaluator getFullValueEvaluator(final EvaluationContextImpl evaluationContext, final ValueDescriptorImpl valueDescriptor)
	{
		return new IconPopupEvaluator(DebuggerBundle.message("message.node.show.image"), evaluationContext)
		{
			@Override
			protected Image getData()
			{
				return getIcon(getEvaluationContext(), valueDescriptor.getValue(), "imageToBytes");
			}
		};
	}

	static JComponent createIconViewer(@Nullable Image uiImage)
	{
		if(uiImage == null)
		{
			return new JLabel("No data", SwingConstants.CENTER);
		}

		Icon icon = TargetAWT.to(uiImage);

		final int w = icon.getIconWidth();
		final int h = icon.getIconHeight();
		final BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		final Graphics2D g = image.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();

		return ImageEditorManagerImpl.createImageEditorUI(image);
	}

	@Nullable
	static Image getIcon(EvaluationContext evaluationContext, Value obj, String methodName)
	{
		try
		{
			Value bytes = getImageBytes(evaluationContext, obj, methodName);
			byte[] data = readBytes(bytes);
			if(data != null)
			{
				return Image.fromBytes(data, 16, 16);
			}
		}
		catch(Exception e)
		{
			LOG.info("Exception while getting image data", e);
		}
		return null;
	}

	private static Value getImageBytes(EvaluationContext evaluationContext, Value obj, String methodName) throws EvaluateException
	{
		DebugProcess process = evaluationContext.getDebugProcess();
		EvaluationContext copyContext = evaluationContext.createEvaluationContext(obj);
		ClassType helperClass = ClassLoadingUtils.getHelperClass(JavaRtClassNames.IMAGE_SERIALIZER, copyContext, process);

		if(helperClass != null)
		{
			List<Method> methods = helperClass.methodsByName(methodName);
			if(!methods.isEmpty())
			{
				return process.invokeMethod(copyContext, helperClass, methods.get(0), Collections.singletonList(obj));
			}
		}
		return null;
	}

	private static byte[] readBytes(Value bytes)
	{
		if(bytes instanceof ArrayReference)
		{
			List<Value> values = ((ArrayReference) bytes).getValues();
			byte[] res = new byte[values.size()];
			int idx = 0;
			for(Value value : values)
			{
				if(value instanceof ByteValue)
				{
					res[idx++] = ((ByteValue) value).value();
				}
				else
				{
					return null;
				}
			}
			return res;
		}
		return null;
	}

	static abstract class IconPopupEvaluator extends CustomPopupFullValueEvaluator<Image>
	{
		public IconPopupEvaluator(@Nonnull String linkText, @Nonnull EvaluationContextImpl evaluationContext)
		{
			super(linkText, evaluationContext);
		}

		@Override
		protected JComponent createComponent(Image data)
		{
			return createIconViewer(data);
		}
	}
}
