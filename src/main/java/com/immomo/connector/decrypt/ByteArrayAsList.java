package com.immomo.connector.decrypt;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.Bytes;
import java.io.Serializable;
import java.util.RandomAccess;

/**
 * Created By wlb on 2018/3/7
 */
public class ByteArrayAsList implements RandomAccess, Serializable {

  private final byte[] array;
  private final int start;
  private final int end;

  private ByteArrayAsList(byte[] array) {
    this(array, 0, array.length);
  }

  private ByteArrayAsList(byte[] array, int start, int end) {
    this.array = array;
    this.start = start;
    this.end = end;
  }

  public static ByteArrayAsList newListWithLength(byte[] array, int length) {
    return new ByteArrayAsList(array, 0, length);
  }

  public static ByteArrayAsList newListWithStartAndLength(byte[] array, int start, int length) {
    return new ByteArrayAsList(array, start, start + length);
  }

  public static ByteArrayAsList newListWithStartAndEnd(byte[] array, int start, int end) {
    return new ByteArrayAsList(array, start, end);
  }

  public int size() {
    return end - start;
  }

  public boolean isEmpty() {
    return false;
  }

  public Byte get(int index) {
    checkElementIndex(index, size());
    return array[start + index];
  }

  public Byte set(int index, Byte element) {
    checkElementIndex(index, size());
    byte oldValue = array[start + index];
    // checkNotNull for GWT (do not optimize)
    array[start + index] = checkNotNull(element);
    return oldValue;
  }


  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof ByteArrayAsList) {
      ByteArrayAsList that = (ByteArrayAsList) object;
      int size = size();
      if (that.size() != size) {
        return false;
      }
      for (int i = 0; i < size; i++) {
        if (array[start + i] != that.array[that.start + i]) {
          return false;
        }
      }
      return true;
    }
    return super.equals(object);
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = start; i < end; i++) {
      result = 31 * result + Bytes.hashCode(array[i]);
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(size() * 5);
    builder.append('[').append(array[start]);
    for (int i = start + 1; i < end; i++) {
      builder.append(", ").append(array[i]);
    }
    return builder.append(']').toString();
  }

  public byte[] toByteArray() {
    // Arrays.copyOfRange() is not available under GWT
    int size = size();
    byte[] result = new byte[size];
    System.arraycopy(array, start, result, 0, size);
    return result;
  }

  private static final long serialVersionUID = 0;
}
