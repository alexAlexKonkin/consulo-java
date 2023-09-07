/**
 * @author VISTALL
 * @since 02/12/2022
 */
open module consulo.java {
  requires consulo.ide.api;

  requires transitive consulo.java.language.api;
  requires transitive consulo.java.indexing.api;
  requires transitive consulo.java.execution.api;
  requires transitive consulo.java.jam.api;
  requires transitive consulo.java.debugger.api;
  requires transitive consulo.java.language.impl;
  requires transitive consulo.java.analysis.impl;
  requires transitive consulo.java.manifest;
  requires transitive consulo.java.debugger.impl;
  requires transitive consulo.java.compiler.impl;

  requires velocity.engine.core;

  requires com.intellij.xml;

  // TODO remove in future
  requires consulo.ide.impl;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;

  requires one.util.streamex;

  exports com.intellij.java.impl.analysis;
  exports com.intellij.java.impl.application.options;
  exports com.intellij.java.impl.application.options.editor;
  exports com.intellij.java.impl.codeEditor;
  exports com.intellij.java.impl.codeEditor.printing;
  exports com.intellij.java.impl.codeInsight;
  exports com.intellij.java.impl.codeInsight.completion;
  exports com.intellij.java.impl.codeInsight.completion.scope;
  exports com.intellij.java.impl.codeInsight.completion.simple;
  exports com.intellij.java.impl.codeInsight.completion.util;
  exports com.intellij.java.impl.codeInsight.daemon;
  exports com.intellij.java.impl.codeInsight.daemon.impl;
  exports com.intellij.java.impl.codeInsight.daemon.impl.actions;
  exports com.intellij.java.impl.codeInsight.daemon.impl.analysis;
  exports com.intellij.java.impl.codeInsight.daemon.impl.quickfix;
  exports com.intellij.java.impl.codeInsight.daemon.quickFix;
  exports com.intellij.java.impl.codeInsight.documentation;
  exports com.intellij.java.impl.codeInsight.editorActions;
  exports com.intellij.java.impl.codeInsight.editorActions.moveLeftRight;
  exports com.intellij.java.impl.codeInsight.editorActions.moveUpDown;
  exports com.intellij.java.impl.codeInsight.editorActions.smartEnter;
  exports com.intellij.java.impl.codeInsight.editorActions.wordSelection;
  exports com.intellij.java.impl.codeInsight.folding.impl;
  exports com.intellij.java.impl.codeInsight.generation;
  exports com.intellij.java.impl.codeInsight.generation.actions;
  exports com.intellij.java.impl.codeInsight.generation.surroundWith;
  exports com.intellij.java.impl.codeInsight.generation.ui;
  exports com.intellij.java.impl.codeInsight.highlighting;
  exports com.intellij.java.impl.codeInsight.hint;
  exports com.intellij.java.impl.codeInsight.hint.api.impls;
  exports com.intellij.java.impl.codeInsight.hints;
  exports com.intellij.java.impl.codeInsight.intention.impl;
  exports com.intellij.java.impl.codeInsight.intention.impl.config;
  exports com.intellij.java.impl.codeInsight.javadoc;
  exports com.intellij.java.impl.codeInsight.lookup;
  exports com.intellij.java.impl.codeInsight.lookup.impl;
  exports com.intellij.java.impl.codeInsight.navigation;
  exports com.intellij.java.impl.codeInsight.navigation.actions;
  exports com.intellij.java.impl.codeInsight.preview;
  exports com.intellij.java.impl.codeInsight.problems;
  exports com.intellij.java.impl.codeInsight.template;
  exports com.intellij.java.impl.codeInsight.template.impl;
  exports com.intellij.java.impl.codeInsight.template.macro;
  exports com.intellij.java.impl.codeInsight.template.postfix.templates;
  exports com.intellij.java.impl.codeInsight.template.postfix.util;
  exports com.intellij.java.impl.codeInsight.unwrap;
  exports com.intellij.java.impl.codeInspection;
  exports com.intellij.java.impl.codeInspection.accessStaticViaInstance;
  exports com.intellij.java.impl.codeInspection.actions;
  exports com.intellij.java.impl.codeInspection.canBeFinal;
  exports com.intellij.java.impl.codeInspection.compiler;
  exports com.intellij.java.impl.codeInspection.concurrencyAnnotations;
  exports com.intellij.java.impl.codeInspection.dataFlow;
  exports com.intellij.java.impl.codeInspection.dataFlow.fix;
  exports com.intellij.java.impl.codeInspection.deadCode;
  exports com.intellij.java.impl.codeInspection.defUse;
  exports com.intellij.java.impl.codeInspection.defaultFileTemplateUsage;
  exports com.intellij.java.impl.codeInspection.dependencyViolation;
  exports com.intellij.java.impl.codeInspection.duplicateStringLiteral;
  exports com.intellij.java.impl.codeInspection.duplicateThrows;
  exports com.intellij.java.impl.codeInspection.emptyMethod;
  exports com.intellij.java.impl.codeInspection.ex;
  exports com.intellij.java.impl.codeInspection.inconsistentLanguageLevel;
  exports com.intellij.java.impl.codeInspection.inferNullity;
  exports com.intellij.java.impl.codeInspection.inheritance;
  exports com.intellij.java.impl.codeInspection.inheritance.search;
  exports com.intellij.java.impl.codeInspection.javaDoc;
  exports com.intellij.java.impl.codeInspection.magicConstant;
  exports com.intellij.java.impl.codeInspection.miscGenerics;
  exports com.intellij.java.impl.codeInspection.nullable;
  exports com.intellij.java.impl.codeInspection.reference;
  exports com.intellij.java.impl.codeInspection.sameParameterValue;
  exports com.intellij.java.impl.codeInspection.sameReturnValue;
  exports com.intellij.java.impl.codeInspection.sillyAssignment;
  exports com.intellij.java.impl.codeInspection.suspiciousNameCombination;
  exports com.intellij.java.impl.codeInspection.testOnly;
  exports com.intellij.java.impl.codeInspection.ui;
  exports com.intellij.java.impl.codeInspection.uncheckedWarnings;
  exports com.intellij.java.impl.codeInspection.unnecessaryModuleDependency;
  exports com.intellij.java.impl.codeInspection.unneededThrows;
  exports com.intellij.java.impl.codeInspection.unusedLibraries;
  exports com.intellij.java.impl.codeInspection.unusedParameters;
  exports com.intellij.java.impl.codeInspection.unusedReturnValue;
  exports com.intellij.java.impl.codeInspection.unusedSymbol;
  exports com.intellij.java.impl.codeInspection.util;
  exports com.intellij.java.impl.codeInspection.varScopeCanBeNarrowed;
  exports com.intellij.java.impl.codeInspection.visibility;
  exports com.intellij.java.impl.codeInspection.wrongPackageStatement;
  exports com.intellij.java.impl.copyright.psi;
  exports com.intellij.java.impl.cyclicDependencies;
  exports com.intellij.java.impl.cyclicDependencies.actions;
  exports com.intellij.java.impl.cyclicDependencies.ui;
  exports com.intellij.java.impl.debugger.codeinsight;
  exports com.intellij.java.impl.find.findUsages;
  exports com.intellij.java.impl.ide;
  exports com.intellij.java.impl.ide.actions;
  exports com.intellij.java.impl.ide.favoritesTreeView;
  exports com.intellij.java.impl.ide.favoritesTreeView.smartPointerPsiNodes;
  exports com.intellij.java.impl.ide.fileTemplates;
  exports com.intellij.java.impl.ide.hierarchy;
  exports com.intellij.java.impl.ide.hierarchy.call;
  exports com.intellij.java.impl.ide.hierarchy.method;
  exports com.intellij.java.impl.ide.hierarchy.type;
  exports com.intellij.java.impl.ide.highlighter;
  exports com.intellij.java.impl.ide.macro;
  exports com.intellij.java.impl.ide.navigationToolbar;
  exports com.intellij.java.impl.ide.projectView;
  exports com.intellij.java.impl.ide.projectView.impl;
  exports com.intellij.java.impl.ide.projectView.impl.nodes;
  exports com.intellij.java.impl.ide.scopeView;
  exports com.intellij.java.impl.ide.scopeView.nodes;
  exports com.intellij.java.impl.ide.structureView.impl;
  exports com.intellij.java.impl.ide.structureView.impl.java;
  exports com.intellij.java.impl.ide.util;
  exports com.intellij.java.impl.ide.util.gotoByName;
  exports com.intellij.java.impl.ide.util.projectWizard.importSources;
  exports com.intellij.java.impl.ide.util.scopeChooser;
  exports com.intellij.java.impl.ide.util.treeView;
  exports com.intellij.java.impl.ig;
  exports com.intellij.java.impl.ig.abstraction;
  exports com.intellij.java.impl.ig.assignment;
  exports com.intellij.java.impl.ig.bitwise;
  exports com.intellij.java.impl.ig.bugs;
  exports com.intellij.java.impl.ig.classlayout;
  exports com.intellij.java.impl.ig.classmetrics;
  exports com.intellij.java.impl.ig.cloneable;
  exports com.intellij.java.impl.ig.controlflow;
  exports com.intellij.java.impl.ig.dataflow;
  exports com.intellij.java.impl.ig.dependency;
  exports com.intellij.java.impl.ig.encapsulation;
  exports com.intellij.java.impl.ig.errorhandling;
  exports com.intellij.java.impl.ig.finalization;
  exports com.intellij.java.impl.ig.fixes;
  exports com.intellij.java.impl.ig.imports;
  exports com.intellij.java.impl.ig.inheritance;
  exports com.intellij.java.impl.ig.initialization;
  exports com.intellij.java.impl.ig.internationalization;
  exports com.intellij.java.impl.ig.j2me;
  exports com.intellij.java.impl.ig.javabeans;
  exports com.intellij.java.impl.ig.javadoc;
  exports com.intellij.java.impl.ig.jdk;
  exports com.intellij.java.impl.ig.junit;
  exports com.intellij.java.impl.ig.logging;
  exports com.intellij.java.impl.ig.maturity;
  exports com.intellij.java.impl.ig.memory;
  exports com.intellij.java.impl.ig.methodmetrics;
  exports com.intellij.java.impl.ig.migration;
  exports com.intellij.java.impl.ig.modularization;
  exports com.intellij.java.impl.ig.naming;
  exports com.intellij.java.impl.ig.numeric;
  exports com.intellij.java.impl.ig.packaging;
  exports com.intellij.java.impl.ig.performance;
  exports com.intellij.java.impl.ig.portability;
  exports com.intellij.java.impl.ig.portability.mediatype;
  exports com.intellij.java.impl.ig.psiutils;
  exports com.intellij.java.impl.ig.redundancy;
  exports com.intellij.java.impl.ig.resources;
  exports com.intellij.java.impl.ig.security;
  exports com.intellij.java.impl.ig.serialization;
  exports com.intellij.java.impl.ig.style;
  exports com.intellij.java.impl.ig.threading;
  exports com.intellij.java.impl.ig.ui;
  exports com.intellij.java.impl.ig.visibility;
  exports com.intellij.java.impl.internal;
  exports com.intellij.java.impl.internal.diGraph;
  exports com.intellij.java.impl.internal.diGraph.analyzer;
  exports com.intellij.java.impl.internal.diGraph.impl;
  exports com.intellij.java.impl.internal.psiView;
  exports com.intellij.java.impl.ipp.adapter;
  exports com.intellij.java.impl.ipp.annotation;
  exports com.intellij.java.impl.ipp.asserttoif;
  exports com.intellij.java.impl.ipp.base;
  exports com.intellij.java.impl.ipp.bool;
  exports com.intellij.java.impl.ipp.braces;
  exports com.intellij.java.impl.ipp.chartostring;
  exports com.intellij.java.impl.ipp.comment;
  exports com.intellij.java.impl.ipp.commutative;
  exports com.intellij.java.impl.ipp.concatenation;
  exports com.intellij.java.impl.ipp.conditional;
  exports com.intellij.java.impl.ipp.constant;
  exports com.intellij.java.impl.ipp.decls;
  exports com.intellij.java.impl.ipp.enumswitch;
  exports com.intellij.java.impl.ipp.equality;
  exports com.intellij.java.impl.ipp.exceptions;
  exports com.intellij.java.impl.ipp.expression;
  exports com.intellij.java.impl.ipp.forloop;
  exports com.intellij.java.impl.ipp.fqnames;
  exports com.intellij.java.impl.ipp.imports;
  exports com.intellij.java.impl.ipp.increment;
  exports com.intellij.java.impl.ipp.initialization;
  exports com.intellij.java.impl.ipp.integer;
  exports com.intellij.java.impl.ipp.interfacetoclass;
  exports com.intellij.java.impl.ipp.junit;
  exports com.intellij.java.impl.ipp.modifiers;
  exports com.intellij.java.impl.ipp.opassign;
  exports com.intellij.java.impl.ipp.parenthesis;
  exports com.intellij.java.impl.ipp.psiutils;
  exports com.intellij.java.impl.ipp.shift;
  exports com.intellij.java.impl.ipp.switchtoif;
  exports com.intellij.java.impl.ipp.trivialif;
  exports com.intellij.java.impl.ipp.types;
  exports com.intellij.java.impl.ipp.varargs;
  exports com.intellij.java.impl.ipp.whileloop;
  exports com.intellij.java.impl.javadoc;
  exports com.intellij.java.impl.javadoc.actions;
  exports com.intellij.java.impl.lang.java;
  exports com.intellij.java.impl.lang.refactoring;
  exports com.intellij.java.impl.lexer;
  exports com.intellij.java.impl.openapi.fileTypes.impl;
  exports com.intellij.java.impl.openapi.options.colors.pages;
  exports com.intellij.java.impl.openapi.projectRoots;
  exports com.intellij.java.impl.openapi.projectRoots.impl;
  exports com.intellij.java.impl.openapi.roots;
  exports com.intellij.java.impl.openapi.roots.impl;
  exports com.intellij.java.impl.openapi.roots.libraries;
  exports com.intellij.java.impl.openapi.roots.ui.configuration;
  exports com.intellij.java.impl.openapi.roots.ui.configuration.libraryEditor;
  exports com.intellij.java.impl.openapi.vcs.contentAnnotation;
  exports com.intellij.java.impl.openapi.vfs.impl.jrt;
  exports com.intellij.java.impl.packageDependencies;
  exports com.intellij.java.impl.packageDependencies.ui;
  exports com.intellij.java.impl.pom.java;
  exports com.intellij.java.impl.pom.java.events;
  exports com.intellij.java.impl.pom.java.impl;
  exports com.intellij.java.impl.profile.codeInspection.ui;
  exports com.intellij.java.impl.psi;
  exports com.intellij.java.impl.psi.codeStyle;
  exports com.intellij.java.impl.psi.codeStyle.arrangement;
  exports com.intellij.java.impl.psi.filters;
  exports com.intellij.java.impl.psi.filters.classes;
  exports com.intellij.java.impl.psi.filters.element;
  exports com.intellij.java.impl.psi.filters.getters;
  exports com.intellij.java.impl.psi.filters.position;
  exports com.intellij.java.impl.psi.filters.types;
  exports com.intellij.java.impl.psi.formatter;
  exports com.intellij.java.impl.psi.formatter.java;
  exports com.intellij.java.impl.psi.formatter.java.wrap;
  exports com.intellij.java.impl.psi.formatter.java.wrap.impl;
  exports com.intellij.java.impl.psi.impl;
  exports com.intellij.java.impl.psi.impl.beanProperties;
  exports com.intellij.java.impl.psi.impl.cache.impl.idCache;
  exports com.intellij.java.impl.psi.impl.file;
  exports com.intellij.java.impl.psi.impl.migration;
  exports com.intellij.java.impl.psi.impl.search;
  exports com.intellij.java.impl.psi.impl.smartPointers;
  exports com.intellij.java.impl.psi.impl.source;
  exports com.intellij.java.impl.psi.impl.source.codeStyle;
  exports com.intellij.java.impl.psi.impl.source.codeStyle.javadoc;
  exports com.intellij.java.impl.psi.impl.source.javadoc;
  exports com.intellij.java.impl.psi.impl.source.resolve;
  exports com.intellij.java.impl.psi.impl.source.resolve.reference.impl;
  exports com.intellij.java.impl.psi.impl.source.resolve.reference.impl.manipulators;
  exports com.intellij.java.impl.psi.impl.source.resolve.reference.impl.providers;
  exports com.intellij.java.impl.psi.impl.source.tree;
  exports com.intellij.java.impl.psi.impl.source.tree.injected;
  exports com.intellij.java.impl.psi.impl.source.tree.java;
  exports com.intellij.java.impl.psi.infos;
  exports com.intellij.java.impl.psi.resolve;
  exports com.intellij.java.impl.psi.search.scope.packageSet;
  exports com.intellij.java.impl.psi.search.searches;
  exports com.intellij.java.impl.psi.statistics;
  exports com.intellij.java.impl.psi.util;
  exports com.intellij.java.impl.psi.util.proximity;
  exports com.intellij.java.impl.refactoring;
  exports com.intellij.java.impl.refactoring.actions;
  exports com.intellij.java.impl.refactoring.anonymousToInner;
  exports com.intellij.java.impl.refactoring.changeClassSignature;
  exports com.intellij.java.impl.refactoring.changeSignature;
  exports com.intellij.java.impl.refactoring.changeSignature.inCallers;
  exports com.intellij.java.impl.refactoring.convertToInstanceMethod;
  exports com.intellij.java.impl.refactoring.copy;
  exports com.intellij.java.impl.refactoring.encapsulateFields;
  exports com.intellij.java.impl.refactoring.extractInterface;
  exports com.intellij.java.impl.refactoring.extractMethod;
  exports com.intellij.java.impl.refactoring.extractMethodObject;
  exports com.intellij.java.impl.refactoring.extractSuperclass;
  exports com.intellij.java.impl.refactoring.extractclass;
  exports com.intellij.java.impl.refactoring.extractclass.usageInfo;
  exports com.intellij.java.impl.refactoring.inheritanceToDelegation;
  exports com.intellij.java.impl.refactoring.inheritanceToDelegation.usageInfo;
  exports com.intellij.java.impl.refactoring.inline;
  exports com.intellij.java.impl.refactoring.inlineSuperClass;
  exports com.intellij.java.impl.refactoring.inlineSuperClass.usageInfo;
  exports com.intellij.java.impl.refactoring.introduceField;
  exports com.intellij.java.impl.refactoring.introduceParameter;
  exports com.intellij.java.impl.refactoring.introduceVariable;
  exports com.intellij.java.impl.refactoring.introduceparameterobject;
  exports com.intellij.java.impl.refactoring.introduceparameterobject.usageInfo;
  exports com.intellij.java.impl.refactoring.invertBoolean;
  exports com.intellij.java.impl.refactoring.listeners;
  exports com.intellij.java.impl.refactoring.listeners.impl;
  exports com.intellij.java.impl.refactoring.makeStatic;
  exports com.intellij.java.impl.refactoring.memberPullUp;
  exports com.intellij.java.impl.refactoring.memberPushDown;
  exports com.intellij.java.impl.refactoring.migration;
  exports com.intellij.java.impl.refactoring.move;
  exports com.intellij.java.impl.refactoring.move.moveClassesOrPackages;
  exports com.intellij.java.impl.refactoring.move.moveFilesOrDirectories;
  exports com.intellij.java.impl.refactoring.move.moveInner;
  exports com.intellij.java.impl.refactoring.move.moveInstanceMethod;
  exports com.intellij.java.impl.refactoring.move.moveMembers;
  exports com.intellij.java.impl.refactoring.openapi.impl;
  exports com.intellij.java.impl.refactoring.psi;
  exports com.intellij.java.impl.refactoring.removemiddleman;
  exports com.intellij.java.impl.refactoring.removemiddleman.usageInfo;
  exports com.intellij.java.impl.refactoring.rename;
  exports com.intellij.java.impl.refactoring.rename.inplace;
  exports com.intellij.java.impl.refactoring.rename.naming;
  exports com.intellij.java.impl.refactoring.replaceConstructorWithBuilder;
  exports com.intellij.java.impl.refactoring.replaceConstructorWithBuilder.usageInfo;
  exports com.intellij.java.impl.refactoring.replaceConstructorWithFactory;
  exports com.intellij.java.impl.refactoring.safeDelete;
  exports com.intellij.java.impl.refactoring.safeDelete.usageInfo;
  exports com.intellij.java.impl.refactoring.tempWithQuery;
  exports com.intellij.java.impl.refactoring.turnRefsToSuper;
  exports com.intellij.java.impl.refactoring.typeCook;
  exports com.intellij.java.impl.refactoring.typeCook.deductive;
  exports com.intellij.java.impl.refactoring.typeCook.deductive.builder;
  exports com.intellij.java.impl.refactoring.typeCook.deductive.resolver;
  exports com.intellij.java.impl.refactoring.typeCook.deductive.util;
  exports com.intellij.java.impl.refactoring.typeMigration;
  exports com.intellij.java.impl.refactoring.typeMigration.actions;
  exports com.intellij.java.impl.refactoring.typeMigration.rules;
  exports com.intellij.java.impl.refactoring.typeMigration.ui;
  exports com.intellij.java.impl.refactoring.typeMigration.usageInfo;
  exports com.intellij.java.impl.refactoring.ui;
  exports com.intellij.java.impl.refactoring.util;
  exports com.intellij.java.impl.refactoring.util.classMembers;
  exports com.intellij.java.impl.refactoring.util.classRefs;
  exports com.intellij.java.impl.refactoring.util.duplicates;
  exports com.intellij.java.impl.refactoring.util.javadoc;
  exports com.intellij.java.impl.refactoring.util.occurrences;
  exports com.intellij.java.impl.refactoring.util.usageInfo;
  exports com.intellij.java.impl.refactoring.wrapreturnvalue;
  exports com.intellij.java.impl.refactoring.wrapreturnvalue.usageInfo;
  exports com.intellij.java.impl.slicer;
  exports com.intellij.java.impl.spi;
  exports com.intellij.java.impl.spi.parsing;
  exports com.intellij.java.impl.spi.psi;
  exports com.intellij.java.impl.testIntegration;
  exports com.intellij.java.impl.testIntegration.createTest;
  exports com.intellij.java.impl.testIntegration.intention;
  exports com.intellij.java.impl.ui;
  exports com.intellij.java.impl.unscramble;
  exports com.intellij.java.impl.usageView;
  exports com.intellij.java.impl.usages.impl.rules;
  exports com.intellij.java.impl.util.descriptors;
  exports com.intellij.java.impl.util.descriptors.impl;
  exports com.intellij.java.impl.util.text;
  exports com.intellij.java.impl.util.xml;
  exports com.intellij.java.impl.util.xml.actions;
  exports com.intellij.java.impl.util.xml.converters;
  exports com.intellij.java.impl.util.xml.converters.values;
  exports com.intellij.java.impl.util.xml.impl;
  exports com.intellij.java.impl.util.xml.ui;
  exports com.intellij.java.impl.vcsUtil;
  exports com.intellij.java.impl.vfs.impl.jar;
  exports consulo.java.impl;
  exports consulo.java.impl.application.options;
  exports consulo.java.impl.codeInsight;
  exports consulo.java.impl.fileEditor.impl;
  exports consulo.java.impl.ide;
  exports consulo.java.impl.ide.newProjectOrModule;
  exports consulo.java.impl.ide.projectView.impl;
  exports consulo.java.impl.library;
  exports consulo.java.impl.model.annotations;
  exports consulo.java.impl.module.extension;
  exports consulo.java.impl.module.extension.ui;
  exports consulo.java.impl.psi.impl;
  exports consulo.java.impl.refactoring;
  exports consulo.java.impl.refactoring.changeSignature;
  exports consulo.java.impl.roots;
  exports consulo.java.impl.spi;
  exports consulo.java.impl.util;
  exports com.intellij.java.impl.generate.config;
  exports com.intellij.java.impl.generate.element;
  exports com.intellij.java.impl.generate.exception;
  exports com.intellij.java.impl.generate.template;
  exports com.intellij.java.impl.generate.template.toString;
  exports com.intellij.java.impl.generate.velocity;
  exports com.intellij.java.impl.generate.view;
}