package me.foreverigor.utils;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ReflectionUtils {

  public static <C, T> Function<C, T> getGetterForFieldExact(Class<C> inClass,
                                                      String fieldName,
                                                      Class<T> fieldType,
                                                      ExactInvocator<T, C> exactInvocator) throws ReflectiveOperationException {
    final MethodHandle fieldGetter = getGetterMethodHandle(inClass, fieldName, fieldType);
    return (InstanceFieldGetter<T, C>) instance -> exactInvocator.invokeExactInvoke(fieldGetter, instance);
  }

  /**
   * Should invoke and return the result of {@link MethodHandle#invokeExact(Object...)}, casted to exactly the required
   * type.
   * <p> Warning, trying to create a generic ExactInvocator, or casting to a generic type won't work! The invokeExact
   * method has to be casted to the correct (expected) type at compile-time(!) because of its {@link MethodHandle#PolymorphicSignature}.
   */
  public interface ExactInvocator<R, T> {
    R invokeExactInvoke(MethodHandle handleToInvoke, T o) throws Throwable;
  }

  public interface ExactMultiInvocator<T, I> {
    T invokeExactInvoke(MethodHandle handleToInvoke, I instance) throws Throwable;
    void invokeExactInvoke(MethodHandle handleToInvoke, I instance, T value) throws Throwable;
  }

  public static MethodHandle getGetterMethodHandle(Class<?> inClass, String fieldName, Class<?> fieldType) throws ReflectiveOperationException {
    MethodHandle handle = getGetterMethodHandle(inClass, fieldName);
    return handle.asType(handle.type().changeReturnType(fieldType));
  }

  private static MethodHandle getGetterMethodHandle(Class<?> inClass, String fieldName) throws ReflectiveOperationException {
    return MethodHandles.lookup().unreflectGetter(makeAccessible(inClass.getDeclaredField(fieldName)));
  }

  public static <C,T> FunctionBiConsumer<C,T> getExactGetterSetter(Class<C> inClass, String fieldName, Class<T> fieldType, ExactMultiInvocator<T,C> exactInvocator) throws ReflectiveOperationException {
    Field field = getField(inClass, fieldName);
    MethodHandle getterHandle = MethodHandleLookup.unreflectGetter(field);
    MethodHandle setterHandle = MethodHandleLookup.unreflectSetter(field);
    return new FieldGetterSetter<>() {
      @Override
      public T getField(C instance) throws Throwable {
        return exactInvocator.invokeExactInvoke(getterHandle, instance);
      }
      @Override
      public void setField(C instance, T value) throws Throwable {
        exactInvocator.invokeExactInvoke(setterHandle, instance, value);
      }
    };
  }

  public static <C, R> Method getSupplierMethod(Class<C> inClass, String methodName, Class<R> returnType) throws ReflectiveOperationException {
    return getMethod(inClass, methodName);
  }

  private static <C,T> Field getField(Class<C> inClass, String fieldName) throws ReflectiveOperationException {
    return makeAccessible(inClass.getDeclaredField(fieldName));
  }

  public static <C> Method getMethod(Class<C> inClass, String methodName, Class<?> ... paramTypes) throws ReflectiveOperationException {
    return makeAccessible(inClass.getDeclaredMethod(methodName, paramTypes));
  }

  public static <T extends AccessibleObject> T makeAccessible(T reflectedObject) throws InaccessibleObjectException {
    reflectedObject.setAccessible(true);
    return reflectedObject;
  }

  private static final MethodHandles.Lookup MethodHandleLookup = MethodHandles.lookup();

  public interface FieldGetterSetter<T,I> extends InstanceFieldGetter<T,I>, InstanceFieldSetter<T,I>, FunctionBiConsumer<I,T> {
  }

  private interface InstanceFieldSetter<T,I> extends BiConsumer<I,T> {
    void setField(I instance, T value) throws Throwable;

    @Override
    default void accept(I i, T t) throws RuntimeException {
      try {
        setField(i, t);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  } // interface InstaceFieldSetter<T,I>

  private interface InstanceFieldGetter<T,I> extends Function<I, T> {
    T getField(I instance) throws Throwable;

    @Override
    default T apply(I i) throws RuntimeException {
      try {
        return getField(i);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }

  } // interface InstanceFieldGetter<T,O> extends Function<O,T>

  private ReflectionUtils() {}

} // class ReflectionUtils
