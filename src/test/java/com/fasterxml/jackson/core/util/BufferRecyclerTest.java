package com.fasterxml.jackson.core.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Unit tests for class {@link BufferRecycler}.
 *
 * @date 16.07.2017
 * @see BufferRecycler
 *
 **/
public class BufferRecyclerTest{


  @Test
  public void testBalloc() {

      BufferRecycler bufferRecycler = new BufferRecycler();
      byte[] byteArray = bufferRecycler.balloc(1);

      bufferRecycler.releaseByteBuffer(1, byteArray);

      assertArrayEquals(new byte[] {(byte)0}, byteArray);
      
      byte[] byteArrayTwo = bufferRecycler.allocByteBuffer(1, 1);

      assertNotSame(byteArrayTwo, byteArray);

  }


}