/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang.reflect;

import sun.reflect.CallerSensitive;
import sun.reflect.MethodAccessor;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.repository.MethodRepository;

import java.lang.annotation.Annotation;

/**
 * 获取参数个数
 * 获取参数类型
 * 获取返回值类型
 * 执行方法
 */
public final class Method extends Executable {


    private String getGenericSignature() {
        return signature;
    }

    @Override
    Executable getRoot() {
        return root;
    }

    @Override
    boolean hasGenericInformation() {
        return (getGenericSignature() != null);
    }

    @Override
    ConstructorRepository getGenericInfo() {
        return null;
    }

    @Override
    void specificToStringHeader(StringBuilder sb) {

    }

    @Override
    void specificToGenericStringHeader(StringBuilder sb) {

    }

    @Override
    byte[] getAnnotationBytes() {
        return annotations;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return clazz;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public TypeVariable<Method>[] getTypeParameters() {
        return (TypeVariable<Method>[]) new TypeVariable[0];
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Type getGenericReturnType() {
        return null;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    @Override
    public int getParameterCount() {
        return parameterTypes.length;
    }


    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return exceptionTypes.clone();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }


    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.methodModifiers(), isDefault());
    }


    @CallerSensitive
    public Object invoke(Object obj, Object... args)
            throws IllegalArgumentException,
            InvocationTargetException {
        MethodAccessor ma = methodAccessor;
        return ma.invoke(obj, args);
    }


    public boolean isBridge() {
        return (getModifiers() & Modifier.BRIDGE) != 0;
    }

    @Override
    public boolean isVarArgs() {
        return super.isVarArgs();
    }


    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }


    public boolean isDefault() {
        return ((getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) ==
                Modifier.PUBLIC) && getDeclaringClass().isInterface();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return super.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return super.getDeclaredAnnotations();
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return sharedGetParameterAnnotations(parameterTypes, parameterAnnotations);
    }

    @Override
    void handleParameterNumberMismatch(int resultLength, int numParameters) {

    }

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return getAnnotatedReturnType0(getGenericReturnType());
    }

    private Class<?> clazz;
    private int slot;
    private String name;
    private Class<?> returnType;
    private Class<?>[] parameterTypes;
    private Class<?>[] exceptionTypes;
    private int modifiers;
    private transient String signature;
    private transient MethodRepository genericInfo;
    private byte[] annotations;
    private byte[] parameterAnnotations;
    private byte[] annotationDefault;
    private volatile MethodAccessor methodAccessor;
    private Method root;

}
