package java.lang.reflect;

import sun.reflect.CallerSensitive;
import sun.reflect.FieldAccessor;
import sun.reflect.Reflection;
import sun.reflect.annotation.AnnotationParser;
import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.TypeAnnotation;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.FieldRepository;
import sun.reflect.generics.scope.ClassScope;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

public final
class Field extends AccessibleObject implements Member {

    public AnnotatedType getAnnotatedType() {
        return TypeAnnotationParser.buildAnnotatedType(getTypeAnnotationBytes0(),
                sun.misc.SharedSecrets.getJavaLangAccess().
                        getConstantPool(getDeclaringClass()),
                this,
                getDeclaringClass(),
                getGenericType(),
                TypeAnnotation.TypeAnnotationTarget.FIELD);
    }


    private Class<?> clazz;
    private int slot;
    private String name;
    private Class<?> type;
    private int modifiers;
    private transient String signature;
    private transient FieldRepository genericInfo;
    private byte[] annotations;
    private FieldAccessor fieldAccessor;
    private FieldAccessor overrideFieldAccessor;
    private Field root;


    private String getGenericSignature() {
        return signature;
    }

    private GenericsFactory getFactory() {
        Class<?> c = getDeclaringClass();
        return CoreReflectionFactory.make(c, ClassScope.make(c));
    }

    private FieldRepository getGenericInfo() {
        if (genericInfo == null) {
            genericInfo = FieldRepository.make(getGenericSignature(),
                    getFactory());
        }
        return genericInfo;
    }

    Field(Class<?> declaringClass,
          String name,
          Class<?> type,
          int modifiers,
          int slot,
          String signature,
          byte[] annotations) {
        this.clazz = declaringClass;
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.slot = slot;
        this.signature = signature;
        this.annotations = annotations;
    }

    Field copy() {
        if (this.root != null) {
            throw new IllegalArgumentException("Can not copy a non-root Field");
        }

        Field res = new Field(clazz, name, type, modifiers, slot, signature, annotations);
        res.root = this;
        // Might as well eagerly propagate this if already present
        res.fieldAccessor = fieldAccessor;
        res.overrideFieldAccessor = overrideFieldAccessor;

        return res;
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

    public boolean isEnumConstant() {
        return (getModifiers() & Modifier.ENUM) != 0;
    }

    @Override
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    public Class<?> getType() {
        return type;
    }

    public Type getGenericType() {
        if (getGenericSignature() != null) {
            return getGenericInfo().getGenericType();
        } else {
            return getType();
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Field) {
            Field other = (Field) obj;
            return (getDeclaringClass() == other.getDeclaringClass())
                    && (getName() == other.getName())
                    && (getType() == other.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    @Override
    public String toString() {
        int mod = getModifiers();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
                + getType().getTypeName() + " "
                + getDeclaringClass().getTypeName() + "."
                + getName());
    }

    public String toGenericString() {
        int mod = getModifiers();
        Type fieldType = getGenericType();
        return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
                + fieldType.getTypeName() + " "
                + getDeclaringClass().getTypeName() + "."
                + getName());
    }

    @CallerSensitive
    public Object get(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).get(obj);
    }

    @CallerSensitive
    public boolean getBoolean(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getBoolean(obj);
    }

    @CallerSensitive
    public byte getByte(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getByte(obj);
    }

    @CallerSensitive
    public char getChar(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getChar(obj);
    }

    @CallerSensitive
    public short getShort(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getShort(obj);
    }

    @CallerSensitive
    public int getInt(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getInt(obj);
    }

    @CallerSensitive
    public long getLong(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getLong(obj);
    }

    @CallerSensitive
    public float getFloat(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getFloat(obj);
    }

    @CallerSensitive
    public double getDouble(Object obj)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        return getFieldAccessor(obj).getDouble(obj);
    }

    @CallerSensitive
    public void set(Object obj, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).set(obj, value);
    }

    @CallerSensitive
    public void setBoolean(Object obj, boolean z)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setBoolean(obj, z);
    }

    @CallerSensitive
    public void setByte(Object obj, byte b)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setByte(obj, b);
    }

    @CallerSensitive
    public void setChar(Object obj, char c)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setChar(obj, c);
    }

    @CallerSensitive
    public void setShort(Object obj, short s)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setShort(obj, s);
    }

    @CallerSensitive
    public void setInt(Object obj, int i)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setInt(obj, i);
    }

    @CallerSensitive
    public void setLong(Object obj, long l)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setLong(obj, l);
    }

    @CallerSensitive
    public void setFloat(Object obj, float f)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setFloat(obj, f);
    }

    @CallerSensitive
    public void setDouble(Object obj, double d)
            throws IllegalArgumentException, IllegalAccessException {
        if (!override) {
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, obj, modifiers);
            }
        }
        getFieldAccessor(obj).setDouble(obj, d);
    }

    private FieldAccessor getFieldAccessor(Object obj)
            throws IllegalAccessException {
        boolean ov = override;
        FieldAccessor a = (ov) ? overrideFieldAccessor : fieldAccessor;
        return (a != null) ? a : acquireFieldAccessor(ov);
    }

    private FieldAccessor acquireFieldAccessor(boolean overrideFinalCheck) {
        FieldAccessor tmp = null;
        if (root != null) {
            tmp = root.getFieldAccessor(overrideFinalCheck);
        }
        if (tmp != null) {
            if (overrideFinalCheck) {
                overrideFieldAccessor = tmp;
            } else {
                fieldAccessor = tmp;
            }
        } else {
            // Otherwise fabricate one and propagate it up to the root
            tmp = reflectionFactory.newFieldAccessor(this, overrideFinalCheck);
            setFieldAccessor(tmp, overrideFinalCheck);
        }

        return tmp;
    }

    private FieldAccessor getFieldAccessor(boolean overrideFinalCheck) {
        return (overrideFinalCheck) ? overrideFieldAccessor : fieldAccessor;
    }

    private void setFieldAccessor(FieldAccessor accessor, boolean overrideFinalCheck) {
        if (overrideFinalCheck) {
            overrideFieldAccessor = accessor;
        } else {
            fieldAccessor = accessor;
        }
        if (root != null) {
            root.setFieldAccessor(accessor, overrideFinalCheck);
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return annotationClass.cast(declaredAnnotations().get(annotationClass));
    }


    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return AnnotationSupport.getDirectlyAndIndirectlyPresent(declaredAnnotations(), annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return AnnotationParser.toArray(declaredAnnotations());
    }

    private transient Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

    private synchronized Map<Class<? extends Annotation>, Annotation> declaredAnnotations() {
        if (declaredAnnotations == null) {
            Field root = this.root;
            if (root != null) {
                declaredAnnotations = root.declaredAnnotations();
            } else {
                declaredAnnotations = AnnotationParser.parseAnnotations(
                        annotations,
                        sun.misc.SharedSecrets.getJavaLangAccess().getConstantPool(getDeclaringClass()),
                        getDeclaringClass());
            }
        }
        return declaredAnnotations;
    }

    private native byte[] getTypeAnnotationBytes0();


}
