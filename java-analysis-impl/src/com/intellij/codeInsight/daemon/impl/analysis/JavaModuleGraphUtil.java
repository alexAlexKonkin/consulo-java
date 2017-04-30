/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.analysis;

import static com.intellij.psi.PsiJavaModule.MODULE_INFO_FILE;
import static com.intellij.psi.util.PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT;

import gnu.trove.THashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackageAccessibilityStatement;
import com.intellij.psi.PsiRequiresStatement;
import com.intellij.psi.impl.light.LightJavaModule;
import com.intellij.psi.impl.source.PsiJavaModuleReference;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;

public class JavaModuleGraphUtil
{
	private JavaModuleGraphUtil()
	{
	}

	@Nullable
	public static PsiJavaModule findDescriptorByElement(@Nullable PsiElement element)
	{
		if(element != null)
		{
			PsiFileSystemItem fsItem = element instanceof PsiFileSystemItem ? (PsiFileSystemItem) element : element.getContainingFile();
			if(fsItem != null)
			{
				return ModuleHighlightUtil.getModuleDescriptor(fsItem);
			}
		}

		return null;
	}

	@Nullable
	public static Collection<PsiJavaModule> findCycle(@NotNull PsiJavaModule module)
	{
		Project project = module.getProject();
		List<Set<PsiJavaModule>> cycles = CachedValuesManager.getManager(project).getCachedValue(project, () -> Result.create(findCycles(project), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT));
		return ContainerUtil.find(cycles, set -> set.contains(module));
	}

	public static boolean exports(@NotNull PsiJavaModule source, @NotNull String packageName, @NotNull PsiJavaModule target)
	{
		Map<String, Set<String>> exports = CachedValuesManager.getCachedValue(source, () -> Result.create(exportsMap(source), source.getContainingFile()));
		Set<String> targets = exports.get(packageName);
		return targets != null && (targets.isEmpty() || targets.contains(target.getName()));
	}

	public static boolean reads(@NotNull PsiJavaModule source, @NotNull PsiJavaModule destination)
	{
		return getRequiresGraph(source).reads(source, destination);
	}

	@Nullable
	public static Trinity<String, PsiJavaModule, PsiJavaModule> findConflict(@NotNull PsiJavaModule module)
	{
		return getRequiresGraph(module).findConflict(module);
	}

	@Nullable
	public static PsiJavaModule findOrigin(@NotNull PsiJavaModule module, @NotNull String packageName)
	{
		return getRequiresGraph(module).findOrigin(module, packageName);
	}

	// Looks for cycles between Java modules in the project sources.
	// Library/JDK modules are excluded - in assumption there can't be any lib -> src dependencies.
	// Module references are resolved "globally" (i.e., without taking project dependencies into account).
	private static List<Set<PsiJavaModule>> findCycles(Project project)
	{
		Set<PsiJavaModule> projectModules = ContainerUtil.newHashSet();
		for(Module module : ModuleManager.getInstance(project).getModules())
		{
			Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, MODULE_INFO_FILE, module.getModuleScope());
			if(files.size() > 1)
			{
				return Collections.emptyList();  // aborts the process when there are incorrect modules in the project
			}
			Optional.ofNullable(ContainerUtil.getFirstItem(files)).map(PsiManager.getInstance(project)::findFile).map(f -> f instanceof PsiJavaFile ? ((PsiJavaFile) f).getModuleDeclaration() : null)
					.ifPresent(projectModules::add);
		}

		if(!projectModules.isEmpty())
		{
			MultiMap<PsiJavaModule, PsiJavaModule> relations = MultiMap.create();
			for(PsiJavaModule module : projectModules)
			{
				for(PsiRequiresStatement statement : module.getRequires())
				{
					PsiJavaModule dependency = PsiJavaModuleReference.resolve(statement, statement.getModuleName(), true);
					if(dependency != null && projectModules.contains(dependency))
					{
						relations.putValue(module, dependency);
					}
				}
			}

			if(!relations.isEmpty())
			{
				Graph<PsiJavaModule> graph = new ChameleonGraph<>(relations, false);
				DFSTBuilder<PsiJavaModule> builder = new DFSTBuilder<>(graph);
				Collection<Collection<PsiJavaModule>> components = builder.getComponents();
				if(!components.isEmpty())
				{
					return components.stream().map(ContainerUtil::newLinkedHashSet).collect(Collectors.toList());
				}
			}
		}

		return Collections.emptyList();
	}

	private static Map<String, Set<String>> exportsMap(@NotNull PsiJavaModule source)
	{
		Map<String, Set<String>> map = ContainerUtil.newHashMap();
		for(PsiPackageAccessibilityStatement statement : source.getExports())
		{
			String pkg = statement.getPackageName();
			List<String> targets = statement.getModuleNames();
			map.put(pkg, targets.isEmpty() ? Collections.emptySet() : ContainerUtil.newTroveSet(targets));
		}
		return map;
	}

	private static RequiresGraph getRequiresGraph(PsiJavaModule module)
	{
		Project project = module.getProject();
		return CachedValuesManager.getManager(project).getCachedValue(project, () -> Result.create(buildRequiresGraph(project), OUT_OF_CODE_BLOCK_MODIFICATION_COUNT));
	}

	// Starting from source modules, collects all module dependencies in the project.
	// The resulting graph is used for tracing readability and checking package conflicts.
	private static RequiresGraph buildRequiresGraph(Project project)
	{
		MultiMap<PsiJavaModule, PsiJavaModule> relations = MultiMap.create();
		Set<String> transitiveEdges = ContainerUtil.newTroveSet();
		for(Module module : ModuleManager.getInstance(project).getModules())
		{
			Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, MODULE_INFO_FILE, module.getModuleScope());
			Optional.ofNullable(ContainerUtil.getFirstItem(files)).map(PsiManager.getInstance(project)::findFile).map(f -> f instanceof PsiJavaFile ? ((PsiJavaFile) f).getModuleDeclaration() : null)
					.ifPresent(m -> visit(m, relations, transitiveEdges));
		}

		Graph<PsiJavaModule> graph = GraphGenerator.generate(new ChameleonGraph<>(relations, true));
		return new RequiresGraph(graph, transitiveEdges);
	}

	private static void visit(PsiJavaModule module, MultiMap<PsiJavaModule, PsiJavaModule> relations, Set<String> transitiveEdges)
	{
		if(!relations.containsKey(module))
		{
			relations.putValues(module, Collections.emptyList());
			boolean explicitJavaBase = false;
			for(PsiRequiresStatement statement : module.getRequires())
			{
				String moduleName = statement.getModuleName();
				if(PsiJavaModule.JAVA_BASE.equals(moduleName))
				{
					explicitJavaBase = true;
				}
				for(PsiJavaModule dependency : PsiJavaModuleReference.multiResolve(statement, moduleName, false))
				{
					relations.putValue(module, dependency);
					if(statement.hasModifierProperty(PsiModifier.TRANSITIVE))
					{
						transitiveEdges.add(RequiresGraph.key(dependency, module));
					}
					visit(dependency, relations, transitiveEdges);
				}
			}
			if(!explicitJavaBase && !(module instanceof LightJavaModule))
			{
				PsiJavaModule javaBase = PsiJavaModuleReference.resolve(module, PsiJavaModule.JAVA_BASE, false);
				if(javaBase != null)
				{
					relations.putValue(module, javaBase);
				}
			}
		}
	}

	private static class RequiresGraph
	{
		private final Graph<PsiJavaModule> myGraph;
		private final Set<String> myTransitiveEdges;

		public RequiresGraph(Graph<PsiJavaModule> graph, Set<String> transitiveEdges)
		{
			myGraph = graph;
			myTransitiveEdges = transitiveEdges;
		}

		public boolean reads(PsiJavaModule source, PsiJavaModule destination)
		{
			Collection<PsiJavaModule> nodes = myGraph.getNodes();
			if(nodes.contains(destination) && nodes.contains(source))
			{
				Iterator<PsiJavaModule> directReaders = myGraph.getOut(destination);
				while(directReaders.hasNext())
				{
					PsiJavaModule next = directReaders.next();
					if(source.equals(next) || myTransitiveEdges.contains(key(destination, next)) && reads(source, next))
					{
						return true;
					}
				}
			}
			return false;
		}

		public Trinity<String, PsiJavaModule, PsiJavaModule> findConflict(PsiJavaModule source)
		{
			Map<String, PsiJavaModule> exports = ContainerUtil.newHashMap();
			return processExports(source, (pkg, m) ->
			{
				PsiJavaModule existing = exports.put(pkg, m);
				return existing != null ? new Trinity<>(pkg, existing, m) : null;
			});
		}

		public PsiJavaModule findOrigin(PsiJavaModule module, String packageName)
		{
			return processExports(module, (pkg, m) -> packageName.equals(pkg) ? m : null);
		}

		private <T> T processExports(PsiJavaModule start, BiFunction<String, PsiJavaModule, T> processor)
		{
			return myGraph.getNodes().contains(start) ? processExports(start.getName(), start, 0, ContainerUtil.newHashSet(), processor) : null;
		}

		private <T> T processExports(String name, PsiJavaModule module, int layer, Set<PsiJavaModule> visited, BiFunction<String, PsiJavaModule, T> processor)
		{
			if(visited.add(module))
			{
				if(layer == 1)
				{
					for(PsiPackageAccessibilityStatement statement : module.getExports())
					{
						List<String> exportTargets = statement.getModuleNames();
						if(exportTargets.isEmpty() || exportTargets.contains(name))
						{
							T result = processor.apply(statement.getPackageName(), module);
							if(result != null)
							{
								return result;
							}
						}
					}
				}
				if(layer < 2)
				{
					Iterator<PsiJavaModule> iterator = myGraph.getIn(module);
					while(iterator.hasNext())
					{
						PsiJavaModule dependency = iterator.next();
						if(layer == 0 || myTransitiveEdges.contains(key(dependency, module)))
						{
							T result = processExports(name, dependency, 1, visited, processor);
							if(result != null)
							{
								return result;
							}
						}
					}
				}
			}

			return null;
		}

		public static String key(PsiJavaModule module, PsiJavaModule exporter)
		{
			return module.getName() + '/' + exporter.getName();
		}
	}

	private static class ChameleonGraph<N> implements Graph<N>
	{
		private final Set<N> myNodes;
		private final MultiMap<N, N> myEdges;
		private final boolean myInbound;

		public ChameleonGraph(MultiMap<N, N> edges, boolean inbound)
		{
			myNodes = new THashSet<>();
			edges.entrySet().forEach(e ->
			{
				myNodes.add(e.getKey());
				myNodes.addAll(e.getValue());
			});
			myEdges = edges;
			myInbound = inbound;
		}

		@Override
		public Collection<N> getNodes()
		{
			return myNodes;
		}

		@Override
		public Iterator<N> getIn(N n)
		{
			return myInbound ? myEdges.get(n).iterator() : Collections.emptyIterator();
		}

		@Override
		public Iterator<N> getOut(N n)
		{
			return myInbound ? Collections.emptyIterator() : myEdges.get(n).iterator();
		}
	}
}