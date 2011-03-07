/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.apt;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.mysema.codegen.model.ClassType;
import com.mysema.commons.lang.Assert;
import com.mysema.query.annotations.Config;
import com.mysema.query.annotations.QueryProjection;
import com.mysema.query.annotations.QueryType;
import com.mysema.query.codegen.CodegenModule;
import com.mysema.query.codegen.EmbeddableSerializer;
import com.mysema.query.codegen.EntitySerializer;
import com.mysema.query.codegen.EntityType;
import com.mysema.query.codegen.ProjectionSerializer;
import com.mysema.query.codegen.QueryTypeFactory;
import com.mysema.query.codegen.Serializer;
import com.mysema.query.codegen.SerializerConfig;
import com.mysema.query.codegen.SimpleSerializerConfig;
import com.mysema.query.codegen.SupertypeSerializer;
import com.mysema.query.codegen.TypeMappings;
import com.mysema.query.types.Expression;

/**
 * DefaultConfiguration is a simple implementation of the Configuration interface
 *
 * @author tiwe
 *
 */
public class DefaultConfiguration implements Configuration {

    private static final String QUERYDSL_CREATE_DEFAULT_VARIABLE = "querydsl.createDefaultVariable";

    private static final String QUERYDSL_PREFIX = "querydsl.prefix";
    
    private static final String QUERYDSL_SUFFIX = "querydsl.suffix";
    
    private static final String QUERYDSL_PACKAGE_SUFFIX = "querydsl.packageSuffix";
    
    private static final String QUERYDSL_MAP_ACCESSORS = "querydsl.mapAccessors";

    private static final String QUERYDSL_LIST_ACCESSORS = "querydsl.listAccessors";

    private static final String QUERYDSL_ENTITY_ACCESSORS = "querydsl.entityAccessors";

    private static final String DEFAULT_OVERWRITE = "defaultOverwrite";

    private final CodegenModule module = new CodegenModule();
    
    private final SerializerConfig defaultSerializerConfig;

    private final Map<String, SerializerConfig> packageToConfig = new HashMap<String, SerializerConfig>();

    protected final Class<? extends Annotation> entityAnn;

    @Nullable
    protected final Class<? extends Annotation> entitiesAnn, superTypeAnn, embeddedAnn, embeddableAnn, skipAnn;

    private final Map<String, SerializerConfig> typeToConfig = new HashMap<String, SerializerConfig>();

    private boolean useFields = true, useGetters = true, defaultOverwrite = false;
    
    public DefaultConfiguration(
            RoundEnvironment roundEnv,
            Map<String, String> options,
            Collection<String> keywords,
            @Nullable Class<? extends Annotation> entitiesAnn,
            Class<? extends Annotation> entityAnn,
            @Nullable Class<? extends Annotation> superTypeAnn,
            @Nullable Class<? extends Annotation> embeddableAnn,
            @Nullable Class<? extends Annotation> embeddedAnn,
            @Nullable Class<? extends Annotation> skipAnn) {
        module.bind(RoundEnvironment.class, roundEnv);
        module.bind(CodegenModule.KEYWORDS, keywords);
        this.entitiesAnn = entitiesAnn;
        this.entityAnn = Assert.notNull(entityAnn,"entityAnn");
        this.superTypeAnn = superTypeAnn;
        this.embeddableAnn = embeddableAnn;
        this.embeddedAnn = embeddedAnn;
        this.skipAnn = skipAnn;
        for (Element element : roundEnv.getElementsAnnotatedWith(Config.class)){
            Config querydslConfig = element.getAnnotation(Config.class);
            SerializerConfig config = SimpleSerializerConfig.getConfig(querydslConfig);
            if (element instanceof PackageElement){
                PackageElement packageElement = (PackageElement)element;
                packageToConfig.put(packageElement.getQualifiedName().toString(), config);
            }else if (element instanceof TypeElement){
                TypeElement typeElement = (TypeElement)element;
                typeToConfig.put(typeElement.getQualifiedName().toString(), config);
            }
        }
        boolean entityAccessors = false;
        boolean listAccessors = false;
        boolean mapAccessors = false;
        boolean createDefaultVariable = true;
        if (options.containsKey(QUERYDSL_ENTITY_ACCESSORS)){
            entityAccessors = Boolean.valueOf(options.get(QUERYDSL_ENTITY_ACCESSORS));
        }
        if (options.containsKey(QUERYDSL_LIST_ACCESSORS)){
            listAccessors = Boolean.valueOf(options.get(QUERYDSL_LIST_ACCESSORS));
        }
        if (options.containsKey(QUERYDSL_MAP_ACCESSORS)){
            mapAccessors = Boolean.valueOf(options.get(QUERYDSL_MAP_ACCESSORS));
        }
        if (options.containsKey(QUERYDSL_CREATE_DEFAULT_VARIABLE)){
            createDefaultVariable = Boolean.valueOf(options.get(QUERYDSL_CREATE_DEFAULT_VARIABLE));
        }
        if (options.containsKey(QUERYDSL_PACKAGE_SUFFIX)){
            module.bind(CodegenModule.PACKAGE_SUFFIX, options.get(QUERYDSL_PACKAGE_SUFFIX));
        }
        if (options.containsKey(QUERYDSL_PREFIX)){
            module.bind(CodegenModule.PREFIX, options.get(QUERYDSL_PREFIX));
        }
        if (options.containsKey(QUERYDSL_SUFFIX)){
            module.bind(CodegenModule.SUFFIX, options.get(QUERYDSL_SUFFIX));
        }
        if (options.containsKey(DEFAULT_OVERWRITE)){
            defaultOverwrite = Boolean.valueOf(options.get(DEFAULT_OVERWRITE));
        }

        defaultSerializerConfig = new SimpleSerializerConfig(entityAccessors, listAccessors, mapAccessors, createDefaultVariable);

    }

    @Override
    public VisitorConfig getConfig(TypeElement e, List<? extends Element> elements){
        if (useFields){
            if (useGetters){
                return VisitorConfig.ALL;
            }else{
                return VisitorConfig.FIELDS_ONLY;
            }
        }else if (useGetters){
            return VisitorConfig.METHODS_ONLY;
        }else{
            return VisitorConfig.NONE;
        }
    }

    @Override
    public Serializer getDTOSerializer() {
        return module.get(ProjectionSerializer.class);
    }

    @Override
    @Nullable
    public Class<? extends Annotation> getEntitiesAnnotation() {
        return entitiesAnn;
    }

    @Override
    @Nullable
    public Class<? extends Annotation> getEmbeddableAnnotation() {
        return embeddableAnn;
    }

    @Override
    public Serializer getEmbeddableSerializer() {
        return module.get(EmbeddableSerializer.class);
    }

    @Override
    public Class<? extends Annotation> getEntityAnnotation() {
        return entityAnn;
    }

    @Override
    @Nullable
    public Class<? extends Annotation> getEmbeddedAnnotation() {
        return embeddedAnn;
    }

    @Override
    public Serializer getEntitySerializer() {
        return module.get(EntitySerializer.class);
    }

    @Override
    public String getNamePrefix() {
        return module.get(String.class, "prefix");
    }

    @Override
    public SerializerConfig getSerializerConfig(EntityType entityType) {
        if (typeToConfig.containsKey(entityType.getFullName())){
            return typeToConfig.get(entityType.getFullName());
        }else if (packageToConfig.containsKey(entityType.getPackageName())){
            return packageToConfig.get(entityType.getPackageName());
        }else{
            return defaultSerializerConfig;
        }
    }

    @Override
    @Nullable
    public Class<? extends Annotation> getSkipAnnotation() {
        return skipAnn;
    }

    @Override
    @Nullable
    public Class<? extends Annotation> getSuperTypeAnnotation() {
        return superTypeAnn;
    }

    @Override
    public Serializer getSupertypeSerializer() {
        return module.get(SupertypeSerializer.class);
    }

    @Override
    public boolean isBlockedField(VariableElement field) {
        if (field.getAnnotation(QueryType.class) != null){
            return false;
        }else{
            return field.getAnnotation(skipAnn) != null
            || field.getModifiers().contains(Modifier.TRANSIENT)
            || field.getModifiers().contains(Modifier.STATIC);
        }
    }

    @Override
    public boolean isBlockedGetter(ExecutableElement getter){
        if (getter.getAnnotation(QueryType.class) != null){
            return false;
        }else{
            return getter.getAnnotation(skipAnn) != null
                || getter.getModifiers().contains(Modifier.STATIC);
        }
    }

    @Override
    public boolean isDefaultOverwrite() {
        return defaultOverwrite;
    }

    @Override
    public boolean isUseFields() {
        return useFields;
    }

    @Override
    public boolean isUseGetters() {
        return useGetters;
    }

    @Override
    public boolean isValidConstructor(ExecutableElement constructor) {
        return constructor.getModifiers().contains(Modifier.PUBLIC)
            && constructor.getAnnotation(QueryProjection.class) != null
            && !constructor.getParameters().isEmpty();
    }

    @Override
    public boolean isValidField(VariableElement field) {
        if (field.getAnnotation(QueryType.class) != null){
            return true;
        }else{
            return field.getAnnotation(skipAnn) == null
                && !field.getModifiers().contains(Modifier.TRANSIENT)
                && !field.getModifiers().contains(Modifier.STATIC);
        }
    }

    @Override
    public boolean isValidGetter(ExecutableElement getter){
        if (getter.getAnnotation(QueryType.class) != null){
            return true;
        }else{
            return getter.getAnnotation(skipAnn) == null
                && !getter.getModifiers().contains(Modifier.STATIC);
        }
    }

    public void setNamePrefix(String namePrefix) {
        module.bind(CodegenModule.PREFIX, namePrefix);
    }

    public void setUseFields(boolean b){
        this.useFields = b;
    }

    public void setUseGetters(boolean b) {
        this.useGetters = b;
    }

    @Override
    public TypeMappings getTypeMappings() {
        return module.get(TypeMappings.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getKeywords(){
        return module.get(Collection.class, CodegenModule.KEYWORDS);
    }

    public String getNameSuffix() {
        return module.get(String.class, CodegenModule.SUFFIX);
    }

    public void setNameSuffix(String nameSuffix) {
        module.bind(CodegenModule.SUFFIX, nameSuffix);
    }
    
    public <T> void addCustomType(Class<T> type, Class<? extends Expression<T>> queryType){
        module.get(TypeMappings.class).register(new ClassType(type), new ClassType(queryType));
    }

    @Override
    public QueryTypeFactory getQueryTypeFactory() {
        return module.get(QueryTypeFactory.class);
    }

}
