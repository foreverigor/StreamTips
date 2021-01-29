package me.foreverigor.utils;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InaccessibleObjectException;
import java.util.function.Function;

public class ReflectionUtils {

  public static <C, T> Function<C, T> getGetterForFieldExact(Class<C> inClass,
                                                      String fieldName,
                                                      Class<T> fieldType,
                                                      ExactInvocator<T, C> exactInvocator) throws ReflectiveOperationException {
    final MethodHandle fieldGetter = getGetterMethodHandle(inClass, fieldName, fieldType);
    return (InstanceFieldGetter<T, C>) instance -> exactInvocator.invokeExactInvoke(fieldGetter, instance);
  }

  public interface ExactInvocator<R, T> {
    R invokeExactInvoke(MethodHandle handleToInvoke, T o) throws Throwable;
  }

  public static MethodHandle getGetterMethodHandle(Class<?> inClass, String fieldName, Class<?> fieldType) throws ReflectiveOperationException {
    MethodHandle handle = getGetterMethodHandle(inClass, fieldName);
    return handle.asType(handle.type().changeReturnType(fieldType));
  }

  private static MethodHandle getGetterMethodHandle(Class<?> inClass, String fieldName) throws ReflectiveOperationException {
    return MethodHandles.lookup().unreflectGetter(makeAccessible(inClass.getDeclaredField(fieldName)));
  }

  public static <T extends AccessibleObject> T makeAccessible(T reflectedObject) throws InaccessibleObjectException {
    reflectedObject.setAccessible(true);
    return reflectedObject;
  }

  private interface InstanceFieldGetter<T, O> extends Function<O, T> {
    @Override
    default T apply(O o) throws RuntimeException {
      try {
        return getField(o);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }

    T getField(O instance) throws Throwable;

  } // interface InstanceFieldGetter<T,O> extends Function<O,T>

  private ReflectionUtils() {}

} // class ReflectionUtils
